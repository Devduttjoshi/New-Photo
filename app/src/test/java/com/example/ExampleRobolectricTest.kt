package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches Lumina TV Photo Frame`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Lumina TV Photo Frame", appName)
    }

    @Test
    fun `settings repository default autoStartOnBoot is true`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsRepo = SettingsRepository(context)
        assertTrue(settingsRepo.settings.value.autoStartOnBoot)
        assertEquals(8, settingsRepo.settings.value.slideIntervalSeconds)
        assertEquals(1500, settingsRepo.settings.value.crossfadeDurationMs)
    }

    @Test
    fun `qr code generator creates valid bitmap for pairing url`() {
        val testUrl = "http://192.168.1.100:8080"
        val bitmap = com.example.util.QrCodeGenerator.generateQrBitmap(testUrl, 256)
        org.junit.Assert.assertNotNull(bitmap)
        org.junit.Assert.assertTrue(bitmap.width >= 200)
        org.junit.Assert.assertTrue(bitmap.height >= 200)
    }

    @Test
    fun `slideshow view model instantiates successfully without exception`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.SlideshowViewModel(app)
        org.junit.Assert.assertNotNull(vm)
        org.junit.Assert.assertNotNull(vm.uiState.value)
    }

    @Test
    fun `settings framing mode defaults to AMBIENT_FIT and can be updated`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsRepo = SettingsRepository(context)
        assertEquals("AMBIENT_FIT", settingsRepo.settings.value.framingMode)
        settingsRepo.updateFramingMode("FILL_CROP")
        assertEquals("FILL_CROP", settingsRepo.settings.value.framingMode)
    }

    @Test
    fun `browser state opens and closes cleanly in view model`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.SlideshowViewModel(app)
        org.junit.Assert.assertFalse(vm.uiState.value.isBrowserOpen)

        vm.openBrowser("https://photos.google.com")
        org.junit.Assert.assertTrue(vm.uiState.value.isBrowserOpen)
        assertEquals("https://photos.google.com", vm.uiState.value.browserUrl)

        vm.closeBrowser()
        org.junit.Assert.assertFalse(vm.uiState.value.isBrowserOpen)
    }

    @Test
    fun `settings session progression persistence updates state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsRepo = SettingsRepository(context)
        assertTrue(settingsRepo.settings.value.autoResumeSession)
        assertEquals(250, settingsRepo.settings.value.maxCacheSizeMb)

        settingsRepo.saveSessionProgression(photoId = 42L, index = 7)
        assertEquals(42L, settingsRepo.settings.value.lastSessionPhotoId)
        assertEquals(7, settingsRepo.settings.value.lastSessionIndex)

        settingsRepo.updateWeekFilter("Oct 2024 • Week 43")
        assertEquals("Oct 2024 • Week 43", settingsRepo.settings.value.activeWeekFilter)

        settingsRepo.updateMaxCacheSizeMb(500)
        assertEquals(500, settingsRepo.settings.value.maxCacheSizeMb)
    }

    @Test
    fun `sanitizeUrl cleans whitespace, quotes and missing scheme`() {
        val testWithQuotes = "\"https://photos.google.com/album123\""
        assertEquals("https://photos.google.com/album123", com.example.data.CloudSyncManager.sanitizeUrl(testWithQuotes))

        val testWithSpaces = "  https://photos.app.goo.gl/test   \n"
        assertEquals("https://photos.app.goo.gl/test", com.example.data.CloudSyncManager.sanitizeUrl(testWithSpaces))

        val testWithoutScheme = "photos.app.goo.gl/test"
        assertEquals("https://photos.app.goo.gl/test", com.example.data.CloudSyncManager.sanitizeUrl(testWithoutScheme))
    }

    @Test
    fun `boot receiver receives boot broadcast without exception`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiver = com.example.receiver.BootReceiver()
        val intent = android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)
    }

    @Test
    fun `settings savedGooglePhotosUrl defaults and persists across instances`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo1 = SettingsRepository(context)
        assertEquals(com.example.data.CloudSyncManager.DEFAULT_GOOGLE_PHOTOS_URL, repo1.settings.value.savedGooglePhotosUrl)

        val customUrl = "https://photos.app.goo.gl/customAlbumLink123"
        repo1.updateSavedGooglePhotosUrl(customUrl)
        assertEquals(customUrl, repo1.settings.value.savedGooglePhotosUrl)

        val repo2 = SettingsRepository(context)
        assertEquals(customUrl, repo2.settings.value.savedGooglePhotosUrl)
    }

    @Test
    fun `slideshow view model updates source filter and library state`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.SlideshowViewModel(app)
        vm.selectSourceFilter("USB")
        assertEquals("USB", vm.settings.value.activeSourceFilter)

        vm.openLibrary()
        assertTrue(vm.uiState.value.isLibraryOpen)
        vm.closeLibrary()
        org.junit.Assert.assertFalse(vm.uiState.value.isLibraryOpen)
    }
}
