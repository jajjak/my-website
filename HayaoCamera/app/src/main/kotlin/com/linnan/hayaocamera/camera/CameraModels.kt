package com.linnan.hayaocamera.camera

import androidx.camera.core.CameraSelector

/** A single width x height combination reported by the camera hardware. */
data class ResolutionOption(val width: Int, val height: Int) {
    val megapixels: Double get() = (width.toLong() * height.toLong()) / 1_000_000.0
    val aspectRatio: Double get() = width.toDouble() / height.toDouble()
}

enum class LensType { WIDE, ULTRA_WIDE, TELEPHOTO, FRONT, UNKNOWN }

/** Describes one physically distinct camera exposed by the platform to this app. */
data class LensInfo(
    val cameraId: String,
    val lensFacing: Int,
    val lensType: LensType,
    val focalLengthMm: Float?,
    val approxZoomRatioVsPrimary: Float,
    val hasFlash: Boolean
)

enum class ResolutionMode { STANDARD, MAXIMUM }

/** Real, queryable capabilities of one camera — never an assumed/marketing spec. */
data class CameraCapabilities(
    val cameraId: String,
    val sensorNativeMegapixels: Double,
    val sensorWidth: Int,
    val sensorHeight: Int,
    val availablePhotoSizes: List<ResolutionOption>,
    val maximumPhotoSize: ResolutionOption,
    val standardPhotoSize: ResolutionOption,
    val supportsRawSensor: Boolean,
    val availableRawSizes: List<ResolutionOption>,
    val hasFlash: Boolean,
    val maxDigitalZoom: Float
)

data class VideoResolutionCapability(
    val supports4k: Boolean,
    val supports1080p: Boolean,
    val supports720p: Boolean,
    val supports60FpsAt1080p: Boolean,
    val supports60FpsAt4k: Boolean
)

enum class VideoQualityChoice { UHD_4K, FHD_1080P, HD_720P }
enum class FrameRateChoice(val fps: Int) { FPS_30(30), FPS_60(60) }

enum class FlashMode { OFF, ON, AUTO }
enum class TimerOption(val seconds: Int) { OFF(0), SEC_3(3), SEC_5(5), SEC_10(10) }
enum class CaptureMode { PHOTO, VIDEO }

fun cameraSelectorFor(cameraId: String): CameraSelector =
    CameraSelector.Builder().addCameraFilter { infos ->
        infos.filter { androidx.camera.camera2.interop.Camera2CameraInfo.from(it).cameraId == cameraId }
    }.build()
