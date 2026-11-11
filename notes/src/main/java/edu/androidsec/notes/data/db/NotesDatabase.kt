package edu.androidsec.notes.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class],
    version = 3,
    exportSchema = false
)
abstract class NotesDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        private const val NAME = "notes.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN is_secret INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN is_pending_delete INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun create(context: Context): NotesDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NotesDatabase::class.java,
                NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(SeedCallback)
                .build()
    }

    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            db.execSQL(
                "INSERT INTO notes (id, title, body, photo_path, updated_at) VALUES " +
                    "(1, 'Подготовиться к защите ЛР2', " +
                    "'Проверить каждую точку входа через adb: am start, am broadcast, content query.', NULL, ${now - 2000}), " +
                    "(2, 'Прочитать лекцию 6', " +
                    "'Явные и неявные интенты, android:exported, PendingIntent и FLAG_IMMUTABLE.', NULL, ${now - 1000}), " +
                    "(3, 'Список покупок', " +
                    "'Молоко, хлеб, кофе.', NULL, $now)"
            )
        }
    }
}
