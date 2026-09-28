package com.linnan.hayaocamera.camera

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.util.Log
import android.util.Range
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionFilter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.linnan.hayaocamera.data.HayaoSettings
import com.linnan.hayaocamera.data.MediaStoreSaver
import com.linnan.hayaocamera.util.awaitFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.abs

data class EngineState(
    val isInitialized: Boolean = false,
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val facingBack: Boolean = true,
    val backLenses: List<LensInfo> = emptyList(),
    val frontLenses: List<LensInfo> = emptyList(),
    val selectedLensId: String? = null,
    val capabilities: CameraCapabilities? = null,
    val videoCapability: VideoResolutionCapability? = null,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val exposureIndex: Int = 0,
    val exposureRange: IntRange = 0..0,
    val exposureStepEv: Float = 0f,
    val hasFlashUnit: Boolean = false,
    val hdrExtensionAvailable: Boolean = false,
    val hdrActiveNow: Boolean = false,
    val hdrConflictWithMaxResolution: Boolean = false,
    val rawAvailable: Boolean = false,
    val actualPhotoResolution: ResolutionOption? = null,
    val isRecording: Boolean = false,
    val recordingElapsedMs: Long = 0L,
    val cameraError: String? = null
)

sealed interface CameraEvent {
    data class PhotoSaved(val uri: Uri?) : CameraEvent
    data class PhotoFailed(val message: String) : CameraEvent
    data class VideoSaved(val uri: Uri?) : CameraEvent
    data class VideoFailed(val message: String) : CameraEvent
    data class Info(val message: String) : CameraEvent
}

data class BindTarget(
    val lifecycleOwner: LifecycleOwner,
    val previewView: PreviewView,
    val mode: CaptureMode,
    val settings: HayaoSettings
)

/**
 * Owns every CameraX interaction: binding, real capability discovery, still capture and
 * video recording. UI code never touches CameraX types directly — it reads [state] and
 * calls the functions here.
 */
class CameraEngine(private val appContext: Context) {

    private val tag = "CameraEngine"
    private val scope = CoroutineScope(SupervisorJob())
    private val capabilityReader = CameraCapabilityReader(appContext)
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var recordingTimerJob: Job? = null
    private var recordingStartUptimeMs: Long = 0L

    private var facingBack = true
    private var selectedBackLensId: String? = null
    private var boundPreviewView: PreviewView? = null

    /**
     * Tracks the physical device rotation (not the locked-portrait Activity rotation) so
     * photos/videos carry correct orientation metadata even when the phone is held sideways.
     */
    private var lastKnownRotation = Surface.ROTATION_0

