package com.linnan.hayaocamera.ui.camera

import android.app.Application
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.linnan.hayaocamera.camera.BindTarget
import com.linnan.hayaocamera.camera.CameraEngine
import com.linnan.hayaocamera.camera.CameraEvent
import com.linnan.hayaocamera.camera.CaptureMode
import com.linnan.hayaocamera.camera.FlashMode
import com.linnan.hayaocamera.camera.TimerOption
import com.linnan.hayaocamera.data.GalleryItem
import com.linnan.hayaocamera.data.GalleryRepository
import com.linnan.hayaocamera.data.HayaoSettings
import com.linnan.hayaocamera.camera.ResolutionMode
import com.linnan.hayaocamera.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val galleryRepository = GalleryRepository(application)
    val engine = CameraEngine(application.applicationContext)

    val settings: StateFlow<HayaoSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, HayaoSettings())

    val engineState = engine.state

    private val _captureMode = MutableStateFlow(CaptureMode.PHOTO)
    val captureMode: StateFlow<CaptureMode> = _captureMode.asStateFlow()

    private val _timerCountdown = MutableStateFlow<Int?>(null)
    val timerCountdown: StateFlow<Int?> = _timerCountdown.asStateFlow()

    private val _latestThumbnail = MutableStateFlow<GalleryItem?>(null)
    val latestThumbnail: StateFlow<GalleryItem?> = _latestThumbnail.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var boundLifecycleOwner: LifecycleOwner? = null
    private var boundPreviewView: PreviewView? = null
    private var previousSettings: HayaoSettings = HayaoSettings()
    private var timerJob: Job? = null
    private var captureBusy = false
    private var micPermissionGranted = false

    init {
        viewModelScope.launch {
            engine.initialize()
            rebind()
            refreshThumbnail()
        }
        viewModelScope.launch {
            engine.events.collect { event -> handleEvent(event) }
        }
        viewModelScope.launch {
            settings.collect { newSettings ->
                onSettingsChanged(previousSettings, newSettings)
                previousSettings = newSettings
            }
        }
    }

    private fun onSettingsChanged(old: HayaoSettings, new: HayaoSettings) {
        val needsRebind = old.resolutionMode != new.resolutionMode ||
            old.rawCaptureEnabled != new.rawCaptureEnabled ||
            old.hdrEnabled != new.hdrEnabled ||
            old.videoQuality != new.videoQuality ||
            old.videoFrameRate != new.videoFrameRate ||
            old.videoStabilizationEnabled != new.videoStabilizationEnabled
        if (needsRebind && boundLifecycleOwner != null) {
            rebind()
        } else if (old.flashMode != new.flashMode) {
            engine.setFlashModeLive(new.flashMode)
        }
    }

    fun attachPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        boundLifecycleOwner = lifecycleOwner
        boundPreviewView = previewView
        rebind()
    }

    private fun rebind() {
        val owner = boundLifecycleOwner ?: return
        val view = boundPreviewView ?: return
        engine.bind(BindTarget(owner, view, _captureMode.value, settings.value))
    }

    fun setCaptureMode(mode: CaptureMode) {
        if (_captureMode.value == mode) return
        if (engineState.value.isRecording) return
        cancelTimer()
        _captureMode.value = mode
        rebind()
    }

    fun switchCamera() {
        if (engineState.value.isRecording) return
        engine.switchFacing()
        rebind()
    }

    fun selectBackLens(cameraId: String) {
        engine.selectBackLens(cameraId)
        rebind()
    }

    fun setZoomRatio(ratio: Float) = engine.setZoomRatio(ratio)

    fun setExposureIndex(index: Int) = engine.setExposureIndex(index)

    fun tapToFocus(point: androidx.camera.core.MeteringPoint) = engine.tapToFocus(point)

    fun cycleFlash() {
        val next = when (settings.value.flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
        viewModelScope.launch { settingsRepository.setFlashMode(next) }
    }

    fun setTimerOption(option: TimerOption) {
        viewModelScope.launch { settingsRepository.setTimerOption(option) }
    }

    fun setResolutionMode(mode: ResolutionMode) {
        viewModelScope.launch { settingsRepository.setResolutionMode(mode) }
    }

    fun setRawEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRawEnabled(enabled) }
    }

    fun setHdrEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHdrEnabled(enabled) }
    }

    fun setGridEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGridEnabled(enabled) }
    }

    fun setVideoStabilizationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVideoStabilizationEnabled(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticsEnabled(enabled) }
    }

    fun setVideoQuality(quality: com.linnan.hayaocamera.camera.VideoQualityChoice) {
        viewModelScope.launch { settingsRepository.setVideoQuality(quality) }
    }

    fun setVideoFrameRate(rate: com.linnan.hayaocamera.camera.FrameRateChoice) {
        viewModelScope.launch { settingsRepository.setVideoFrameRate(rate) }
    }

    fun setMicPermissionGranted(granted: Boolean) {
        micPermissionGranted = granted
    }

    fun onShutterPressed() {
        val context = getApplication<Application>()
        if (_captureMode.value == CaptureMode.PHOTO) {
            capturePhotoWithTimer(context.applicationContext)
        } else {
            if (engineState.value.isRecording) {
                engine.stopVideoRecording()
            } else {
                engine.startVideoRecording(context.applicationContext, settings.value, micPermissionGranted)
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerCountdown.value = null
        captureBusy = false
    }

    private fun capturePhotoWithTimer(context: android.content.Context) {
        if (captureBusy) return
        val timer = settings.value.timerOption
        captureBusy = true
        if (timer == TimerOption.OFF) {
            engine.capturePhoto(context, settings.value)
        } else {
            _timerCountdown.value = timer.seconds
            timerJob = viewModelScope.launch {
                var remaining = timer.seconds
                while (remaining > 0) {
                    delay(1000)
                    remaining -= 1
                    _timerCountdown.value = remaining
                }
                engine.refocusCenter()
                delay(300)
                _timerCountdown.value = null
                engine.capturePhoto(context, settings.value)
            }
        }
    }

    private fun handleEvent(event: CameraEvent) {
        val savedMessage = getApplication<Application>().getString(com.linnan.hayaocamera.R.string.capture_saved)
        when (event) {
            is CameraEvent.PhotoSaved -> {
                captureBusy = false
                _statusMessage.value = savedMessage
                refreshThumbnail()
            }

            is CameraEvent.PhotoFailed -> {
                captureBusy = false
                _statusMessage.value = event.message
            }

            is CameraEvent.VideoSaved -> {
                _statusMessage.value = savedMessage
                refreshThumbnail()
            }

            is CameraEvent.VideoFailed -> {
                _statusMessage.value = event.message
            }

            is CameraEvent.Info -> {
                _statusMessage.value = event.message
            }
        }
    }

    fun consumeStatusMessage() {
        _statusMessage.value = null
    }

    fun refreshThumbnail() {
        viewModelScope.launch {
            val items = galleryRepository.loadOwnMedia()
            _latestThumbnail.value = items.firstOrNull()
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}
