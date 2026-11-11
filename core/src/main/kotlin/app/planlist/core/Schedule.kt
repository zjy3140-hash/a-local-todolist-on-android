package app.planlist.core

/**
 * Next wall-clock instant to remind, or null when nothing should be scheduled.
 *
 * Once: fire at the deadline.
 * Every interval: fire on the interval grid anchored at creation, and also at
 * the deadline when the grid would otherwise step past it. Stops at the deadline.
 */
fun nextReminderAt(plan: Plan, nowMillis: Long): Long? {
    if (plan.status != PlanStatus.IN_PROGRESS) return null
    if (nowMillis >= plan.deadlineMillis) return null
    if (plan.repeat == RepeatKind.ONCE || plan.intervalSeconds <= 0L) {
        return plan.deadlineMillis
    }
    val intervalMs = plan.intervalSeconds * 1_000L
    if (intervalMs <= 0L) return plan.deadlineMillis
    val anchor = plan.createdAtMillis
    val next = if (anchor + intervalMs > nowMillis) {
        anchor + intervalMs
    } else {
        val elapsed = nowMillis - anchor
        val steps = elapsed / intervalMs + 1
        val increment = steps * intervalMs
        if (increment <= 0L || anchor > Long.MAX_VALUE - increment) {
            plan.deadlineMillis
        } else {
            anchor + increment
        }
    }
    if (next <= nowMillis) return if (nowMillis < plan.deadlineMillis) plan.deadlineMillis else null
    return if (next < plan.deadlineMillis) next else plan.deadlineMillis
}
