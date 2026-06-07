package com.segundoserrano.supertask.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val dueDate: LocalDate,

    val groupId: Long? = null, // null = sin grupo

    val isCompleted: Boolean = false,

    val completedDate: LocalDate? = null,

    // Recurrencia
    val recurrenceType: RecurrenceType = RecurrenceType.NEVER,

    val recurrenceInterval: Int = 1, // Para recurrencia personalizada (cada X días)

    val recurrenceEndDate: LocalDate? = null, // Fecha de finalización de recurrencia

    /**
     * Identificador único de la serie recurrente. Todas las instancias de una
     * misma tarea recurrente comparten el mismo seriesId. Permite identificar
     * la serie sin depender del título (que puede cambiar) ni del grupo
     * (que puede moverse).
     *
     * null para tareas no recurrentes (recurrenceType = NEVER).
     */
    val seriesId: String? = null,

    val createdAt: LocalDate = LocalDate.now()
)

enum class RecurrenceType {
    NEVER,
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM // Cada X días
}

// Extensiones útiles
fun Task.isOverdue(): Boolean {
    return !isCompleted && dueDate.isBefore(LocalDate.now())
}

fun Task.isToday(): Boolean {
    return dueDate.isEqual(LocalDate.now())
}

fun Task.isTomorrow(): Boolean {
    return dueDate.isEqual(LocalDate.now().plusDays(1))
}