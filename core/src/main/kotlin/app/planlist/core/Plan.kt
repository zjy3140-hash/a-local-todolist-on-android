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

/** Done and Give up leave the active list and stop being reminded. */
fun isInactive(status: PlanStatus): Boolean =
    status == PlanStatus.DONE || status == PlanStatus.GIVE_UP

fun activePlans(plans: List<Plan>): List<Plan> =
    sortPlans(plans.filter { !isInactive(it.status) })

fun inactivePlans(plans: List<Plan>): List<Plan> =
    sortPlans(plans.filter { isInactive(it.status) })

/** Status may change only strictly before the deadline. */
fun canChangeStatus(deadlineMillis: Long, nowMillis: Long): Boolean =
    nowMillis < deadlineMillis

/** An inactive plan cannot be flipped back from the status menu. Restart does that. */
fun canSetStatus(current: PlanStatus, deadlineMillis: Long, nowMillis: Long): Boolean =
    !isInactive(current) && canChangeStatus(deadlineMillis, nowMillis)
