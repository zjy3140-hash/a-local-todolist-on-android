package app.planlist.local.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.planlist.core.Plan
import app.planlist.core.ReminderKind
import app.planlist.local.MainActivity
import app.planlist.local.R
import app.planlist.local.prefs.SettingsStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Notifier {
    private const val CHANNEL_POPUP = "plan_popup"
    private const val CHANNEL_ALARM = "plan_alarm"

    fun show(context: Context, plan: Plan) {
        val manager = NotificationManagerCompat.from(context)
        ensureChannels(context)
        val zone = SettingsStore(context).zoneId()
        val whenText = context.getString(R.string.notification_due, formatWhen(plan.deadlineMillis, zone, context))
        val open = openPlan(context, plan.id)
        val alarm = plan.reminder == ReminderKind.ALARM
        val fullScreen = if (alarm) {
            PendingIntent.getActivity(
                context,
                ReminderScheduler.requestCode(plan.id, 41),
                Intent(context, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(ReminderScheduler.EXTRA_PLAN_ID, plan.id)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            null
        }
        val builder = NotificationCompat.Builder(context, if (alarm) CHANNEL_ALARM else CHANNEL_POPUP)
            .setSmallIcon(R.drawable.ic_stat_plan)
            .setContentTitle(plan.title)
            .setContentText(whenText)
            .setWhen(plan.deadlineMillis)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(if (alarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (alarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (alarm && fullScreen != null) {
            builder.setFullScreenIntent(fullScreen, true)
            builder.setOngoing(true)
        }
        if (!Permissions.notificationsGranted(context)) return
        manager.notify(notificationId(plan.id), builder.build())
    }

    fun cancel(context: Context, id: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(id))
    }

    private fun openPlan(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_PLAN_ID, id)
        }
        return PendingIntent.getActivity(
            context,
            ReminderScheduler.requestCode(id, 17),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val popup = NotificationChannel(
            CHANNEL_POPUP,
            context.getString(R.string.channel_popup),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_popup_desc)
        }
        val alarm = NotificationChannel(
            CHANNEL_ALARM,
            context.getString(R.string.channel_alarm),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarm_desc)
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(sound, attributes)
        }
        manager.createNotificationChannel(popup)
        manager.createNotificationChannel(alarm)
    }

    private fun formatWhen(millis: Long, zone: ZoneId, context: Context): String {
        val locale = context.resources.configuration.locales[0]
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", locale)
        return formatter.format(Instant.ofEpochMilli(millis).atZone(zone))
    }

    fun notificationId(id: Long): Int = ReminderScheduler.requestCode(id, 3)
}
