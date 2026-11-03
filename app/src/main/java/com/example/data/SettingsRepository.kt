package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SlideshowSettings(
    val autoStartOnBoot: Boolean = true,
    val slideIntervalSeconds: Int = 8,
    val crossfadeDurationMs: Int = 1500,
    val kenBurnsEnabled: Boolean = true,
    val showClockAndDate: Boolean = true,
    val showMetadataBadge: Boolean = true,
    val shuffleOrder: Boolean = false,
    val cacheCloudPhotosOffline: Boolean = true,
    val activeSourceFilter: String = "ALL", // "ALL", "USB", "CLOUD"
    val cloudAlbumPreset: String = "LANDSCAPES"
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lumina_tv_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SlideshowSettings> = _settings.asStateFlow()

    private fun loadSettings(): SlideshowSettings {
        return SlideshowSettings(
            autoStartOnBoot = prefs.getBoolean(KEY_AUTO_BOOT, true),
            slideIntervalSeconds = prefs.getInt(KEY_INTERVAL, 8),
            crossfadeDurationMs = prefs.getInt(KEY_CROSSFADE, 1500),
            kenBurnsEnabled = prefs.getBoolean(KEY_KEN_BURNS, true),
            showClockAndDate = prefs.getBoolean(KEY_SHOW_CLOCK, true),
            showMetadataBadge = prefs.getBoolean(KEY_SHOW_METADATA, true),
            shuffleOrder = prefs.getBoolean(KEY_SHUFFLE, false),
            cacheCloudPhotosOffline = prefs.getBoolean(KEY_CACHE_OFFLINE, true),
            activeSourceFilter = prefs.getString(KEY_SOURCE_FILTER, "ALL") ?: "ALL",
            cloudAlbumPreset = prefs.getString(KEY_CLOUD_PRESET, "LANDSCAPES") ?: "LANDSCAPES"
        )
    }

    fun updateAutoStartOnBoot(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BOOT, enabled).apply()
        _settings.value = _settings.value.copy(autoStartOnBoot = enabled)
    }

    fun updateSlideInterval(seconds: Int) {
        prefs.edit().putInt(KEY_INTERVAL, seconds).apply()
        _settings.value = _settings.value.copy(slideIntervalSeconds = seconds)
    }

    fun updateCrossfadeDuration(durationMs: Int) {
        prefs.edit().putInt(KEY_CROSSFADE, durationMs).apply()
        _settings.value = _settings.value.copy(crossfadeDurationMs = durationMs)
    }

    fun updateKenBurns(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEN_BURNS, enabled).apply()
        _settings.value = _settings.value.copy(kenBurnsEnabled = enabled)
    }

    fun updateShowClock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_CLOCK, enabled).apply()
        _settings.value = _settings.value.copy(showClockAndDate = enabled)
    }

    fun updateShowMetadata(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_METADATA, enabled).apply()
        _settings.value = _settings.value.copy(showMetadataBadge = enabled)
    }

    fun updateShuffle(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE, enabled).apply()
        _settings.value = _settings.value.copy(shuffleOrder = enabled)
    }

    fun updateCacheCloudOffline(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CACHE_OFFLINE, enabled).apply()
        _settings.value = _settings.value.copy(cacheCloudPhotosOffline = enabled)
    }

    fun updateSourceFilter(filter: String) {
        prefs.edit().putString(KEY_SOURCE_FILTER, filter).apply()
        _settings.value = _settings.value.copy(activeSourceFilter = filter)
    }

    fun updateCloudPreset(preset: String) {
        prefs.edit().putString(KEY_CLOUD_PRESET, preset).apply()
        _settings.value = _settings.value.copy(cloudAlbumPreset = preset)
    }

    companion object {
        private const val KEY_AUTO_BOOT = "auto_boot"
        private const val KEY_INTERVAL = "slide_interval"
        private const val KEY_CROSSFADE = "crossfade_ms"
        private const val KEY_KEN_BURNS = "ken_burns"
        private const val KEY_SHOW_CLOCK = "show_clock"
        private const val KEY_SHOW_METADATA = "show_metadata"
        private const val KEY_SHUFFLE = "shuffle"
        private const val KEY_CACHE_OFFLINE = "cache_offline"
        private const val KEY_SOURCE_FILTER = "source_filter"
        private const val KEY_CLOUD_PRESET = "cloud_preset"

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
