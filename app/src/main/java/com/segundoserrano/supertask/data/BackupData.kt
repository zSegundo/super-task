package com.segundoserrano.supertask.data

data class TaskBackup(
    val id: Long,
    val title: String,
    val dueDate: String,
    val groupId: Long?,
    val isCompleted: Boolean,
    val completedDate: String?,
    val recurrenceType: String,
    val recurrenceInterval: Int,
    val recurrenceEndDate: String?,
    /** Null en backups antiguos. Si null, el import asigna seriesIds al vuelo
     *  agrupando por (title, recurrenceType, recurrenceInterval, recurrenceEndDate). */
    val seriesId: String? = null,
    val createdAt: String
)

data class GroupBackup(
    val id: Long,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val position: Int
)

data class BackupData(
    val exportDate: String,
    val version: Int = 1,
    val groups: List<GroupBackup>,
    val tasks: List<TaskBackup>
)
