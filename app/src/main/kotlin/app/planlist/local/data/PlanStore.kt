package app.planlist.local.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import app.planlist.core.DraftIssue
import app.planlist.core.canChangeStatus
import app.planlist.core.IntervalRules
import app.planlist.core.Plan
import app.planlist.core.PlanDraft
import app.planlist.core.PlanStatus
import app.planlist.core.PriorityScale
import app.planlist.core.ReminderKind
import app.planlist.core.RepeatKind
import app.planlist.core.resolvedStatus
import app.planlist.core.sortPlans
import app.planlist.core.validate
import app.planlist.local.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val deadlineMillis: Long,
    val status: String,
    val reminder: String,
    val priority: Int,
    val repeatKind: String,
    val intervalSeconds: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

fun PlanEntity.toPlan(): Plan = Plan(
    id = id,
    title = title,
    deadlineMillis = deadlineMillis,
    status = PlanStatus.valueOf(status),
    reminder = ReminderKind.valueOf(reminder),
    priority = priority,
    repeat = RepeatKind.valueOf(repeatKind),
    intervalSeconds = intervalSeconds,
    createdAtMillis = createdAtMillis,
)

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans")
    fun observeAll(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans")
    suspend fun all(): List<PlanEntity>

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun byId(id: Long): PlanEntity?

    @Insert
    suspend fun insert(entity: PlanEntity): Long

    @Update
    suspend fun update(entity: PlanEntity): Int

    @Delete
    suspend fun delete(entity: PlanEntity)
}

@Database(entities = [PlanEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "plans.db").build()
    }
}

sealed interface SaveResult {
    data class Saved(val id: Long) : SaveResult
    data class Rejected(val issue: DraftIssue) : SaveResult
    data object Missing : SaveResult
}

class PlanRepository(
    private val dao: PlanDao,
    private val scheduler: ReminderScheduler,
) {
    fun observe(): Flow<List<Plan>> =
        dao.observeAll().map { rows -> sortPlans(rows.map { it.toPlan() }) }

    suspend fun get(id: Long): Plan? = dao.byId(id)?.toPlan()

    suspend fun save(draft: PlanDraft, now: Long): SaveResult {
        validate(draft, now)?.let { return SaveResult.Rejected(it) }
        val existing = draft.id?.let { dao.byId(it) }
        if (draft.id != null && existing == null) return SaveResult.Missing
        val interval = if (draft.repeat == RepeatKind.EVERY) {
            IntervalRules.sanitize(
                draft.interval.days,
                draft.interval.hours,
                draft.interval.minutes,
                draft.interval.seconds,
            ).totalSeconds
        } else {
            0L
        }
        val previous = existing?.status?.let { runCatching { PlanStatus.valueOf(it) }.getOrNull() }
        val entity = PlanEntity(
            id = existing?.id ?: 0,
            title = draft.title.trim(),
            deadlineMillis = draft.deadlineMillis,
            status = resolvedStatus(draft, now, previous).name,
            reminder = draft.reminder.name,
            priority = PriorityScale.coerce(draft.priority),
            repeatKind = draft.repeat.name,
            intervalSeconds = interval,
            createdAtMillis = existing?.createdAtMillis ?: now,
            updatedAtMillis = now,
        )
        val id = if (existing == null) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            entity.id
        }
        val saved = dao.byId(id)?.toPlan() ?: return SaveResult.Missing
        scheduler.reschedule(saved, now)
        return SaveResult.Saved(id)
    }

    suspend fun setStatus(id: Long, status: PlanStatus, now: Long): Boolean {
        val entity = dao.byId(id) ?: return false
        if (!canChangeStatus(entity.deadlineMillis, now)) return false
        val updated = entity.copy(status = status.name, updatedAtMillis = now)
        dao.update(updated)
        scheduler.reschedule(updated.toPlan(), now)
        return true
    }

    suspend fun delete(id: Long) {
        val entity = dao.byId(id) ?: return
        scheduler.cancel(id)
        dao.delete(entity)
    }

    suspend fun rescheduleAll(now: Long) {
        scheduler.rescheduleAll(dao.all().map { it.toPlan() }, now)
    }
}
