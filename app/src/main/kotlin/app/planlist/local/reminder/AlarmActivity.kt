package app.planlist.local.reminder

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.planlist.core.Plan
import app.planlist.core.PlanStatus
import app.planlist.core.canChangeStatus
import app.planlist.local.PlanlistApp
import app.planlist.local.R
import app.planlist.local.prefs.Locales
import app.planlist.local.ui.formatClock
import app.planlist.local.ui.formatDeadline
import app.planlist.local.ui.statusLabel
import app.planlist.local.ui.theme.Cinnabar
import app.planlist.local.ui.theme.Cream
import app.planlist.local.ui.theme.Ink
import app.planlist.local.ui.theme.Paper
import app.planlist.local.ui.theme.PlanlistTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId

class AlarmActivity : ComponentActivity() {
    private val planId = mutableLongStateOf(-1L)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Locales.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        planId.longValue = readPlanId(intent)
        Notifier.cancel(this, planId.longValue)
        val app = application as PlanlistApp
        setContent {
            PlanlistTheme {
                AlarmScreen(
                    app = app,
                    planId = planId.longValue,
                    onFinish = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val id = readPlanId(intent)
        Notifier.cancel(this, id)
        planId.longValue = id
    }

    private fun readPlanId(intent: Intent): Long =
        intent.getLongExtra(ReminderScheduler.EXTRA_PLAN_ID, -1L)
}

@Composable
private fun AlarmScreen(
    app: PlanlistApp,
    planId: Long,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var plan by remember(planId) { mutableStateOf<Plan?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(planId) {
        plan = app.repository.get(planId)
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    DisposableEffect(planId) {
        val ringtone = alarmRingtone(context)
        ringtone?.play()
        val vibrator = vibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 350), 0))
        }
        onDispose {
            ringtone?.stop()
            vibrator.cancel()
        }
    }
    val zone = remember { app.settings.zoneId() }
    val current = plan
    Column(Modifier.fillMaxSize().background(Paper)) {
        Column(Modifier.fillMaxWidth().background(Cinnabar).padding(horizontal = 24.dp, vertical = 36.dp)) {
            Text(stringResource(R.string.alarm_title), color = Cream, style = MaterialTheme.typography.labelLarge)
            Text(
                formatClock(now, zone, locale),
                color = Cream,
                style = MaterialTheme.typography.displaySmall,
            )
        }
        Column(Modifier.padding(24.dp)) {
            if (current == null) {
                Text(stringResource(R.string.missing_plan), style = MaterialTheme.typography.titleLarge)
            } else {
                Text(current.title, style = MaterialTheme.typography.headlineMedium, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    formatDeadline(current.deadlineMillis, zone, locale),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(statusLabel(current.status), style = MaterialTheme.typography.bodyMedium)
                if (canChangeStatus(current.deadlineMillis, now)) {
                    Spacer(Modifier.height(20.dp))
                    StatusAction(stringResource(R.string.status_done)) {
                        app.scope.launch {
                            app.repository.setStatus(current.id, PlanStatus.DONE, System.currentTimeMillis())
                            onFinish()
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusAction(stringResource(R.string.status_give_up)) {
                        app.scope.launch {
                            app.repository.setStatus(current.id, PlanStatus.GIVE_UP, System.currentTimeMillis())
                            onFinish()
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cinnabar, contentColor = Cream),
            ) {
                Text(stringResource(R.string.dismiss), color = Cream)
            }
        }
    }
}

@Composable
private fun StatusAction(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text(label, color = Cinnabar)
    }
}

private fun alarmRingtone(context: Context): Ringtone? {
    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: return null
    return RingtoneManager.getRingtone(context, uri)?.apply {
        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isLooping = true
        }
    }
}

private fun vibrator(context: Context): Vibrator {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}
