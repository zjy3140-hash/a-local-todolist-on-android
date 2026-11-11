package app.planlist.core

enum class IntervalUnit {
    DAY,
    HOUR,
    MINUTE,
    SECOND,
}

/**
 * Repeat gap for "once per interval".
 *
 * Precision follows the largest unit:
 * - days: exact to the hour (minutes and seconds are dropped)
 * - hours: exact to the minute (seconds are dropped)
 * - under one hour: exact to the second
 *
 * The total cannot exceed 30 days.
 */
data class IntervalParts(
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
) {
    val totalSeconds: Long
        get() = days * 86_400L + hours * 3_600L + minutes * 60L + seconds

    val largestUnit: IntervalUnit
        get() = when {
            days > 0 -> IntervalUnit.DAY
            hours > 0 -> IntervalUnit.HOUR
            minutes > 0 -> IntervalUnit.MINUTE
            else -> IntervalUnit.SECOND
        }
}

object IntervalRules {
    const val MAX_DAYS = 30
    const val MAX_SECONDS = MAX_DAYS * 86_400L

    fun sanitize(days: Int, hours: Int, minutes: Int, seconds: Int): IntervalParts {
        val safeDays = days.coerceIn(0, MAX_DAYS)
        if (safeDays > 0) {
            val safeHours = if (safeDays >= MAX_DAYS) 0 else hours.coerceIn(0, 23)
            return IntervalParts(safeDays, safeHours, 0, 0)
        }
        val safeHours = hours.coerceIn(0, 23)
        if (safeHours > 0) {
            return IntervalParts(0, safeHours, minutes.coerceIn(0, 59), 0)
        }
        return IntervalParts(0, 0, minutes.coerceIn(0, 59), seconds.coerceIn(0, 59))
    }

    fun isPositiveWithinLimit(parts: IntervalParts): Boolean {
        val total = parts.totalSeconds
        return total in 1..MAX_SECONDS
    }

    fun partsFromSeconds(totalSeconds: Long): IntervalParts {
        val total = totalSeconds.coerceIn(0, MAX_SECONDS)
        if (total >= 86_400L) {
            val days = (total / 86_400L).toInt()
            val hours = ((total % 86_400L) / 3_600L).toInt()
            return IntervalParts(days, hours, 0, 0)
        }
        if (total >= 3_600L) {
            val hours = (total / 3_600L).toInt()
            val minutes = ((total % 3_600L) / 60L).toInt()
            return IntervalParts(0, hours, minutes, 0)
        }
        val minutes = (total / 60L).toInt()
        val seconds = (total % 60L).toInt()
        return IntervalParts(0, 0, minutes, seconds)
    }
}
