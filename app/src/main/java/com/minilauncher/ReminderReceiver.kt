package com.minilauncher

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preferences = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        val language = LanguageStore(preferences).loadLanguage()
        val localizedContext = context.withAppLanguage(language)

        ensureChannel(localizedContext)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL).orEmpty().ifBlank {
            localizedContext.getString(R.string.reminder_default_app)
        }
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)

        val notification = NotificationCompat.Builder(localizedContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(localizedContext.getString(R.string.reminder_title))
            .setContentText(localizedContext.getString(R.string.reminder_content, minutes, appLabel))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_MINUTES = "extra_minutes"
        private const val CHANNEL_ID = "usage_reminders"
    }
}
