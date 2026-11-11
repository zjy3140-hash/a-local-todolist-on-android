package app.planlist.core

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

data class DeadlineParts(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
)

fun daysInMonth(year: Int, month: Int): Int =
    YearMonth.of(year.coerceAtLeast(1), month.coerceIn(1, 12)).lengthOfMonth()

fun clampDeadline(parts: DeadlineParts): DeadlineParts {
    val month = parts.month.coerceIn(1, 12)
    val day = parts.day.coerceIn(1, daysInMonth(parts.year, month))
    return parts.copy(
        month = month,
        day = day,
        hour = parts.hour.coerceIn(0, 23),
        minute = parts.minute.coerceIn(0, 59),
    )
}

fun toEpochMillis(parts: DeadlineParts, zone: ZoneId): Long {
    val safe = clampDeadline(parts)
    return ZonedDateTime.of(
        safe.year,
        safe.month,
        safe.day,
        safe.hour,
        safe.minute,
        0,
        0,
        zone,
    ).toInstant().toEpochMilli()
}

fun fromEpochMillis(millis: Long, zone: ZoneId): DeadlineParts {
    val zoned = Instant.ofEpochMilli(millis).atZone(zone)
    return DeadlineParts(
        year = zoned.year,
        month = zoned.monthValue,
        day = zoned.dayOfMonth,
        hour = zoned.hour,
        minute = zoned.minute,
    )
}

fun defaultDeadline(nowMillis: Long, zone: ZoneId): DeadlineParts {
    val zoned = Instant.ofEpochMilli(nowMillis).atZone(zone).plusHours(1).withSecond(0).withNano(0)
    return DeadlineParts(zoned.year, zoned.monthValue, zoned.dayOfMonth, zoned.hour, zoned.minute)
}

fun yearOptions(anchorYear: Int, selectedYear: Int, span: Int = 10): List<Int> {
    val start = minOf(anchorYear, selectedYear)
    val end = maxOf(anchorYear + span, selectedYear)
    return (start..end).toList()
}
