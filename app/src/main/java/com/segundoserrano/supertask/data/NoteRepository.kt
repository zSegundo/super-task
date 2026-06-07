package com.segundoserrano.supertask.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.LocalDate

class NoteRepository(
    private val noteDao: NoteDao,
    private val context: Context
) {

    private val noteImagesDir: File
        get() = File(context.filesDir, "note_images").also { it.mkdirs() }

    private val tempImagesDir: File
        get() = File(context.cacheDir, "note_images/temp").also { it.mkdirs() }

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(
        note.copy(
            modifiedDate = LocalDate.now(),
            modifiedTimestamp = System.currentTimeMillis()
        )
    )

    suspend fun deleteNote(note: Note) {
        val images = noteDao.getImagesForNoteSync(note.id)
        images.forEach { File(it.imagePath).delete() }
        noteDao.deleteNote(note)
    }

    fun getImagesForNote(noteId: Long): Flow<List<NoteImage>> = noteDao.getImagesForNote(noteId)

    /** Copies an image from a content URI into internal storage and persists the record. */
    suspend fun saveImageFromUri(noteId: Long, uri: Uri): NoteImage? {
        return try {
            val dir = File(noteImagesDir, noteId.toString()).also { it.mkdirs() }
            val destFile = File(dir, "${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            val image = NoteImage(noteId = noteId, imagePath = destFile.absolutePath)
            val id = noteDao.insertNoteImage(image)
            image.copy(id = id)
        } catch (e: Exception) {
            null
        }
    }

    /** Creates a temp file for the camera to write into (before the note has been saved). */
    fun createTempCameraFile(): File {
        return File(tempImagesDir, "${System.currentTimeMillis()}.jpg")
    }

    /**
     * Moves a camera-captured file (in temp) to the note's permanent directory
     * and persists the image record.
     */
    suspend fun saveTempCameraImage(noteId: Long, tempFile: File): NoteImage? {
        return try {
            val dir = File(noteImagesDir, noteId.toString()).also { it.mkdirs() }
            val destFile = File(dir, tempFile.name)
            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()
            val image = NoteImage(noteId = noteId, imagePath = destFile.absolutePath)
            val id = noteDao.insertNoteImage(image)
            image.copy(id = id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteImage(image: NoteImage) {
        File(image.imagePath).delete()
        noteDao.deleteNoteImage(image)
    }
}
