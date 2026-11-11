package app.planlist.local.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.planlist.core.PlanStatus
import app.planlist.local.PlanlistApp
import app.planlist.local.prefs.Locales
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMIND) return
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_PLAN_ID, -1L)
        if (id < 0L) return
        val trigger = intent.getLongExtra(ReminderScheduler.EXTRA_TRIGGER, 0L)
        val pending = goAsync()
        val app = context.applicationContext as PlanlistApp
        app.scope.launch {
            try {
                val plan = app.repository.get(id) ?: return@launch
                if (plan.status != PlanStatus.IN_PROGRESS) return@launch
                Notifier.show(Locales.wrap(context), plan)
                val now = System.currentTimeMillis()
                app.scheduler.reschedule(plan, maxOf(now, trigger))
            } finally {
                pending.finish()
            }
        }
    }
}

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }
        val pending = goAsync()
        val app = context.applicationContext as PlanlistApp
        app.scope.launch {
            try {
                app.repository.rescheduleAll(System.currentTimeMillis())
            } finally {
                pending.finish()
            }
        }
    }
}
