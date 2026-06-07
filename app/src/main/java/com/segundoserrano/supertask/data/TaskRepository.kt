package com.segundoserrano.supertask.data

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TaskRepository(private val taskDao: TaskDao, private val groupDao: GroupDao) {

    // Tasks
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
    fun getPendingTasks(): Flow<List<Task>> = taskDao.getPendingTasks()
    fun getCompletedTasks(): Flow<List<Task>> = taskDao.getCompletedTasks()
    fun getTasksByDate(date: LocalDate): Flow<List<Task>> = taskDao.getTasksByDate(date)
    fun getTasksByGroup(groupId: Long): Flow<List<Task>> = taskDao.getTasksByGroup(groupId)
    fun getTasksWithoutGroup(): Flow<List<Task>> = taskDao.getTasksWithoutGroup()

    suspend fun getTaskById(taskId: Long): Task? = taskDao.getTaskById(taskId)

    suspend fun insertTask(task: Task): Long {
        // Si es una tarea recurrente nueva (sin seriesId), generar uno.
        val toInsert = if (task.recurrenceType != RecurrenceType.NEVER && task.seriesId == null) {
            task.copy(seriesId = java.util.UUID.randomUUID().toString())
        } else {
            task
        }
        return taskDao.insertTask(toInsert)
    }

    suspend fun updateTask(task: Task) {
        val old = taskDao.getTaskById(task.id)

        // Caso especial: la tarea pasa de NEVER a recurrente (el usuario activó
        // recurrencia al editar). Le asignamos un seriesId nuevo si no tiene.
        val taskToSave = if (
            task.recurrenceType != RecurrenceType.NEVER &&
            task.seriesId == null
        ) {
            task.copy(seriesId = old?.seriesId ?: java.util.UUID.randomUUID().toString())
        } else {
            task
        }

        // Si la tarea pertenecía a una serie recurrente y el usuario cambió un
        // campo que define la identidad de la serie (título, tipo, intervalo,
        // fecha fin), propagamos los nuevos valores a TODAS las hermanas de
        // la serie por seriesId. Así nunca quedan series fantasma con la config vieja.
        if (old != null && old.seriesId != null && old.recurrenceType != RecurrenceType.NEVER) {
            val seriesIdentityChanged =
                old.title != taskToSave.title ||
                old.recurrenceType != taskToSave.recurrenceType ||
                old.recurrenceInterval != taskToSave.recurrenceInterval ||
                old.recurrenceEndDate != taskToSave.recurrenceEndDate

            if (seriesIdentityChanged) {
                taskDao.propagateSeriesChangesById(
                    seriesId = old.seriesId,
                    excludeId = old.id,
                    newTitle = taskToSave.title,
                    newRecurrenceType = taskToSave.recurrenceType.name,
                    newRecurrenceInterval = taskToSave.recurrenceInterval,
                    newRecurrenceEndDate = taskToSave.recurrenceEndDate
                )
            }
        }

        taskDao.updateTask(taskToSave)
    }

    suspend fun deleteTask(task: Task) {
        if (task.seriesId != null) {
            // Eliminar TODA la serie sin rastro: pasadas, presentes, futuras,
            // sin importar grupo, fecha fin o estado de completado.
            taskDao.deleteBySeriesId(task.seriesId)
        } else {
            taskDao.deleteTask(task)
        }
    }

    suspend fun completeTask(task: Task) {
        // Marcar la tarea como completada (no eliminar, para que generateRecurringTaskInstances
        // no la vuelva a crear como pendiente)
        taskDao.updateTask(task.copy(
            isCompleted = true,
            completedDate = LocalDate.now()
        ))

        // Si es recurrente, crear la siguiente instancia si no existe ya
        if (task.recurrenceType != RecurrenceType.NEVER && task.seriesId != null) {
            val nextTask = createNextRecurringTask(task)

            // Solo crear si no ha pasado la fecha de finalización
            if (task.recurrenceEndDate == null || nextTask.dueDate.isBefore(task.recurrenceEndDate) || nextTask.dueDate.isEqual(task.recurrenceEndDate)) {
                // Dedupe por seriesId
                val existing = taskDao.findTaskInSeriesById(task.seriesId, nextTask.dueDate)
                if (existing == null) {
                    taskDao.insertTask(nextTask)
                }
            }
        }
    }

    private fun createNextRecurringTask(task: Task): Task {
        val nextDate = when (task.recurrenceType) {
            RecurrenceType.DAILY -> task.dueDate.plusDays(1)
            RecurrenceType.WEEKLY -> task.dueDate.plusWeeks(1)
            RecurrenceType.MONTHLY -> task.dueDate.plusMonths(1)
            RecurrenceType.CUSTOM -> task.dueDate.plusDays(task.recurrenceInterval.toLong())
            RecurrenceType.NEVER -> task.dueDate
        }

        return task.copy(
            id = 0, // Nuevo ID
            dueDate = nextDate,
            isCompleted = false,
            completedDate = null,
            createdAt = LocalDate.now()
        )
    }

    suspend fun getPendingTasksForDate(date: LocalDate): List<Task> =
        taskDao.getPendingTasksForDate(date)

    /**
     * Genera instancias futuras de tareas recurrentes hasta hoy.
     *
     * Las instancias se agrupan por su seriesId. Para cada serie:
     *  - Toma la instancia con la dueDate más reciente como base.
     *  - Genera hacia adelante desde su siguiente fecha de recurrencia hasta hoy.
     *  - Las nuevas instancias heredan el groupId, título y configuración de esa base
     *    (y por supuesto, el mismo seriesId).
     */
    suspend fun generateRecurringTaskInstances() {
        val today = LocalDate.now()
        val recurringTasks = taskDao.getAllRecurringTasks()

        // Agrupar por seriesId. Las tareas sin seriesId (no debería suceder
        // después de la migración, pero por seguridad) se ignoran.
        val seriesMap = recurringTasks
            .filter { it.seriesId != null }
            .groupBy { it.seriesId!! }

        seriesMap.forEach { (seriesId, tasksInSeries) ->
            // La instancia más reciente define los datos de las próximas (incluyendo groupId).
            // Desempate por id ASC para preferir la edición más reciente cuando hay misma fecha.
            val baseTask = tasksInSeries.sortedWith(
                compareBy<Task> { it.dueDate }.thenBy { it.id }
            ).lastOrNull() ?: return@forEach

            // Si la recurrencia ya finalizó, saltar
            if (baseTask.recurrenceEndDate != null && today.isAfter(baseTask.recurrenceEndDate)) {
                return@forEach
            }

            // Avanzar a la siguiente fecha de recurrencia (no procesar la fecha de baseTask que ya existe)
            var currentDate = nextRecurrenceDate(baseTask.dueDate, baseTask)

            while (!currentDate.isAfter(today)) {
                if (baseTask.recurrenceEndDate != null && currentDate.isAfter(baseTask.recurrenceEndDate)) break

                // Dedupe por seriesId
                val existing = taskDao.findTaskInSeriesById(seriesId, currentDate)

                if (existing == null) {
                    taskDao.insertTask(baseTask.copy(
                        id = 0,
                        dueDate = currentDate,
                        isCompleted = false,
                        completedDate = null,
                        createdAt = LocalDate.now()
                    ))
                }

                currentDate = nextRecurrenceDate(currentDate, baseTask)
            }
        }
    }

    private fun nextRecurrenceDate(date: LocalDate, task: Task): LocalDate = when (task.recurrenceType) {
        RecurrenceType.DAILY -> date.plusDays(1)
        RecurrenceType.WEEKLY -> date.plusWeeks(1)
        RecurrenceType.MONTHLY -> date.plusMonths(1)
        RecurrenceType.CUSTOM -> date.plusDays(task.recurrenceInterval.toLong())
        RecurrenceType.NEVER -> date.plusYears(100) // No debería llegar aquí
    }

    // Export / Import
    suspend fun exportData(): String {
        val tasks = taskDao.getAllTasksAsList()
        val groups = groupDao.getAllGroupsAsList()
        val backup = BackupData(
            exportDate = LocalDate.now().toString(),
            version = 1,
            groups = groups.map { GroupBackup(it.id, it.name, it.colorHex, it.iconName, it.position) },
            tasks = tasks.map { TaskBackup(
                id = it.id,
                title = it.title,
                dueDate = it.dueDate.toString(),
                groupId = it.groupId,
                isCompleted = it.isCompleted,
                completedDate = it.completedDate?.toString(),
                recurrenceType = it.recurrenceType.name,
                recurrenceInterval = it.recurrenceInterval,
                recurrenceEndDate = it.recurrenceEndDate?.toString(),
                seriesId = it.seriesId,
                createdAt = it.createdAt.toString()
            )}
        )
        return Gson().toJson(backup)
    }

    suspend fun getConflictingTaskIds(backup: BackupData): List<Long> {
        return backup.tasks
            .map { it.id }
            .filter { id -> taskDao.getTaskById(id) != null }
    }

    suspend fun importData(backup: BackupData, overwriteConflicts: Boolean) {
        backup.groups.forEach { g ->
            val exists = groupDao.getGroupById(g.id) != null
            if (!exists || overwriteConflicts) {
                groupDao.insertGroup(Group(g.id, g.name, g.colorHex, g.iconName, g.position))
            }
        }

        // Backups antiguos (sin seriesId): agrupamos por la antigua identidad
        // de serie y asignamos un UUID a cada grupo, así no llegan recurrentes
        // huérfanos a la base.
        data class LegacyKey(
            val title: String,
            val recurrenceType: String,
            val recurrenceInterval: Int,
            val recurrenceEndDate: String?
        )
        val assignedSeriesIds = mutableMapOf<LegacyKey, String>()

        backup.tasks.forEach { t ->
            val exists = taskDao.getTaskById(t.id) != null
            if (!exists || overwriteConflicts) {
                val seriesId = when {
                    t.seriesId != null -> t.seriesId
                    t.recurrenceType == RecurrenceType.NEVER.name -> null
                    else -> {
                        val key = LegacyKey(t.title, t.recurrenceType, t.recurrenceInterval, t.recurrenceEndDate)
                        assignedSeriesIds.getOrPut(key) { java.util.UUID.randomUUID().toString() }
                    }
                }
                val task = Task(
                    id = t.id,
                    title = t.title,
                    dueDate = LocalDate.parse(t.dueDate),
                    groupId = t.groupId,
                    isCompleted = t.isCompleted,
                    completedDate = t.completedDate?.let { LocalDate.parse(it) },
                    recurrenceType = RecurrenceType.valueOf(t.recurrenceType),
                    recurrenceInterval = t.recurrenceInterval,
                    recurrenceEndDate = t.recurrenceEndDate?.let { LocalDate.parse(it) },
                    seriesId = seriesId,
                    createdAt = LocalDate.parse(t.createdAt)
                )
                taskDao.insertTask(task)
            }
        }
    }

    // Groups
    fun getAllGroups(): Flow<List<Group>> = groupDao.getAllGroups()

    suspend fun getGroupById(groupId: Long): Group? = groupDao.getGroupById(groupId)

    suspend fun insertGroup(group: Group): Long = groupDao.insertGroup(group)

    suspend fun updateGroup(group: Group) = groupDao.updateGroup(group)

    suspend fun deleteGroup(group: Group, deleteTasksToo: Boolean) {
        if (deleteTasksToo) {
            taskDao.deleteTasksByGroup(group.id)
        } else {
            taskDao.moveTasksToNoGroup(group.id)
        }
        groupDao.deleteGroup(group)
    }

    suspend fun countPendingTasksByGroup(groupId: Long): Int =
        taskDao.countPendingTasksByGroup(groupId)

    suspend fun getPinnedGroup(): Group? = groupDao.getPinnedGroup()

    suspend fun setPinnedGroup(groupId: Long) {
        groupDao.unpinAllGroups()
        groupDao.pinGroup(groupId)
    }

    suspend fun unpinGroup() = groupDao.unpinAllGroups()
}