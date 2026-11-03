package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MainActivity
import com.example.data.SettingsRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val settingsRepo = SettingsRepository.getInstance(context)
            if (settingsRepo.settings.value.autoStartOnBoot) {
                Log.d("BootReceiver", "Auto-start enabled. Launching Lumina TV Photo Frame...")
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("EXTRA_BOOT_LAUNCH", true)
                }
                context.startActivity(launchIntent)
            } else {
                Log.d("BootReceiver", "Auto-start on boot is disabled by user setting.")
            }
        }
    }
}
