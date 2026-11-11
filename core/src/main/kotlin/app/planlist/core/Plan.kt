package app.planlist.core

enum class PlanStatus {
    IN_PROGRESS,
    DONE,
    GIVE_UP,
}

enum class ReminderKind {
    POPUP,
    ALARM,
}

enum class RepeatKind {
    ONCE,
    EVERY,
}

data class Plan(
    val id: Long,
    val title: String,
    val deadlineMillis: Long,
    val status: PlanStatus,
    val reminder: ReminderKind,
    val priority: Int,
    val repeat: RepeatKind,
    val intervalSeconds: Long,
    val createdAtMillis: Long,
)

object PriorityScale {
    const val MIN = 1
    const val MAX = 10
    const val DEFAULT = 5

    fun coerce(value: Int): Int = value.coerceIn(MIN, MAX)
}

/** Higher priority first. Equal priority: the sooner deadline first. */
fun sortPlans(plans: List<Plan>): List<Plan> =
    plans.sortedWith(
        compareByDescending<Plan> { it.priority }
            .thenBy { it.deadlineMillis }
            .thenBy { it.id },
    )

/** Status may change only strictly before the deadline. */
fun canChangeStatus(deadlineMillis: Long, nowMillis: Long): Boolean =
    nowMillis < deadlineMillis
