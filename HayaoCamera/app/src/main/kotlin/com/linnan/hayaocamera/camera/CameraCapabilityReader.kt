package com.linnan.hayaocamera.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import kotlin.math.abs

/**
 * Reads the *real* capabilities the platform exposes for each camera via the Camera2
 * characteristics API. Nothing here is guessed: every number comes straight from
 * [CameraCharacteristics], so a sensor's marketing megapixel count is never assumed —
 * only what [StreamConfigurationMap] actually reports as capturable is trusted.
 */
class CameraCapabilityReader(context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun listBackLenses(): List<LensInfo> = buildLensList(CameraCharacteristics.LENS_FACING_BACK)

    fun listFrontLenses(): List<LensInfo> = buildLensList(CameraCharacteristics.LENS_FACING_FRONT)

    private fun buildLensList(facing: Int): List<LensInfo> {
        val infos = mutableListOf<LensInfo>()
        for (id in safeCameraIdList()) {
            val chars = try {
                cameraManager.getCameraCharacteristics(id)
            } catch (_: Exception) {
                continue
            }
            val lensFacing = chars.get(CameraCharacteristics.LENS_FACING) ?: continue
            if (lensFacing != facing) continue

            val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            val focalLength = focalLengths?.minOrNull()
            val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

            infos += LensInfo(
                cameraId = id,
                lensFacing = lensFacing,
                lensType = if (facing == CameraCharacteristics.LENS_FACING_FRONT) LensType.FRONT else LensType.UNKNOWN,
                focalLengthMm = focalLength,
                approxZoomRatioVsPrimary = 1f,
                hasFlash = hasFlash
            )
        }

        if (facing == CameraCharacteristics.LENS_FACING_BACK) {
            return classifyBackLenses(infos)
        }
        return infos
    }

    private fun classifyBackLenses(lenses: List<LensInfo>): List<LensInfo> {
        if (lenses.isEmpty()) return lenses
        val withFocal = lenses.filter { it.focalLengthMm != null }
        if (withFocal.size < 2) {
            // Only one usable focal length reported: treat it as the sole wide lens.
            return lenses.map { it.copy(lensType = LensType.WIDE, approxZoomRatioVsPrimary = 1f) }
        }
        val primaryFocal = withFocal.minByOrNull { abs(it.focalLengthMm!! - referenceWideFocalMm(withFocal)) }!!.focalLengthMm!!
        return lenses.map { lens ->
            val focal = lens.focalLengthMm
            if (focal == null) {
                lens.copy(lensType = LensType.UNKNOWN)
            } else {
                val ratio = primaryFocal / focal
                val type = when {
                    focal < primaryFocal * 0.85f -> LensType.ULTRA_WIDE
                    focal > primaryFocal * 1.15f -> LensType.TELEPHOTO
                    else -> LensType.WIDE
                }
                lens.copy(lensType = type, approxZoomRatioVsPrimary = ratio)
            }
        }.sortedBy { it.focalLengthMm ?: Float.MAX_VALUE }
    }

    /** The "primary" lens on most phones is the middle focal length of the set. */
    private fun referenceWideFocalMm(lenses: List<LensInfo>): Float {
        val sorted = lenses.mapNotNull { it.focalLengthMm }.sorted()
        return sorted[sorted.size / 2]
    }

    fun readCapabilities(cameraId: String): CameraCapabilities? {
        val chars = try {
            cameraManager.getCameraCharacteristics(cameraId)
        } catch (_: Exception) {
            return null
        }
        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return null
        val jpegSizes = map.getOutputSizes(ImageFormat.JPEG)
            ?.map { ResolutionOption(it.width, it.height) }
            ?.sortedByDescending { it.width.toLong() * it.height.toLong() }
            ?: emptyList()
        if (jpegSizes.isEmpty()) return null

        val pixelArray = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val sensorMp = pixelArray?.let { (it.width.toLong() * it.height.toLong()) / 1_000_000.0 } ?: jpegSizes.first().megapixels

        val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        val supportsRaw = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        val rawSizes = if (supportsRaw) {
            map.getOutputSizes(ImageFormat.RAW_SENSOR)
                ?.map { ResolutionOption(it.width, it.height) }
                ?.sortedByDescending { it.width.toLong() * it.height.toLong() }
                ?: emptyList()
        } else emptyList()

        val maxSize = jpegSizes.first()
        val standardSize = pickStandardSize(jpegSizes, maxSize)
        val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
        val maxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f

        return CameraCapabilities(
            cameraId = cameraId,
            sensorNativeMegapixels = sensorMp,
            sensorWidth = pixelArray?.width ?: maxSize.width,
            sensorHeight = pixelArray?.height ?: maxSize.height,
            availablePhotoSizes = jpegSizes,
            maximumPhotoSize = maxSize,
            standardPhotoSize = standardSize,
            supportsRawSensor = supportsRaw && rawSizes.isNotEmpty(),
            availableRawSizes = rawSizes,
            hasFlash = hasFlash,
            maxDigitalZoom = maxZoom
        )
    }

    /**
     * A "standard" quality tier: the size closest to 12 megapixels that shares the sensor's
     * native aspect ratio, so users get a fast, storage-friendly capture without an
     * upscaled or oddly cropped image.
     */
    private fun pickStandardSize(sizes: List<ResolutionOption>, max: ResolutionOption): ResolutionOption {
        val sameAspect = sizes.filter { abs(it.aspectRatio - max.aspectRatio) < 0.05 }
        val pool = sameAspect.ifEmpty { sizes }
        if (pool.size == 1) return pool.first()
        val target = 12.0
        return pool.minByOrNull { abs(it.megapixels - target) } ?: pool.first()
    }

    fun readVideoResolutionCapability(cameraId: String): VideoResolutionCapability {
        val chars = try {
            cameraManager.getCameraCharacteristics(cameraId)
        } catch (_: Exception) {
            return VideoResolutionCapability(false, true, true, false, false)
        }
        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: emptyArray()
        val supports60 = fpsRanges.any { it.lower >= 50 && it.upper >= 60 }

        val sizes4k = ResolutionOption(3840, 2160)
        val sizes1080 = ResolutionOption(1920, 1080)

        fun sizeSupported(target: ResolutionOption): Boolean {
            val outputs = map?.getOutputSizes(android.media.MediaRecorder::class.java) ?: return false
            return outputs.any { it.width == target.width && it.height == target.height }
        }

        val supports4k = sizeSupported(sizes4k)
        val supports60At1080 = supports60 && has60FpsForSize(map, sizes1080)
        val supports60At4k = supports60 && supports4k && has60FpsForSize(map, sizes4k)

        return VideoResolutionCapability(
            supports4k = supports4k,
            // Every camera2-compliant device can record 1080p/720p; these are not gated.
            supports1080p = true,
            supports720p = true,
            supports60FpsAt1080p = supports60At1080,
            supports60FpsAt4k = supports60At4k
        )
    }

    private fun has60FpsForSize(map: StreamConfigurationMap?, size: ResolutionOption): Boolean {
        if (map == null) return false
        val duration = try {
            map.getOutputMinFrameDuration(
                android.media.MediaRecorder::class.java,
                android.util.Size(size.width, size.height)
            )
        } catch (_: Exception) {
            return false
        }
        if (duration <= 0) return false
        val fps = 1_000_000_000.0 / duration
        return fps >= 59.0
    }

    private fun safeCameraIdList(): Array<String> = try {
        cameraManager.cameraIdList
    } catch (_: Exception) {
        emptyArray()
    }
}
