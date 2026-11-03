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
@Config(sdk = [34])
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
    fun `settings repository update crossfade duration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsRepo = SettingsRepository(context)
        settingsRepo.updateCrossfadeDuration(3000)
        assertEquals(3000, settingsRepo.settings.value.crossfadeDurationMs)
    }
}
