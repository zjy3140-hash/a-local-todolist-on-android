package app.planlist.local.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.planlist.core.Plan
import app.planlist.core.PlanStatus
import app.planlist.core.activePlans
import app.planlist.core.canChangeStatus
import app.planlist.core.inactivePlans
import app.planlist.local.R
import app.planlist.local.ui.theme.Cinnabar
import app.planlist.local.ui.theme.Cream
import app.planlist.local.ui.theme.Ink
import app.planlist.local.ui.theme.InkSoft
import app.planlist.local.ui.theme.Line
import app.planlist.local.ui.theme.Paper
import app.planlist.local.ui.theme.PaperCard
import app.planlist.local.ui.theme.priorityColor
import app.planlist.local.ui.theme.statusColor
import kotlinx.coroutines.launch
import java.time.ZoneId

@Composable
fun PlanScreen(
    plans: List<Plan>,
    zone: ZoneId,
    nowMillis: Long,
    permissionsMissing: Boolean,
    onOpenSettings: () -> Unit,
    onOpenPlan: (Long) -> Unit,
    onStatus: (Plan, PlanStatus) -> Unit,
    onDelete: (Plan) -> Unit,
    onRestart: (Plan) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val active = activePlans(plans)
    val inactive = inactivePlans(plans)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(top = 8.dp, bottom = 6.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
                    Text(stringResource(R.string.sort_hint), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = Ink,
                    )
                }
            }
        }
        if (permissionsMissing) {
            item {
                Text(
                    stringResource(R.string.banner_permissions),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Cinnabar.copy(alpha = 0.1f))
                        .clickable(onClick = onOpenSettings)
                        .padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Cinnabar,
                )
            }
        }
        if (active.isEmpty() && inactive.isEmpty()) {
            item(key = "empty") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.empty_body), style = MaterialTheme.typography.bodyLarge, color = InkSoft)
                }
            }
        } else if (active.isEmpty()) {
            item(key = "active-empty") {
                Text(
                    stringResource(R.string.active_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSoft,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        items(active, key = { it.id }) { plan ->
            PlanCard(
                plan = plan,
                deadline = formatDeadline(plan.deadlineMillis, zone, locale),
                nowMillis = nowMillis,
                onOpen = { onOpenPlan(plan.id) },
                onStatus = { onStatus(plan, it) },
            )
        }
        item(key = "inactive-header") {
            Column(Modifier.padding(top = 22.dp, bottom = 2.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                Text(
                    stringResource(R.string.inactive_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(R.string.inactive_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (inactive.isEmpty()) {
                    Text(
                        stringResource(R.string.inactive_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        items(inactive, key = { "inactive-${it.id}" }) { plan ->
            InactiveCard(
                plan = plan,
                deadline = formatDeadline(plan.deadlineMillis, zone, locale),
                onDelete = { onDelete(plan) },
                onRestart = { onRestart(plan) },
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    deadline: String,
    nowMillis: Long,
    onOpen: () -> Unit,
    onStatus: (PlanStatus) -> Unit,
) {
    val locked = !canChangeStatus(plan.deadlineMillis, nowMillis)
    val statuses = PlanStatus.entries
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .background(PaperCard)
            .clickable(onClick = onOpen)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(priorityColor(plan.priority).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    plan.priority.toString(),
                    color = priorityColor(plan.priority),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    plan.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(deadline, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                Text(
                    reminderLabel(plan.reminder) + "  ·  " + repeatText(plan.repeat, plan.intervalSeconds),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (locked) {
            Text(statusLabel(plan.status), color = statusColor(plan.status), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.status_locked), style = MaterialTheme.typography.bodySmall)
        } else {
            MenuButton(
                label = stringResource(R.string.field_status),
                value = statusLabel(plan.status),
                options = statuses.map { statusLabel(it) },
                valueColor = statusColor(plan.status),
                onSelectIndex = { onStatus(statuses[it]) },
            )
        }
    }
}

@Composable
private fun InactiveCard(
    plan: Plan,
    deadline: String,
    onDelete: () -> Unit,
    onRestart: () -> Unit,
) {
    var confirmDelete by remember(plan.id) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .background(Paper)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(statusColor(plan.status).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    plan.priority.toString(),
                    color = statusColor(plan.status),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    plan.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = InkSoft,
                )
                Text(deadline, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                Text(
                    statusLabel(plan.status),
                    color = statusColor(plan.status),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    if (!confirmDelete) confirmDelete = true else onDelete()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    stringResource(if (confirmDelete) R.string.confirm_delete else R.string.delete),
                    color = Cinnabar,
                )
            }
            Button(
                onClick = onRestart,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cinnabar, contentColor = Cream),
            ) {
                Text(stringResource(R.string.restart), color = Cream)
            }
        }
    }
}

@Composable
fun InactivePlanPage(
    plan: Plan,
    zone: ZoneId,
    onRestart: () -> Unit,
    onDelete: suspend () -> Unit,
    onBack: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    var confirmDelete by remember(plan.id) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Ink,
                )
            }
            Text(stringResource(R.string.inactive_title), style = MaterialTheme.typography.headlineMedium)
        }
        Text(plan.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Text(
            statusLabel(plan.status),
            color = statusColor(plan.status),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            formatDeadline(plan.deadlineMillis, zone, locale),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            stringResource(R.string.inactive_detail),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cinnabar, contentColor = Cream),
        ) {
            Text(stringResource(R.string.restart), color = Cream)
        }
        OutlinedButton(
            onClick = {
                if (!confirmDelete) {
                    confirmDelete = true
                } else {
                    scope.launch {
                        onDelete()
                        onBack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                stringResource(if (confirmDelete) R.string.confirm_delete else R.string.delete),
                color = Cinnabar,
            )
        }
    }
}
