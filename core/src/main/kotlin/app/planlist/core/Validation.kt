package app.planlist.core

data class PlanDraft(
    val id: Long?,
    val title: String,
    val deadlineMillis: Long,
    val status: PlanStatus,
    val reminder: ReminderKind,
    val priority: Int,
    val repeat: RepeatKind,
    val interval: IntervalParts,
    val originalDeadlineMillis: Long?,
    val restarting: Boolean = false,
)

sealed interface DraftIssue {
    data object EmptyTitle : DraftIssue
    data object DeadlineNotInFuture : DraftIssue
    data object IntervalInvalid : DraftIssue
}

fun validate(draft: PlanDraft, nowMillis: Long): DraftIssue? {
    if (draft.title.trim().isEmpty()) return DraftIssue.EmptyTitle
    val keepsOriginal = !draft.restarting &&
        draft.originalDeadlineMillis != null &&
        draft.deadlineMillis == draft.originalDeadlineMillis
    if (draft.deadlineMillis <= nowMillis && !keepsOriginal) {
        return DraftIssue.DeadlineNotInFuture
    }
    if (draft.repeat == RepeatKind.EVERY) {
        val parts = IntervalRules.sanitize(
            draft.interval.days,
            draft.interval.hours,
            draft.interval.minutes,
            draft.interval.seconds,
        )
        if (!IntervalRules.isPositiveWithinLimit(parts)) return DraftIssue.IntervalInvalid
    }
    return null
}

fun resolvedStatus(draft: PlanDraft, nowMillis: Long, previous: PlanStatus?): PlanStatus {
    if (draft.restarting) return PlanStatus.IN_PROGRESS
    if (previous != null && isInactive(previous)) return previous
    if (!canChangeStatus(draft.deadlineMillis, nowMillis)) {
        return previous ?: PlanStatus.IN_PROGRESS
    }
    return draft.status
}