    private val orientationListener = object : OrientationEventListener(appContext) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            val rotation = when {
                orientation >= 315 || orientation < 45 -> Surface.ROTATION_0
                orientation < 135 -> Surface.ROTATION_270
                orientation < 225 -> Surface.ROTATION_180
                else -> Surface.ROTATION_90
            }
            lastKnownRotation = rotation
            imageCapture?.targetRotation = rotation
            videoCapture?.targetRotation = rotation
        }
    }

    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CameraEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<CameraEvent> = _events.asSharedFlow()

    suspend fun initialize() {
        if (cameraProvider != null) return
        val provider = ProcessCameraProvider.getInstance(appContext).awaitFuture()
        cameraProvider = provider
        extensionsManager = try {
            ExtensionsManager.getInstanceAsync(appContext, provider).awaitFuture()
        } catch (e: Exception) {
            Log.w(tag, "Extensions unavailable: ${e.message}")
            null
        }
        val backLenses = capabilityReader.listBackLenses()
        val frontLenses = capabilityReader.listFrontLenses()
        selectedBackLensId = backLenses.firstOrNull { it.lensType == LensType.WIDE }?.cameraId
            ?: backLenses.firstOrNull()?.cameraId

        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }

        _state.update {
            it.copy(isInitialized = true, backLenses = backLenses, frontLenses = frontLenses)
        }
    }

    fun selectBackLens(cameraId: String) {
        selectedBackLensId = cameraId
    }

    fun switchFacing() {
        facingBack = !facingBack
    }

    fun isFacingBack(): Boolean = facingBack

    fun bind(target: BindTarget) {
        val provider = cameraProvider ?: return
        val lensList = if (facingBack) _state.value.backLenses else _state.value.frontLenses
        val lensId = if (facingBack) {
            selectedBackLensId ?: lensList.firstOrNull()?.cameraId
        } else {
            lensList.firstOrNull()?.cameraId
        }
        if (lensId == null) {
            _state.update { it.copy(cameraError = "利用できるカメラが見つかりません。") }
            return
        }

        val capabilities = capabilityReader.readCapabilities(lensId)
        val videoCapability = capabilityReader.readVideoResolutionCapability(lensId)
        val baseSelector = cameraSelectorFor(lensId)

        val hdrAvailable = try {
            extensionsManager?.isExtensionAvailable(baseSelector, ExtensionMode.HDR) ?: false
        } catch (e: Exception) {
            false
        }
        val hdrConflict = target.settings.hdrEnabled && target.settings.resolutionMode == ResolutionMode.MAXIMUM
        val useHdr = target.settings.hdrEnabled && hdrAvailable && !hdrConflict

        val selector = if (useHdr) {
            try {
                extensionsManager!!.getExtensionEnabledCameraSelector(baseSelector, ExtensionMode.HDR)
            } catch (e: Exception) {
                baseSelector
            }
        } else baseSelector

        val useRaw = target.settings.rawCaptureEnabled && capabilities?.supportsRawSensor == true

        val previewBuilder = Preview.Builder()
        if (target.mode == CaptureMode.VIDEO &&
            target.settings.videoFrameRate == FrameRateChoice.FPS_60 &&
            videoSupports60(target.settings.videoQuality, videoCapability)
        ) {
            Camera2Interop.Extender(previewBuilder).setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(60, 60)
            )
        }
        val preview = previewBuilder.build().also { it.surfaceProvider = target.previewView.surfaceProvider }

        val captureBuilder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(100)
            .setFlashMode(mapFlashMode(target.settings.flashMode))

        if (useRaw) {
            captureBuilder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_RAW_JPEG)
        } else if (capabilities != null) {
            val target2 = if (target.settings.resolutionMode == ResolutionMode.MAXIMUM) {
                capabilities.maximumPhotoSize
            } else {
                capabilities.standardPhotoSize
            }
            captureBuilder.setResolutionSelector(buildResolutionSelector(target2))
        }
        captureBuilder.setTargetRotation(lastKnownRotation)
        val newImageCapture = captureBuilder.build()

        val recorder = Recorder.Builder()
            .setQualitySelector(buildQualitySelector(target.settings.videoQuality))
            .build()
        if (target.settings.videoFrameRate == FrameRateChoice.FPS_60 &&
            videoSupports60(target.settings.videoQuality, videoCapability)
        ) {
            recorder.setVideoEncodingFrameRate(60)
        }
        val newVideoCapture = VideoCapture.Builder(recorder)
            .setVideoStabilizationEnabled(target.settings.videoStabilizationEnabled)
            .setTargetRotation(lastKnownRotation)
            .build()

        provider.unbindAll()
        val boundCamera = try {
            if (target.mode == CaptureMode.PHOTO) {
                provider.bindToLifecycle(target.lifecycleOwner, selector, preview, newImageCapture)
            } else {
                provider.bindToLifecycle(target.lifecycleOwner, selector, preview, newVideoCapture)
            }
        } catch (e: Exception) {
            Log.e(tag, "bindToLifecycle failed", e)
            _state.update { it.copy(cameraError = "カメラを起動できませんでした。") }
            return
        }

        camera = boundCamera
        imageCapture = if (target.mode == CaptureMode.PHOTO) newImageCapture else null
        videoCapture = if (target.mode == CaptureMode.VIDEO) newVideoCapture else null
        boundPreviewView = target.previewView

        if (target.mode == CaptureMode.VIDEO && boundCamera.cameraInfo.hasFlashUnit()) {
            boundCamera.cameraControl.enableTorch(target.settings.flashMode == FlashMode.ON)
        }

        val cameraInfo = boundCamera.cameraInfo
        val zoomState = cameraInfo.zoomState.value
        val exposureState = cameraInfo.exposureState

        _state.update {
            it.copy(
                captureMode = target.mode,
                facingBack = facingBack,
                selectedLensId = lensId,
                capabilities = capabilities,
                videoCapability = videoCapability,
                zoomRatio = zoomState?.zoomRatio ?: 1f,
                minZoomRatio = zoomState?.minZoomRatio ?: 1f,
                maxZoomRatio = zoomState?.maxZoomRatio ?: 1f,
                exposureIndex = exposureState.exposureCompensationIndex,
                exposureRange = exposureState.exposureCompensationRange.lower..exposureState.exposureCompensationRange.upper,
                exposureStepEv = exposureState.exposureCompensationStep.toFloat(),
                hasFlashUnit = cameraInfo.hasFlashUnit(),
                hdrExtensionAvailable = hdrAvailable,
                hdrActiveNow = useHdr,
                hdrConflictWithMaxResolution = hdrConflict,
                rawAvailable = capabilities?.supportsRawSensor == true,
                actualPhotoResolution = imageCapture?.resolutionInfo?.resolution?.let {
                    ResolutionOption(it.width, it.height)
                } ?: capabilities?.let { c ->
                    if (target.settings.resolutionMode == ResolutionMode.MAXIMUM) c.maximumPhotoSize else c.standardPhotoSize
                },
                cameraError = null
            )
        }

        cameraInfo.zoomState.observe(target.lifecycleOwner) { z ->
            _state.update { s -> s.copy(zoomRatio = z.zoomRatio, minZoomRatio = z.minZoomRatio, maxZoomRatio = z.maxZoomRatio) }
        }

        if (target.mode == CaptureMode.PHOTO) {
            // ResolutionInfo is finalized asynchronously once the capture session opens.
            scope.launch {
                delay(250)
                imageCapture?.resolutionInfo?.resolution?.let { res ->
                    _state.update { it.copy(actualPhotoResolution = ResolutionOption(res.width, res.height)) }
                }
            }
        }
    }

    private fun videoSupports60(quality: VideoQualityChoice, cap: VideoResolutionCapability?): Boolean {
        if (cap == null) return false
        return when (quality) {
            VideoQualityChoice.UHD_4K -> cap.supports60FpsAt4k
            else -> cap.supports60FpsAt1080p
        }
    }

    private fun buildQualitySelector(choice: VideoQualityChoice): QualitySelector {
        val quality = when (choice) {
            VideoQualityChoice.UHD_4K -> Quality.UHD
            VideoQualityChoice.FHD_1080P -> Quality.FHD
            VideoQualityChoice.HD_720P -> Quality.HD
        }
        return QualitySelector.from(quality, FallbackStrategy.lowerQualityOrHigherThan(quality))
    }

    private fun mapFlashMode(mode: FlashMode): Int = when (mode) {
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }

    private fun buildResolutionSelector(target: ResolutionOption): ResolutionSelector {
        val filter = ResolutionFilter { supportedSizes, _ ->
            supportedSizes.sortedBy { size ->
                val aspectDiff = abs(size.width.toDouble() / size.height - target.aspectRatio)
                val areaDiff = abs(
                    size.width.toLong() * size.height.toLong() -
                        target.width.toLong() * target.height.toLong()
                ).toDouble()
                aspectDiff * 1_000_000_000.0 + areaDiff
            }
        }
        return ResolutionSelector.Builder().setResolutionFilter(filter).build()
    }

    fun setZoomRatio(ratio: Float) {
        val cam = camera ?: return
        val zoomState = cam.cameraInfo.zoomState.value ?: return
        val clamped = ratio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
        cam.cameraControl.setZoomRatio(clamped)
    }

    fun setExposureIndex(index: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    fun tapToFocus(point: androidx.camera.core.MeteringPoint) {
        val cam = camera ?: return
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(4, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    /** Re-locks AF/AE on the frame centre — used right before a timer-delayed shutter fires. */
    fun refocusCenter() {
        val view = boundPreviewView ?: return
        if (view.width == 0 || view.height == 0) return
        val point = view.meteringPointFactory.createPoint(view.width / 2f, view.height / 2f)
        tapToFocus(point)
    }

    fun setFlashModeLive(mode: FlashMode) {
        imageCapture?.setFlashMode(mapFlashMode(mode))
        val cam = camera ?: return
        if (_state.value.captureMode == CaptureMode.VIDEO && cam.cameraInfo.hasFlashUnit()) {
            cam.cameraControl.enableTorch(mode == FlashMode.ON)
        }
    }

    fun capturePhoto(context: Context, settings: HayaoSettings) {
        val capture = imageCapture ?: run {
            scope.launch { _events.emit(CameraEvent.PhotoFailed("カメラの準備ができていません。")) }
            return
        }
        val useRaw = settings.rawCaptureEnabled && _state.value.rawAvailable

        if (useRaw) {
            val rawOptions = MediaStoreSaver.createPhotoOutputOptions(context, isRaw = true)
            val jpegOptions = MediaStoreSaver.createPhotoOutputOptions(context, isRaw = false)
            capture.takePicture(
                rawOptions,
                jpegOptions,
                ioExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        MediaStoreSaver.finalizePending(context, outputFileResults.savedUri)
                        scope.launch { _events.emit(CameraEvent.PhotoSaved(outputFileResults.savedUri)) }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        scope.launch { _events.emit(CameraEvent.PhotoFailed(describeCaptureError(exception))) }
                    }
                }
            )
        } else {
            val options = MediaStoreSaver.createPhotoOutputOptions(context, isRaw = false)
            capture.takePicture(
                options,
                ioExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        MediaStoreSaver.finalizePending(context, outputFileResults.savedUri)
                        scope.launch { _events.emit(CameraEvent.PhotoSaved(outputFileResults.savedUri)) }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        scope.launch { _events.emit(CameraEvent.PhotoFailed(describeCaptureError(exception))) }
                    }
                }
            )
        }
    }

    private fun describeCaptureError(exception: ImageCaptureException): String {
        return when (exception.imageCaptureError) {
            ImageCapture.ERROR_FILE_IO -> "空き容量が不足しているか、保存先にアクセスできませんでした。"
            ImageCapture.ERROR_CAPTURE_FAILED -> "撮影に失敗しました。もう一度お試しください。"
            ImageCapture.ERROR_CAMERA_CLOSED -> "カメラが使用できませんでした。"
            else -> "撮影中にエラーが発生しました。"
        }
    }

    fun startVideoRecording(context: Context, settings: HayaoSettings, audioEnabled: Boolean) {
        val capture = videoCapture ?: run {
            scope.launch { _events.emit(CameraEvent.VideoFailed("カメラの準備ができていません。")) }
            return
        }
        if (activeRecording != null) return

        val outputOptions = MediaStoreSaver.createVideoOutputOptions(context)
        var pending: PendingRecording = capture.output.prepareRecording(context, outputOptions)
        if (audioEnabled) {
            pending = pending.withAudioEnabled()
        }

        recordingStartUptimeMs = android.os.SystemClock.elapsedRealtime()
        activeRecording = pending.start(mainExecutor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    _state.update { it.copy(isRecording = true, recordingElapsedMs = 0L) }
                    startElapsedTimer()
                }

                is VideoRecordEvent.Finalize -> {
                    _state.update { it.copy(isRecording = false, recordingElapsedMs = 0L) }
                    recordingTimerJob?.cancel()
                    activeRecording = null
                    if (event.hasError()) {
                        scope.launch { _events.emit(CameraEvent.VideoFailed(describeVideoError(event.error))) }
                    } else {
                        scope.launch { _events.emit(CameraEvent.VideoSaved(event.outputResults.outputUri)) }
                    }
                }

                else -> Unit
            }
        }
    }

    fun stopVideoRecording() {
        activeRecording?.stop()
    }

    private fun startElapsedTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = scope.launch {
            while (true) {
                val elapsed = android.os.SystemClock.elapsedRealtime() - recordingStartUptimeMs
                _state.update { it.copy(recordingElapsedMs = elapsed) }
                delay(200)
            }
        }
    }

    private fun describeVideoError(error: Int): String = when (error) {
        VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE -> "空き容量が不足しています。不要なファイルを削除してください。"
        VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE -> "カメラが使用できなくなったため録画を停止しました。"
        VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED -> "ファイルサイズの上限に達しました。"
        VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA -> "録画データを保存できませんでした。"
        else -> "録画中にエラーが発生しました。"
    }

    fun release() {
        recordingTimerJob?.cancel()
        activeRecording?.stop()
        activeRecording = null
        orientationListener.disable()
        cameraProvider?.unbindAll()
    }
}
