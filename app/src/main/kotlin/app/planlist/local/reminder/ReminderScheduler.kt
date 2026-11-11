package app.planlist.local.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.planlist.core.Plan
import app.planlist.core.PlanStatus
import app.planlist.core.ReminderKind
import app.planlist.core.nextReminderAt
import app.planlist.local.MainActivity

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun reschedule(plan: Plan, nowMillis: Long) {
        cancelAlarm(plan.id)
        if (plan.status != PlanStatus.IN_PROGRESS) {
            Notifier.cancel(context, plan.id)
        }
        val trigger = nextReminderAt(plan, nowMillis) ?: return
        if (!Permissions.exactAlarmGranted(context)) return
        val pending = broadcast(plan.id, trigger)
        try {
            if (plan.reminder == ReminderKind.ALARM) {
                val show = activity(plan.id, MainActivity::class.java, SALT_SHOW)
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, show), pending)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            }
        } catch (_: SecurityException) {
            // The system time permission is missing. The settings screen explains how to grant it.
        }
    }

    fun cancel(id: Long) {
        cancelAlarm(id)
        Notifier.cancel(context, id)
    }

    private fun cancelAlarm(id: Long) {
        alarmManager.cancel(broadcast(id, trigger = 0L))
    }

    fun rescheduleAll(plans: List<Plan>, nowMillis: Long) {
        plans.forEach { reschedule(it, nowMillis) }
    }

    private fun broadcast(id: Long, trigger: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_PLAN_ID, id)
            putExtra(EXTRA_TRIGGER, trigger)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(id, SALT_BROADCAST),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun activity(id: Long, clazz: Class<*>, salt: Int): PendingIntent {
        val intent = Intent(context, clazz).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_PLAN_ID, id)
        }
        return PendingIntent.getActivity(
            context,
            requestCode(id, salt),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_REMIND = "app.planlist.local.REMIND"
        const val EXTRA_PLAN_ID = "plan_id"
        const val EXTRA_TRIGGER = "trigger"
        private const val SALT_BROADCAST = 11
        private const val SALT_SHOW = 29

        fun requestCode(id: Long, salt: Int): Int {
            val mixed = id xor (id ushr 32) xor salt.toLong()
            val code = mixed.toInt()
            return if (code == Int.MIN_VALUE) 0 else code
        }
    }
}
