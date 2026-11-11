package app.planlist.local.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object Permissions {
    fun notificationsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun exactAlarmGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    fun fullScreenGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.canUseFullScreenIntent()
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        start(context, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = packageUri(context)
        })
    }

    fun openFullScreenSettings(context: Context) {
        if (Build.VERSION.SDK_INT < 34) return
        start(context, Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = packageUri(context)
        })
    }

    fun openDateSettings(context: Context) {
        start(context, Intent(Settings.ACTION_DATE_SETTINGS))
    }

    private fun packageUri(context: Context): Uri = Uri.parse("package:${context.packageName}")

    private fun start(context: Context, intent: Intent) {
        runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
