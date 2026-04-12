package com.minilauncher

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
        val language = LanguageStore(context.launcherDataStore).loadLanguageBlocking()
        val localizedContext = context.withAppLanguage(language)
        val canPostNotifications =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        ensureChannel(localizedContext)

        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL).orEmpty().ifBlank {
            localizedContext.getString(R.string.reminder_default_app)
        }
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, (System.currentTimeMillis() % Int.MAX_VALUE).toInt())

        if (packageName.isNotBlank()) {
            UsageBlockStore.markBlocked(
                context = localizedContext,
                packageName = packageName,
                appLabel = appLabel,
                minutes = minutes,
            )
        }

        val launcherIntent = Intent(localizedContext, MainActivity::class.java).apply {
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_LABEL, appLabel)
            putExtra(EXTRA_MINUTES, minutes)
            putExtra(EXTRA_TIMEOUT_REACHED, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            localizedContext,
            notificationId,
            launcherIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(localizedContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(localizedContext.getString(R.string.reminder_title))
            .setContentText(localizedContext.getString(R.string.reminder_content, minutes, appLabel))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        if (canPostNotifications) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TIMEOUT_REACHED = "extra_timeout_reached"
        private const val CHANNEL_ID = "usage_reminders"
    }
}
