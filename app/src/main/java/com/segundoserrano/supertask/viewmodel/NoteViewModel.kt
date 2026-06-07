package com.segundoserrano.supertask.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.segundoserrano.supertask.data.AppDatabase
import com.segundoserrano.supertask.data.Note
import com.segundoserrano.supertask.data.NoteImage
import com.segundoserrano.supertask.data.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NoteRepository(database.noteDao(), application)
    }

    val allNotes: StateFlow<List<Note>> = repository.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getImagesForNote(noteId: Long): Flow<List<NoteImage>> =
        repository.getImagesForNote(noteId)

    suspend fun getNoteById(id: Long): Note? = withContext(Dispatchers.IO) {
        repository.getNoteById(id)
    }

    /** Inserts or updates a note and returns its ID. */
    suspend fun saveNote(note: Note): Long = withContext(Dispatchers.IO) {
        if (note.id == 0L) {
            repository.insertNote(note)
        } else {
            repository.updateNote(note)
            note.id
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteNote(note) }
    }

    fun createTempCameraFile(): File = repository.createTempCameraFile()

    suspend fun saveImageFromUri(noteId: Long, uri: Uri): NoteImage? =
        withContext(Dispatchers.IO) { repository.saveImageFromUri(noteId, uri) }

    suspend fun saveTempCameraImage(noteId: Long, tempFile: File): NoteImage? =
        withContext(Dispatchers.IO) { repository.saveTempCameraImage(noteId, tempFile) }

    fun deleteImage(image: NoteImage) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteImage(image) }
    }
}
