package app.planlist.local

import android.app.Application
import android.content.Context
import app.planlist.local.data.AppDatabase
import app.planlist.local.data.PlanRepository
import app.planlist.local.prefs.Locales
import app.planlist.local.prefs.SettingsStore
import app.planlist.local.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PlanlistApp : Application() {
    lateinit var settings: SettingsStore
        private set
    lateinit var repository: PlanRepository
        private set
    lateinit var scheduler: ReminderScheduler
        private set
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(Locales.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        Locales.apply(this)
        settings = SettingsStore(this)
        scheduler = ReminderScheduler(this)
        repository = PlanRepository(AppDatabase.create(this).planDao(), scheduler)
    }
}
