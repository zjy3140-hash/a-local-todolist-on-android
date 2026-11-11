package app.planlist.local.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.planlist.core.DraftIssue
import app.planlist.core.IntervalParts
import app.planlist.core.IntervalRules
import app.planlist.core.PlanStatus
import app.planlist.core.ReminderKind
import app.planlist.core.RepeatKind
import app.planlist.local.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDeadline(millis: Long, zone: ZoneId, locale: Locale): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", locale)
    val zoned = Instant.ofEpochMilli(millis).atZone(zone)
    return formatter.format(zoned) + " · " + zone.id
}

fun formatClock(millis: Long, zone: ZoneId, locale: Locale): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", locale)
    return formatter.format(Instant.ofEpochMilli(millis).atZone(zone))
}

fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

@Composable
fun statusLabel(status: PlanStatus): String = when (status) {
    PlanStatus.IN_PROGRESS -> stringResource(R.string.status_in_progress)
    PlanStatus.DONE -> stringResource(R.string.status_done)
    PlanStatus.GIVE_UP -> stringResource(R.string.status_give_up)
}

@Composable
fun reminderLabel(kind: ReminderKind): String = when (kind) {
    ReminderKind.POPUP -> stringResource(R.string.reminder_popup)
    ReminderKind.ALARM -> stringResource(R.string.reminder_alarm)
}

@Composable
fun repeatText(repeat: RepeatKind, intervalSeconds: Long): String {
    if (repeat == RepeatKind.ONCE || intervalSeconds <= 0L) {
        return stringResource(R.string.repeat_once)
    }
    return intervalText(IntervalRules.partsFromSeconds(intervalSeconds))
}

@Composable
fun intervalText(parts: IntervalParts): String = when {
    parts.days > 0 && parts.hours > 0 ->
        stringResource(R.string.every_days_hours, parts.days, parts.hours)
    parts.days > 0 -> stringResource(R.string.every_days, parts.days)
    parts.hours > 0 && parts.minutes > 0 ->
        stringResource(R.string.every_hours_minutes, parts.hours, parts.minutes)
    parts.hours > 0 -> stringResource(R.string.every_hours, parts.hours)
    parts.minutes > 0 && parts.seconds > 0 ->
        stringResource(R.string.every_minutes_seconds, parts.minutes, parts.seconds)
    parts.minutes > 0 -> stringResource(R.string.every_minutes, parts.minutes)
    else -> stringResource(R.string.every_seconds, parts.seconds)
}

@Composable
fun issueText(issue: DraftIssue): String = when (issue) {
    DraftIssue.EmptyTitle -> stringResource(R.string.error_title)
    DraftIssue.DeadlineNotInFuture -> stringResource(R.string.error_deadline)
    DraftIssue.IntervalInvalid -> stringResource(R.string.error_interval)
}
