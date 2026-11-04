package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SettingsRepository

/**
 * Handles Android TV startup events and launches Lumina TV Photo Frame.
 * Uses both direct activity launch and high-priority Full-Screen Intent notification
 * to reliably overcome background start restrictions on modern Android TV OS (API 29–36).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver received action: $action")

        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )

        if (action in validActions) {
            val settingsRepo = SettingsRepository.getInstance(context)
            if (!settingsRepo.settings.value.autoStartOnBoot) {
                Log.d(TAG, "Auto-start on boot is disabled by user setting.")
                return
            }

            Log.d(TAG, "Auto-start enabled. Launching Lumina TV Photo Frame...")
            launchAppOnTv(context)
        }
    }

    private fun launchAppOnTv(context: Context) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            )
            putExtra("EXTRA_BOOT_LAUNCH", true)
        }

        // 1. Attempt direct activity launch
        try {
            context.startActivity(launchIntent)
            Log.d(TAG, "Direct startActivity executed successfully.")
        } catch (t: Throwable) {
            Log.w(TAG, "Direct startActivity failed or was restricted: ${t.message}")
        }

        // 2. Also send high-priority Full-Screen Intent notification
        // Modern Android TV OS reliably surfaces full-screen intents on boot
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            val channelId = "tv_boot_autostart"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "TV Auto-start",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Launches photo frame when TV powers on"
                    setSound(null, null)
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Lumina TV Photo Frame")
                .setContentText("Starting ambient slideshow...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
            Log.d(TAG, "Full-screen boot notification dispatched successfully.")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to dispatch full-screen boot notification", t)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
