package app.planlist.core

import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlanRulesTest {
    private val shanghai = ZoneId.of("Asia/Shanghai")

    @Test
    fun sortsHigherPriorityFirstThenSoonerDeadline() {
        val plans = listOf(
            plan(id = 1, priority = 3, deadline = 300),
            plan(id = 2, priority = 9, deadline = 800),
            plan(id = 3, priority = 9, deadline = 200),
            plan(id = 4, priority = 3, deadline = 100),
        )
        assertEquals(listOf(3L, 2L, 4L, 1L), sortPlans(plans).map { it.id })
    }

    @Test
    fun dayPrecisionDropsMinutesAndSeconds() {
        val parts = IntervalRules.sanitize(days = 2, hours = 3, minutes = 40, seconds = 15)
        assertEquals(IntervalParts(2, 3, 0, 0), parts)
        assertEquals(IntervalUnit.DAY, parts.largestUnit)
        assertEquals(2 * 86_400L + 3 * 3_600L, parts.totalSeconds)
    }

    @Test
    fun hourPrecisionDropsSeconds() {
        val parts = IntervalRules.sanitize(days = 0, hours = 5, minutes = 12, seconds = 40)
        assertEquals(IntervalParts(0, 5, 12, 0), parts)
        assertEquals(IntervalUnit.HOUR, parts.largestUnit)
    }

    @Test
    fun underOneHourKeepsSeconds() {
        val parts = IntervalRules.sanitize(days = 0, hours = 0, minutes = 4, seconds = 8)
        assertEquals(IntervalParts(0, 0, 4, 8), parts)
        assertEquals(IntervalUnit.MINUTE, parts.largestUnit)
        assertTrue(IntervalRules.isPositiveWithinLimit(parts))
    }

    @Test
    fun thirtyDaysRejectsExtraHours() {
        val parts = IntervalRules.sanitize(days = 30, hours = 2, minutes = 10, seconds = 10)
        assertEquals(IntervalParts(30, 0, 0, 0), parts)
        assertEquals(IntervalRules.MAX_SECONDS, parts.totalSeconds)
        assertTrue(IntervalRules.isPositiveWithinLimit(parts))
    }

    @Test
    fun twentyNineDaysKeepsHours() {
        val parts = IntervalRules.sanitize(days = 29, hours = 23, minutes = 59, seconds = 59)
        assertEquals(IntervalParts(29, 23, 0, 0), parts)
        assertTrue(parts.totalSeconds < IntervalRules.MAX_SECONDS)
    }

    @Test
    fun zeroIntervalIsInvalid() {
        val parts = IntervalRules.sanitize(0, 0, 0, 0)
        assertFalse(IntervalRules.isPositiveWithinLimit(parts))
    }

    @Test
    fun partsRoundTripThroughSeconds() {
        val original = IntervalRules.sanitize(1, 4, 20, 9)
        assertEquals(original, IntervalRules.partsFromSeconds(original.totalSeconds))
        val short = IntervalRules.sanitize(0, 0, 0, 12)
        assertEquals(short, IntervalRules.partsFromSeconds(short.totalSeconds))
    }

    @Test
    fun statusChangesOnlyBeforeDeadline() {
        assertTrue(canChangeStatus(deadlineMillis = 1_000, nowMillis = 999))
        assertFalse(canChangeStatus(deadlineMillis = 1_000, nowMillis = 1_000))
        assertFalse(canChangeStatus(deadlineMillis = 1_000, nowMillis = 1_001))
    }

    @Test
    fun onceReminderFiresAtDeadline() {
        val item = plan(deadline = 5_000, created = 1_000, repeat = RepeatKind.ONCE)
        assertEquals(5_000, nextReminderAt(item, nowMillis = 1_000))
        assertNull(nextReminderAt(item, nowMillis = 5_000))
    }

    @Test
    fun intervalStepsUntilDeadline() {
        val item = plan(
            deadline = 10_000,
            created = 0,
            repeat = RepeatKind.EVERY,
            intervalSeconds = 3,
        )
        assertEquals(3_000, nextReminderAt(item, nowMillis = 0))
        assertEquals(6_000, nextReminderAt(item, nowMillis = 3_000))
        assertEquals(9_000, nextReminderAt(item, nowMillis = 7_000))
        assertEquals(10_000, nextReminderAt(item, nowMillis = 9_000))
    }

    @Test
    fun longIntervalStillRemindsAtDeadline() {
        val item = plan(
            deadline = 2_000,
            created = 0,
            repeat = RepeatKind.EVERY,
            intervalSeconds = 86_400,
        )
        assertEquals(2_000, nextReminderAt(item, nowMillis = 100))
    }

    @Test
    fun finishedPlansAreNotReminded() {
        val done = plan(status = PlanStatus.DONE, deadline = 9_000)
        val abandoned = plan(status = PlanStatus.GIVE_UP, deadline = 9_000)
        assertNull(nextReminderAt(done, nowMillis = 0))
        assertNull(nextReminderAt(abandoned, nowMillis = 0))
    }

    @Test
    fun deadlineUsesTheGivenZone() {
        val millis = toEpochMillis(DeadlineParts(2026, 11, 11, 20, 0), shanghai)
        assertEquals(Instant.parse("2026-11-11T12:00:00Z").toEpochMilli(), millis)
        assertEquals(
            DeadlineParts(2026, 11, 11, 20, 0),
            fromEpochMillis(millis, shanghai),
        )
        assertEquals(
            DeadlineParts(2026, 11, 11, 12, 0),
            fromEpochMillis(millis, ZoneId.of("UTC")),
        )
    }

    @Test
    fun februaryDayIsClamped() {
        assertEquals(28, daysInMonth(2026, 2))
        assertEquals(29, daysInMonth(2028, 2))
        val clamped = clampDeadline(DeadlineParts(2026, 2, 31, 26, 70))
        assertEquals(DeadlineParts(2026, 2, 28, 23, 59), clamped)
    }

    @Test
    fun newPlanRejectsAPastDeadlineAndABlankTitle() {
        val past = draft(title = "buy milk", deadline = 50, original = null)
        assertEquals(DraftIssue.DeadlineNotInFuture, validate(past, nowMillis = 100))
        val blank = draft(title = "  ", deadline = 200, original = null)
        assertEquals(DraftIssue.EmptyTitle, validate(blank, nowMillis = 100))
        val ok = draft(title = "buy milk", deadline = 200, original = null)
        assertNull(validate(ok, nowMillis = 100))
    }

    @Test
    fun existingPlanMayKeepItsOriginalDeadline() {
        val kept = draft(title = "keep", deadline = 50, original = 50)
        assertNull(validate(kept, nowMillis = 100))
        val movedEarlier = draft(title = "move", deadline = 40, original = 50)
        assertEquals(DraftIssue.DeadlineNotInFuture, validate(movedEarlier, nowMillis = 100))
    }

    @Test
    fun statusStaysPutOnceTheDeadlineHasPassed() {
        val past = draft(title = "x", deadline = 50, original = 50, status = PlanStatus.DONE)
        assertEquals(
            PlanStatus.IN_PROGRESS,
            resolvedStatus(past, nowMillis = 100, previous = PlanStatus.IN_PROGRESS),
        )
        val future = draft(title = "x", deadline = 200, original = null, status = PlanStatus.GIVE_UP)
        assertEquals(
            PlanStatus.GIVE_UP,
            resolvedStatus(future, nowMillis = 100, previous = PlanStatus.IN_PROGRESS),
        )
    }

    @Test
    fun emptyRepeatIntervalIsRejected() {
        val bad = draft(
            title = "x",
            deadline = 500,
            original = null,
            repeat = RepeatKind.EVERY,
            interval = IntervalParts(0, 0, 0, 0),
        )
        assertEquals(DraftIssue.IntervalInvalid, validate(bad, nowMillis = 0))
    }

    @Test
    fun zoneIdsAreGroupedFromTheSystemList() {
        val ids = listOf("Asia/Shanghai", "Asia/Tokyo", "UTC", "Europe/Paris")
        assertEquals(listOf("Asia", "Europe", SystemZones.OTHER), SystemZones.regions(ids))
        assertEquals(listOf("Asia/Shanghai", "Asia/Tokyo"), SystemZones.zones(ids, "Asia"))
        assertEquals(listOf("UTC"), SystemZones.zones(ids, SystemZones.OTHER))
    }

    private fun plan(
        id: Long = 1,
        priority: Int = 5,
        deadline: Long = 10_000,
        created: Long = 0,
        status: PlanStatus = PlanStatus.IN_PROGRESS,
        repeat: RepeatKind = RepeatKind.ONCE,
        intervalSeconds: Long = 0,
    ) = Plan(
        id = id,
        title = "plan",
        deadlineMillis = deadline,
        status = status,
        reminder = ReminderKind.POPUP,
        priority = priority,
        repeat = repeat,
        intervalSeconds = intervalSeconds,
        createdAtMillis = created,
    )

    private fun draft(
        title: String,
        deadline: Long,
        original: Long?,
        status: PlanStatus = PlanStatus.IN_PROGRESS,
        repeat: RepeatKind = RepeatKind.ONCE,
        interval: IntervalParts = IntervalParts(0, 0, 0, 0),
    ) = PlanDraft(
        id = null,
        title = title,
        deadlineMillis = deadline,
        status = status,
        reminder = ReminderKind.POPUP,
        priority = 5,
        repeat = repeat,
        interval = interval,
        originalDeadlineMillis = original,
    )
}
