package com.segundoserrano.supertask.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Task::class, Group::class, Note::class, NoteImage::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun groupDao(): GroupDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `groups` ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `groupId` INTEGER,
                        `createdDate` TEXT NOT NULL,
                        `modifiedDate` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `note_images` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `noteId` INTEGER NOT NULL,
                        `imagePath` TEXT NOT NULL,
                        FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_images_noteId` ON `note_images` (`noteId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `modifiedTimestamp` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Agrega la columna seriesId a tasks y asigna un UUID por cada serie
         * recurrente preexistente. Identidad inicial de serie (heredada del
         * esquema anterior): (title, recurrenceType, recurrenceInterval,
         * recurrenceEndDate). Las tareas NEVER quedan con seriesId = NULL.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `seriesId` TEXT")

                val cursor = db.query("""
                    SELECT DISTINCT title, recurrenceType, recurrenceInterval, recurrenceEndDate
                    FROM tasks
                    WHERE recurrenceType != 'NEVER'
                """.trimIndent())

                try {
                    while (cursor.moveToNext()) {
                        val title = cursor.getString(0)
                        val recurrenceType = cursor.getString(1)
                        val recurrenceInterval = cursor.getInt(2)
                        val recurrenceEndDate = if (cursor.isNull(3)) null else cursor.getString(3)
                        val seriesId = java.util.UUID.randomUUID().toString()

                        if (recurrenceEndDate == null) {
                            db.execSQL(
                                """UPDATE tasks SET seriesId = ?
                                   WHERE title = ? AND recurrenceType = ?
                                   AND recurrenceInterval = ? AND recurrenceEndDate IS NULL""",
                                arrayOf<Any>(seriesId, title, recurrenceType, recurrenceInterval)
                            )
                        } else {
                            db.execSQL(
                                """UPDATE tasks SET seriesId = ?
                                   WHERE title = ? AND recurrenceType = ?
                                   AND recurrenceInterval = ? AND recurrenceEndDate = ?""",
                                arrayOf<Any>(seriesId, title, recurrenceType, recurrenceInterval, recurrenceEndDate)
                            )
                        }
                    }
                } finally {
                    cursor.close()
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "supertask_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    // Insertar grupos predefinidos en la primera ejecución
                    database.groupDao().insertGroups(DefaultGroups.getDefaultGroups())
                }
            }
        }
    }
}