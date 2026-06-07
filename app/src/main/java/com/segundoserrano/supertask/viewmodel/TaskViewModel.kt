package com.segundoserrano.supertask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.segundoserrano.supertask.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    private var lastAppliedPinnedGroupId: Long? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao(), database.groupDao())
    }

    // Tasks
    val allTasks: StateFlow<List<Task>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTasks: StateFlow<List<Task>> = repository.getPendingTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTasks: StateFlow<List<Task>> = repository.getCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Groups
    val allGroups: StateFlow<List<Group>> = repository.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtros
    private val _selectedFilter = MutableStateFlow<TaskFilter>(TaskFilter.All)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    val selectedGroupId: StateFlow<Long?> = _selectedGroupId.asStateFlow()

    init {
        // Auto-apply pinned group only when the pinned group actually changes
        viewModelScope.launch {
            allGroups.collect { groups ->
                val pinnedId = groups.find { it.isPinned }?.id
                if (pinnedId != lastAppliedPinnedGroupId) {
                    lastAppliedPinnedGroupId = pinnedId
                    if (pinnedId != null) {
                        _selectedGroupId.value = pinnedId
                        _selectedFilter.value = TaskFilter.ByGroup
                    }
                }
            }
        }
    }

    // Tareas filtradas
    val filteredTasks: StateFlow<List<Task>> = combine(
        allTasks,
        selectedFilter,
        selectedGroupId
    ) { tasks, filter, groupId ->
        when (filter) {
            TaskFilter.All -> tasks.filter { !it.isCompleted }
            TaskFilter.Pending -> tasks.filter { !it.isCompleted }
            TaskFilter.ByGroup -> {
                if (groupId != null) {
                    tasks.filter { it.groupId == groupId && !it.isCompleted }
                } else {
                    tasks.filter { !it.isCompleted }
                }
            }
            TaskFilter.Completed -> tasks.filter { it.isCompleted }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tareas organizadas por sección
    val organizedTasks: StateFlow<Map<TaskSection, List<Task>>> = filteredTasks
        .map { tasks ->
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            tasks.groupBy { task ->
                when {
                    task.isOverdue() -> TaskSection.Overdue
                    task.isToday() -> TaskSection.Today
                    task.isTomorrow() -> TaskSection.Tomorrow
                    else -> TaskSection.Upcoming
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setFilter(filter: TaskFilter) {
        _selectedFilter.value = filter
    }

    fun setSelectedGroup(groupId: Long?) {
        _selectedGroupId.value = groupId
        if (groupId != null) {
            _selectedFilter.value = TaskFilter.ByGroup
        }
    }

    fun getTasksByDate(date: LocalDate): Flow<List<Task>> {
        return repository.getTasksByDate(date)
    }

    suspend fun getTaskById(taskId: Long): Task? {
        return repository.getTaskById(taskId)
    }

    fun insertTask(task: Task) {
        viewModelScope.launch {
            repository.insertTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            repository.completeTask(task)
        }
    }

    // Groups
    fun insertGroup(group: Group) {
        viewModelScope.launch {
            repository.insertGroup(group)
        }
    }

    fun updateGroup(group: Group) {
        viewModelScope.launch {
            repository.updateGroup(group)
        }
    }

    fun pinGroup(groupId: Long) {
        viewModelScope.launch { repository.setPinnedGroup(groupId) }
    }

    fun unpinGroup() {
        viewModelScope.launch { repository.unpinGroup() }
    }

    fun deleteGroup(group: Group, deleteTasksToo: Boolean) {
        viewModelScope.launch {
            repository.deleteGroup(group, deleteTasksToo)
        }
    }

    suspend fun countPendingTasksByGroup(groupId: Long): Int {
        return repository.countPendingTasksByGroup(groupId)
    }

    // Export / Import
    suspend fun exportData(): String = withContext(Dispatchers.IO) {
        repository.exportData()
    }

    suspend fun getConflictingTaskIds(backup: BackupData): List<Long> = withContext(Dispatchers.IO) {
        repository.getConflictingTaskIds(backup)
    }

    fun importData(backup: BackupData, overwriteConflicts: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.importData(backup, overwriteConflicts)
        }
    }
}

enum class TaskFilter {
    All,
    Pending,
    ByGroup,
    Completed
}

enum class TaskSection {
    Overdue,
    Today,
    Tomorrow,
    Upcoming
}