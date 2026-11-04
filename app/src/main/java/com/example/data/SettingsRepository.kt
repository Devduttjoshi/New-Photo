package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SlideshowSettings(
    val autoStartOnBoot: Boolean = true,
    val autoResumeSession: Boolean = true,
    val slideIntervalSeconds: Int = 8,
    val crossfadeDurationMs: Int = 1500,
    val kenBurnsEnabled: Boolean = true,
    val showClockAndDate: Boolean = true,
    val showMetadataBadge: Boolean = true,
    val shuffleOrder: Boolean = false,
    val cacheCloudPhotosOffline: Boolean = true,
    val activeSourceFilter: String = "ALL", // "ALL", "USB", "CLOUD"
    val activeWeekFilter: String = "ALL", // "ALL" or specific week label like "Oct 2024 • Week 43"
    val cloudAlbumPreset: String = "GOOGLE_PHOTOS_TEST",
    val framingMode: String = "AMBIENT_FIT", // "AMBIENT_FIT", "FILL_CROP", "BLACK_BARS"
    val maxCacheSizeMb: Int = 250, // Rolling cache cap to protect TV disk storage
    val lastSessionPhotoId: Long = 0L,
    val lastSessionIndex: Int = 0
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lumina_tv_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SlideshowSettings> = _settings.asStateFlow()

    private fun loadSettings(): SlideshowSettings {
        return SlideshowSettings(
            autoStartOnBoot = prefs.getBoolean(KEY_AUTO_BOOT, true),
            autoResumeSession = prefs.getBoolean(KEY_AUTO_RESUME, true),
            slideIntervalSeconds = prefs.getInt(KEY_INTERVAL, 8),
            crossfadeDurationMs = prefs.getInt(KEY_CROSSFADE, 1500),
            kenBurnsEnabled = prefs.getBoolean(KEY_KEN_BURNS, true),
            showClockAndDate = prefs.getBoolean(KEY_SHOW_CLOCK, true),
            showMetadataBadge = prefs.getBoolean(KEY_SHOW_METADATA, true),
            shuffleOrder = prefs.getBoolean(KEY_SHUFFLE, false),
            cacheCloudPhotosOffline = prefs.getBoolean(KEY_CACHE_OFFLINE, true),
            activeSourceFilter = prefs.getString(KEY_SOURCE_FILTER, "ALL") ?: "ALL",
            activeWeekFilter = prefs.getString(KEY_WEEK_FILTER, "ALL") ?: "ALL",
            cloudAlbumPreset = prefs.getString(KEY_CLOUD_PRESET, "GOOGLE_PHOTOS_TEST") ?: "GOOGLE_PHOTOS_TEST",
            framingMode = prefs.getString(KEY_FRAMING_MODE, "AMBIENT_FIT") ?: "AMBIENT_FIT",
            maxCacheSizeMb = prefs.getInt(KEY_MAX_CACHE_MB, 250),
            lastSessionPhotoId = prefs.getLong(KEY_LAST_PHOTO_ID, 0L),
            lastSessionIndex = prefs.getInt(KEY_LAST_INDEX, 0)
        )
    }

    fun updateAutoStartOnBoot(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BOOT, enabled).apply()
        _settings.value = _settings.value.copy(autoStartOnBoot = enabled)
    }

    fun updateAutoResumeSession(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RESUME, enabled).apply()
        _settings.value = _settings.value.copy(autoResumeSession = enabled)
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

    fun updateWeekFilter(week: String) {
        prefs.edit().putString(KEY_WEEK_FILTER, week).apply()
        _settings.value = _settings.value.copy(activeWeekFilter = week)
    }

    fun updateCloudPreset(preset: String) {
        prefs.edit().putString(KEY_CLOUD_PRESET, preset).apply()
        _settings.value = _settings.value.copy(cloudAlbumPreset = preset)
    }

    fun updateFramingMode(mode: String) {
        prefs.edit().putString(KEY_FRAMING_MODE, mode).apply()
        _settings.value = _settings.value.copy(framingMode = mode)
    }

    fun updateMaxCacheSizeMb(sizeMb: Int) {
        prefs.edit().putInt(KEY_MAX_CACHE_MB, sizeMb).apply()
        _settings.value = _settings.value.copy(maxCacheSizeMb = sizeMb)
    }

    fun saveSessionProgression(photoId: Long, index: Int) {
        prefs.edit()
            .putLong(KEY_LAST_PHOTO_ID, photoId)
            .putInt(KEY_LAST_INDEX, index)
            .apply()
        _settings.value = _settings.value.copy(
            lastSessionPhotoId = photoId,
            lastSessionIndex = index
        )
    }

    companion object {
        private const val KEY_AUTO_BOOT = "auto_boot"
        private const val KEY_AUTO_RESUME = "auto_resume"
        private const val KEY_INTERVAL = "slide_interval"
        private const val KEY_CROSSFADE = "crossfade_ms"
        private const val KEY_KEN_BURNS = "ken_burns"
        private const val KEY_SHOW_CLOCK = "show_clock"
        private const val KEY_SHOW_METADATA = "show_metadata"
        private const val KEY_SHUFFLE = "shuffle"
        private const val KEY_CACHE_OFFLINE = "cache_offline"
        private const val KEY_SOURCE_FILTER = "source_filter"
        private const val KEY_WEEK_FILTER = "week_filter"
        private const val KEY_CLOUD_PRESET = "cloud_preset"
        private const val KEY_FRAMING_MODE = "framing_mode"
        private const val KEY_MAX_CACHE_MB = "max_cache_mb"
        private const val KEY_LAST_PHOTO_ID = "last_photo_id"
        private const val KEY_LAST_INDEX = "last_index"

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
