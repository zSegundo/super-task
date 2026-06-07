package com.segundoserrano.supertask.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val groupId: Long? = null,
    val createdDate: LocalDate = LocalDate.now(),
    val modifiedDate: LocalDate = LocalDate.now(),
    val modifiedTimestamp: Long = System.currentTimeMillis()
)
