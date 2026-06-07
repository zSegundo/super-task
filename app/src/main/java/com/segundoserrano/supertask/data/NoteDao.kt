package com.segundoserrano.supertask.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY modifiedTimestamp DESC, id DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM note_images WHERE noteId = :noteId ORDER BY id ASC")
    fun getImagesForNote(noteId: Long): Flow<List<NoteImage>>

    @Query("SELECT * FROM note_images WHERE noteId = :noteId ORDER BY id ASC")
    suspend fun getImagesForNoteSync(noteId: Long): List<NoteImage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteImage(image: NoteImage): Long

    @Delete
    suspend fun deleteNoteImage(image: NoteImage)
}
