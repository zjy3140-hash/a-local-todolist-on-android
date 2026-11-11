package app.planlist.local

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import app.planlist.local.prefs.Locales
import app.planlist.local.reminder.Permissions
import app.planlist.local.ui.PlanlistRoot
import app.planlist.local.ui.theme.PlanlistTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val pendingPlanId: MutableState<Long?> = mutableStateOf(null)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Locales.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        pendingPlanId.value = intent.planIdOrNull()
        val app = application as PlanlistApp
        setContent {
            PlanlistTheme {
                PlanlistRoot(
                    app = app,
                    pendingPlanId = pendingPlanId,
                    onLanguageChange = { tag ->
                        Locales.setTag(this, tag)
                        Locales.apply(application as Application)
                        recreate()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPlanId.value = intent.planIdOrNull()
    }

    override fun onResume() {
        super.onResume()
        val app = application as PlanlistApp
        maybeAskForNotifications(app)
        app.scope.launch {
            app.repository.rescheduleAll(System.currentTimeMillis())
        }
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT < 33) return
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun maybeAskForNotifications(app: PlanlistApp) {
        if (Build.VERSION.SDK_INT < 33) return
        if (Permissions.notificationsGranted(this)) return
        if (app.settings.wasNotificationAsked()) return
        app.settings.setNotificationAsked()
        requestNotifications()
    }

    companion object {
        const val EXTRA_PLAN_ID = "app.planlist.local.PLAN_ID"
    }
}

private fun Intent.planIdOrNull(): Long? {
    if (!hasExtra(MainActivity.EXTRA_PLAN_ID)) return null
    return getLongExtra(MainActivity.EXTRA_PLAN_ID, Long.MIN_VALUE).takeIf { it >= 0L }
}
