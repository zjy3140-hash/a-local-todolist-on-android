package app.planlist.local.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.planlist.core.DeadlineParts
import app.planlist.core.IntervalParts
import app.planlist.core.IntervalRules
import app.planlist.core.IntervalUnit
import app.planlist.core.Plan
import app.planlist.core.PlanDraft
import app.planlist.core.PlanStatus
import app.planlist.core.PriorityScale
import app.planlist.core.ReminderKind
import app.planlist.core.RepeatKind
import app.planlist.core.canChangeStatus
import app.planlist.core.clampDeadline
import app.planlist.core.daysInMonth
import app.planlist.core.defaultDeadline
import app.planlist.core.fromEpochMillis
import app.planlist.core.toEpochMillis
import app.planlist.core.validate
import app.planlist.core.yearOptions
import app.planlist.local.R
import app.planlist.local.data.SaveResult
import app.planlist.local.ui.theme.Cinnabar
import app.planlist.local.ui.theme.Cream
import app.planlist.local.ui.theme.Ink
import app.planlist.local.ui.theme.InkSoft
import app.planlist.local.ui.theme.Line
import app.planlist.local.ui.theme.PaperCard
import app.planlist.local.ui.theme.statusColor
import kotlinx.coroutines.launch
import java.time.ZoneId

