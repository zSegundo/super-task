package com.segundoserrano.supertask.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val colorHex: String, // Color en formato hex (#FF00AA66)

    val iconName: String = "folder", // Nombre del ícono (para futuras expansiones)

    val position: Int = 0, // Para ordenar grupos

    val isPinned: Boolean = false // Grupo fijado como vista por defecto
)

// Grupos predefinidos con sus colores
object DefaultGroups {
    fun getDefaultGroups(): List<Group> {
        return listOf(
            Group(
                id = 1,
                name = "Personal",
                colorHex = "#4A90E2",
                position = 0
            ),
            Group(
                id = 2,
                name = "Work",
                colorHex = "#FF8C42",
                position = 1
            ),
            Group(
                id = 3,
                name = "Shopping",
                colorHex = "#9B59B6",
                position = 2
            ),
            Group(
                id = 4,
                name = "Fitness",
                colorHex = "#E91E63",
                position = 3
            )
        )
    }
}