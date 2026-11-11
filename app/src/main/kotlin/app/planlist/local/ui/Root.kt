package app.planlist.local.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.planlist.core.Plan
import app.planlist.core.isInactive
import app.planlist.local.MainActivity
import app.planlist.local.PlanlistApp
import app.planlist.local.R
import app.planlist.local.reminder.Permissions
import app.planlist.local.ui.theme.Cinnabar
import app.planlist.local.ui.theme.Cream
import app.planlist.local.ui.theme.InkSoft
import app.planlist.local.ui.theme.Line
import app.planlist.local.ui.theme.Paper
import app.planlist.local.ui.theme.PaperCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SECTION_PLAN = "plan"
private const val SECTION_SETTINGS = "settings"
private const val EDITOR_CLOSED = -2L

@Composable
fun PlanlistRoot(
    app: PlanlistApp,
    pendingPlanId: MutableState<Long?>,
    onLanguageChange: (String) -> Unit,
) {
    val context = LocalContext.current
    var section by rememberSaveable { mutableStateOf(SECTION_PLAN) }
    var editorKey by rememberSaveable { mutableStateOf(EDITOR_CLOSED) }
    var restarting by rememberSaveable { mutableStateOf(false) }
    var settingsVersion by remember { mutableIntStateOf(0) }
    val zone = remember(settingsVersion, section) { app.settings.zoneId() }
    val plansFlow = remember { app.repository.observe() }
    val plans by plansFlow.collectAsStateWithLifecycle(emptyList())
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val resumeTick = rememberResumeTick()
    val permissionsMissing = remember(resumeTick) {
        !Permissions.exactAlarmGranted(context) || !Permissions.notificationsGranted(context)
    }
    val pending = pendingPlanId.value
    LaunchedEffect(pending) {
        if (pending != null && pending >= 0L) {
            editorKey = pending
            pendingPlanId.value = null
            (context as? MainActivity)?.intent?.removeExtra(MainActivity.EXTRA_PLAN_ID)
        }
    }
    val editing = editorKey != EDITOR_CLOSED
    BackHandler(enabled = editing) {
        editorKey = EDITOR_CLOSED
        restarting = false
    }

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            if (!editing) {
                Column {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    NavigationBar(containerColor = PaperCard, tonalElevation = 0.dp) {
                        NavigationBarItem(
                            selected = section == SECTION_PLAN,
                            onClick = { section = SECTION_PLAN },
                            icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                            label = { Text(stringResource(R.string.plan_section)) },
                            colors = navColors(),
                        )
                        NavigationBarItem(
                            selected = section == SECTION_SETTINGS,
                            onClick = { section = SECTION_SETTINGS },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            label = { Text(stringResource(R.string.settings_section)) },
                            colors = navColors(),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!editing && section == SECTION_PLAN) {
                FloatingActionButton(
                    onClick = { editorKey = -1L },
                    containerColor = Cinnabar,
                    contentColor = Cream,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (editing) {
                EditorRoute(
                    app = app,
                    planId = editorKey.takeIf { it >= 0L },
                    zone = zone,
                    nowMillis = now,
                    restart = restarting,
                    onRestart = { restarting = true },
                    onBack = {
                        editorKey = EDITOR_CLOSED
                        restarting = false
                    },
                )
            } else if (section == SECTION_SETTINGS) {
                SettingsScreen(
                    settings = app.settings,
                    nowMillis = now,
                    onLanguage = onLanguageChange,
                    onChanged = { settingsVersion += 1 },
                )
            } else {
                PlanScreen(
                    plans = plans,
                    zone = zone,
                    nowMillis = now,
                    permissionsMissing = permissionsMissing,
                    onOpenSettings = { section = SECTION_SETTINGS },
                    onOpenPlan = {
                        restarting = false
                        editorKey = it
                    },
                    onStatus = { plan, status ->
                        app.scope.launch {
                            app.repository.setStatus(plan.id, status, System.currentTimeMillis())
                        }
                    },
                    onDelete = { plan ->
                        app.scope.launch { app.repository.delete(plan.id) }
                    },
                    onRestart = { plan ->
                        restarting = true
                        editorKey = plan.id
                    },
                )
            }
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Cinnabar,
    selectedTextColor = Cinnabar,
    indicatorColor = Cinnabar.copy(alpha = 0.14f),
    unselectedIconColor = InkSoft,
    unselectedTextColor = InkSoft,
)

@Composable
private fun EditorRoute(
    app: PlanlistApp,
    planId: Long?,
    zone: java.time.ZoneId,
    nowMillis: Long,
    restart: Boolean,
    onRestart: () -> Unit,
    onBack: () -> Unit,
) {
    var existing by remember { androidx.compose.runtime.mutableStateOf<Plan?>(null) }
    var missing by remember { androidx.compose.runtime.mutableStateOf(false) }
    var ready by remember { androidx.compose.runtime.mutableStateOf(planId == null) }
    LaunchedEffect(planId) {
        if (planId == null) {
            existing = null
            missing = false
            ready = true
        } else {
            val plan = app.repository.get(planId)
            existing = plan
            missing = plan == null
            ready = true
        }
    }
    if (!ready) return
    if (missing) {
        Column(Modifier.padding(24.dp)) {
            Text(stringResource(R.string.missing_plan), style = MaterialTheme.typography.titleLarge)
            androidx.compose.material3.TextButton(onClick = onBack) {
                Text(stringResource(R.string.back), color = Cinnabar)
            }
        }
        return
    }
    val loaded = existing
    if (loaded != null && isInactive(loaded.status) && !restart) {
        InactivePlanPage(
            plan = loaded,
            zone = zone,
            onRestart = onRestart,
            onDelete = suspend { app.repository.delete(loaded.id) },
            onBack = onBack,
        )
        return
    }
    PlanEditor(
        existing = existing,
        zone = zone,
        nowMillis = nowMillis,
        restart = restart && loaded != null && isInactive(loaded.status),
        onSave = { draft -> app.repository.save(draft, System.currentTimeMillis()) },
        onDelete = planId?.let { id -> suspend { app.repository.delete(id) } },
        onBack = onBack,
        onMissing = onBack,
    )
}