@Composable
fun PlanEditor(
    existing: Plan?,
    zone: ZoneId,
    nowMillis: Long,
    restart: Boolean = false,
    onSave: suspend (PlanDraft) -> SaveResult,
    onDelete: (suspend () -> Unit)?,
    onBack: () -> Unit,
    onMissing: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    val anchorYear = remember(zone, existing?.id, restart) {
        val basis = if (restart || existing == null) nowMillis else existing.deadlineMillis
        fromEpochMillis(basis, zone).year
    }
    var title by remember(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var parts by remember(existing?.id, zone.id, restart) {
        mutableStateOf(
            if (restart || existing == null) {
                defaultDeadline(nowMillis, zone)
            } else {
                fromEpochMillis(existing.deadlineMillis, zone)
            },
        )
    }
    var status by remember(existing?.id, restart) {
        mutableStateOf(if (restart) PlanStatus.IN_PROGRESS else existing?.status ?: PlanStatus.IN_PROGRESS)
    }
    var reminder by remember(existing?.id) { mutableStateOf(existing?.reminder ?: ReminderKind.POPUP) }
    var priority by remember(existing?.id) { mutableIntStateOf(existing?.priority ?: PriorityScale.DEFAULT) }
    var repeat by remember(existing?.id) { mutableStateOf(existing?.repeat ?: RepeatKind.ONCE) }
    var interval by remember(existing?.id) {
        mutableStateOf(
            if (existing?.repeat == RepeatKind.EVERY) {
                IntervalRules.partsFromSeconds(existing.intervalSeconds)
            } else {
                IntervalParts(1, 0, 0, 0)
            },
        )
    }
    var showError by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val clamped = clampDeadline(parts)
    val composedDeadline = toEpochMillis(clamped, zone)
    val deadlineMillis = if (
        existing != null && fromEpochMillis(existing.deadlineMillis, zone) == clamped
    ) {
        existing.deadlineMillis
    } else {
        composedDeadline
    }
    val draft = PlanDraft(
        id = existing?.id,
        title = title,
        deadlineMillis = deadlineMillis,
        status = if (restart) PlanStatus.IN_PROGRESS else status,
        reminder = reminder,
        priority = priority,
        repeat = repeat,
        interval = interval,
        originalDeadlineMillis = if (restart) null else existing?.deadlineMillis,
        restarting = restart,
    )
    val issue = validate(draft, nowMillis)
    val statusEnabled = !restart && canChangeStatus(deadlineMillis, nowMillis)
    val years = yearOptions(anchorYear, clamped.year)
    val statuses = PlanStatus.entries
    val reminders = ReminderKind.entries
    val repeats = listOf(RepeatKind.ONCE, RepeatKind.EVERY)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = Ink)
            }
            Text(
                stringResource(
                    when {
                        restart -> R.string.restart_plan
                        existing == null -> R.string.add_plan
                        else -> R.string.edit_plan
                    },
                ),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Text(
            stringResource(R.string.system_time) + "  " + formatClock(nowMillis, zone, locale),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 12.dp, bottom = if (restart && existing != null) 4.dp else 12.dp),
        )
        if (restart && existing != null) {
            Text(
                stringResource(
                    R.string.previous_deadline,
                    formatDeadline(existing.deadlineMillis, zone, locale),
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp),
            )
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_plan)) },
            minLines = 2,
            maxLines = 4,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Cinnabar,
                unfocusedBorderColor = Line,
                focusedLabelColor = Cinnabar,
                unfocusedLabelColor = InkSoft,
                cursorColor = Cinnabar,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
                focusedContainerColor = PaperCard,
                unfocusedContainerColor = PaperCard,
            ),
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.field_deadline), style = MaterialTheme.typography.labelSmall)
        Text(
            formatDeadline(deadlineMillis, zone, locale),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberMenu(
                label = stringResource(R.string.unit_year),
                value = clamped.year.toString(),
                options = years.map { it.toString() },
                onSelectIndex = { parts = clampDeadline(clamped.copy(year = years[it])) },
                modifier = Modifier.weight(1.3f),
            )
            NumberMenu(
                label = stringResource(R.string.unit_month),
                value = clamped.month.toString(),
                options = (1..12).map { it.toString() },
                onSelectIndex = { parts = clampDeadline(clamped.copy(month = it + 1)) },
                modifier = Modifier.weight(1f),
            )
            NumberMenu(
                label = stringResource(R.string.unit_date_day),
                value = clamped.day.toString(),
                options = (1..daysInMonth(clamped.year, clamped.month)).map { it.toString() },
                onSelectIndex = { parts = clampDeadline(clamped.copy(day = it + 1)) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberMenu(
                label = stringResource(R.string.unit_hour),
                value = twoDigits(clamped.hour),
                options = (0..23).map { twoDigits(it) },
                onSelectIndex = { parts = clamped.copy(hour = it) },
                modifier = Modifier.weight(1f),
            )
            NumberMenu(
                label = stringResource(R.string.unit_minute),
                value = twoDigits(clamped.minute),
                options = (0..59).map { twoDigits(it) },
                onSelectIndex = { parts = clamped.copy(minute = it) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        MenuButton(
            label = stringResource(R.string.field_status),
            value = statusLabel(status),
            options = statuses.map { statusLabel(it) },
            enabled = statusEnabled,
            valueColor = statusColor(status),
            onSelectIndex = { status = statuses[it] },
        )
        Text(
            stringResource(
                when {
                    restart -> R.string.restart_hint
                    statusEnabled -> R.string.status_hint
                    else -> R.string.status_locked
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp),
        )
        MenuButton(
            label = stringResource(R.string.field_reminder),
            value = reminderLabel(reminder),
            options = reminders.map { reminderLabel(it) },
            onSelectIndex = { reminder = reminders[it] },
        )
        Spacer(Modifier.height(12.dp))
        PrioritySlider(
            value = priority,
            onChange = { priority = it },
            resetKey = existing?.id,
        )
        Spacer(Modifier.height(12.dp))
        MenuButton(
            label = stringResource(R.string.field_count),
            value = if (repeat == RepeatKind.ONCE) {
                stringResource(R.string.repeat_once)
            } else {
                stringResource(R.string.repeat_every)
            },
            options = repeats.map {
                if (it == RepeatKind.ONCE) stringResource(R.string.repeat_once)
                else stringResource(R.string.repeat_every)
            },
            onSelectIndex = { repeat = repeats[it] },
        )
        if (repeat == RepeatKind.EVERY) {
            Spacer(Modifier.height(8.dp))
            IntervalMenus(interval = interval, onChange = { interval = it })
            Text(
                precisionText(interval),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
            Text(
                intervalText(interval),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
        if (showError && issue != null) {
            Text(
                issueText(issue),
                color = Cinnabar,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                if (issue != null) {
                    showError = true
                    return@Button
                }
                if (saving) return@Button
                saving = true
                scope.launch {
                    when (onSave(draft)) {
                        is SaveResult.Saved -> onBack()
                        is SaveResult.Rejected -> {
                            showError = true
                            saving = false
                        }
                        SaveResult.Missing -> onMissing()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cinnabar, contentColor = Cream),
        ) {
            Text(
                stringResource(if (restart) R.string.restart else R.string.save),
                style = MaterialTheme.typography.labelLarge,
                color = Cream,
            )
        }
        if (onDelete != null) {
            TextButton(
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(if (confirmDelete) R.string.confirm_delete else R.string.delete),
                    color = Cinnabar,
                )
            }
        }
    }
}

@Composable
private fun IntervalMenus(
    interval: IntervalParts,
    onChange: (IntervalParts) -> Unit,
) {
    val days = (0..IntervalRules.MAX_DAYS).toList()
    val hourMax = if (interval.days >= IntervalRules.MAX_DAYS) 0 else 23
    val hours = (0..hourMax).toList()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberMenu(
                label = stringResource(R.string.unit_day),
                value = interval.days.toString(),
                options = days.map { it.toString() },
                onSelectIndex = {
                    onChange(IntervalRules.sanitize(days[it], interval.hours, interval.minutes, interval.seconds))
                },
                modifier = Modifier.weight(1f),
            )
            NumberMenu(
                label = stringResource(R.string.unit_hour),
                value = interval.hours.toString(),
                options = hours.map { it.toString() },
                onSelectIndex = {
                    onChange(IntervalRules.sanitize(interval.days, hours[it], interval.minutes, interval.seconds))
                },
                modifier = Modifier.weight(1f),
            )
        }
        if (interval.days == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberMenu(
                    label = stringResource(R.string.unit_minute),
                    value = interval.minutes.toString(),
                    options = (0..59).map { it.toString() },
                    onSelectIndex = {
                        onChange(IntervalRules.sanitize(0, interval.hours, it, interval.seconds))
                    },
                    modifier = Modifier.weight(1f),
                )
                if (interval.hours == 0) {
                    NumberMenu(
                        label = stringResource(R.string.unit_second),
                        value = interval.seconds.toString(),
                        options = (0..59).map { it.toString() },
                        onSelectIndex = {
                            onChange(IntervalRules.sanitize(0, 0, interval.minutes, it))
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun precisionText(interval: IntervalParts): String = when (interval.largestUnit) {
    IntervalUnit.DAY -> stringResource(R.string.precision_day)
    IntervalUnit.HOUR -> stringResource(R.string.precision_hour)
    IntervalUnit.MINUTE, IntervalUnit.SECOND -> stringResource(R.string.precision_second)
}
