package com.segundoserrano.supertask.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDate < :today ORDER BY dueDate ASC")
    suspend fun getOverdueTasks(today: LocalDate): List<Task>

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC")
    fun getPendingTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedDate DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date ORDER BY createdAt DESC")
    fun getTasksByDate(date: LocalDate): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE groupId = :groupId ORDER BY dueDate ASC")
    fun getTasksByGroup(groupId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE groupId IS NULL ORDER BY dueDate ASC")
    fun getTasksWithoutGroup(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE groupId = :groupId")
    suspend fun deleteTasksByGroup(groupId: Long)

    @Query("UPDATE tasks SET groupId = NULL WHERE groupId = :groupId")
    suspend fun moveTasksToNoGroup(groupId: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE groupId = :groupId AND isCompleted = 0")
    suspend fun countPendingTasksByGroup(groupId: Long): Int

    @Query("SELECT * FROM tasks WHERE dueDate = :date AND isCompleted = 0")
    suspend fun getPendingTasksForDate(date: LocalDate): List<Task>

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    suspend fun getAllTasksAsList(): List<Task>

    @Query("SELECT * FROM tasks WHERE recurrenceType != 'NEVER' AND seriesId IS NOT NULL ORDER BY dueDate ASC")
    suspend fun getAllRecurringTasks(): List<Task>

    /**
     * Busca una instancia existente dentro de la misma serie recurrente,
     * identificada de forma robusta por su seriesId.
     */
    @Query("SELECT * FROM tasks WHERE seriesId = :seriesId AND dueDate = :dueDate LIMIT 1")
    suspend fun findTaskInSeriesById(seriesId: String, dueDate: LocalDate): Task?

    /**
     * Elimina TODAS las tareas que pertenecen a una serie recurrente
     * (pasadas, presentes, futuras), sin importar grupo, título o estado.
     */
    @Query("DELETE FROM tasks WHERE seriesId = :seriesId")
    suspend fun deleteBySeriesId(seriesId: String)

    /**
     * Propaga cambios de identidad de la serie a todas las instancias
     * que comparten el mismo seriesId, excepto la tarea editada (que se
     * actualiza por separado vía updateTask).
     */
    @Query("""
        UPDATE tasks SET
            title = :newTitle,
            recurrenceType = :newRecurrenceType,
            recurrenceInterval = :newRecurrenceInterval,
            recurrenceEndDate = :newRecurrenceEndDate
        WHERE seriesId = :seriesId AND id != :excludeId
    """)
    suspend fun propagateSeriesChangesById(
        seriesId: String,
        excludeId: Long,
        newTitle: String,
        newRecurrenceType: String,
        newRecurrenceInterval: Int,
        newRecurrenceEndDate: LocalDate?
    )

}