package com.linnan.hayaocamera.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.linnan.hayaocamera.camera.FlashMode
import com.linnan.hayaocamera.camera.FrameRateChoice
import com.linnan.hayaocamera.camera.ResolutionMode
import com.linnan.hayaocamera.camera.TimerOption
import com.linnan.hayaocamera.camera.VideoQualityChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "hayao_camera_settings")

data class HayaoSettings(
    val resolutionMode: ResolutionMode = ResolutionMode.STANDARD,
    val rawCaptureEnabled: Boolean = false,
    val gridEnabled: Boolean = false,
    val hdrEnabled: Boolean = false,
    val shutterSoundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val flashMode: FlashMode = FlashMode.OFF,
    val timerOption: TimerOption = TimerOption.OFF,
    val videoQuality: VideoQualityChoice = VideoQualityChoice.FHD_1080P,
    val videoFrameRate: FrameRateChoice = FrameRateChoice.FPS_30,
    val videoStabilizationEnabled: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val RESOLUTION_MODE = stringPreferencesKey("resolution_mode")
        val RAW_ENABLED = booleanPreferencesKey("raw_enabled")
        val GRID_ENABLED = booleanPreferencesKey("grid_enabled")
        val HDR_ENABLED = booleanPreferencesKey("hdr_enabled")
        val SHUTTER_SOUND = booleanPreferencesKey("shutter_sound")
        val HAPTICS = booleanPreferencesKey("haptics")
        val FLASH_MODE = stringPreferencesKey("flash_mode")
        val TIMER_OPTION = stringPreferencesKey("timer_option")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val VIDEO_FPS = stringPreferencesKey("video_fps")
        val VIDEO_STABILIZATION = booleanPreferencesKey("video_stabilization")
    }

    val settings: Flow<HayaoSettings> = context.dataStore.data.map { prefs ->
        HayaoSettings(
            resolutionMode = prefs[Keys.RESOLUTION_MODE]?.let { runCatching { ResolutionMode.valueOf(it) }.getOrNull() }
                ?: ResolutionMode.STANDARD,
            rawCaptureEnabled = prefs[Keys.RAW_ENABLED] ?: false,
            gridEnabled = prefs[Keys.GRID_ENABLED] ?: false,
            hdrEnabled = prefs[Keys.HDR_ENABLED] ?: false,
            shutterSoundEnabled = prefs[Keys.SHUTTER_SOUND] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            flashMode = prefs[Keys.FLASH_MODE]?.let { runCatching { FlashMode.valueOf(it) }.getOrNull() }
                ?: FlashMode.OFF,
            timerOption = prefs[Keys.TIMER_OPTION]?.let { runCatching { TimerOption.valueOf(it) }.getOrNull() }
                ?: TimerOption.OFF,
            videoQuality = prefs[Keys.VIDEO_QUALITY]?.let { runCatching { VideoQualityChoice.valueOf(it) }.getOrNull() }
                ?: VideoQualityChoice.FHD_1080P,
            videoFrameRate = prefs[Keys.VIDEO_FPS]?.let { runCatching { FrameRateChoice.valueOf(it) }.getOrNull() }
                ?: FrameRateChoice.FPS_30,
            videoStabilizationEnabled = prefs[Keys.VIDEO_STABILIZATION] ?: true
        )
    }

    suspend fun setResolutionMode(mode: ResolutionMode) = update { it[Keys.RESOLUTION_MODE] = mode.name }
    suspend fun setRawEnabled(enabled: Boolean) = update { it[Keys.RAW_ENABLED] = enabled }
    suspend fun setGridEnabled(enabled: Boolean) = update { it[Keys.GRID_ENABLED] = enabled }
    suspend fun setHdrEnabled(enabled: Boolean) = update { it[Keys.HDR_ENABLED] = enabled }
    suspend fun setShutterSoundEnabled(enabled: Boolean) = update { it[Keys.SHUTTER_SOUND] = enabled }
    suspend fun setHapticsEnabled(enabled: Boolean) = update { it[Keys.HAPTICS] = enabled }
    suspend fun setFlashMode(mode: FlashMode) = update { it[Keys.FLASH_MODE] = mode.name }
    suspend fun setTimerOption(option: TimerOption) = update { it[Keys.TIMER_OPTION] = option.name }
    suspend fun setVideoQuality(quality: VideoQualityChoice) = update { it[Keys.VIDEO_QUALITY] = quality.name }
    suspend fun setVideoFrameRate(rate: FrameRateChoice) = update { it[Keys.VIDEO_FPS] = rate.name }
    suspend fun setVideoStabilizationEnabled(enabled: Boolean) = update { it[Keys.VIDEO_STABILIZATION] = enabled }

    suspend fun resetToDefaults() = context.dataStore.edit { it.clear() }

    private suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
