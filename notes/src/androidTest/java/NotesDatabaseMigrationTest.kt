
package edu.androidsec.notes.data

import android.database.sqlite.SQLiteDatabase
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.androidsec.notes.data.db.NotesDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import junit.framework.TestCase

@RunWith(AndroidJUnit4::class)
class NotesDatabaseMigrationTest {

    private lateinit var context: Context
    private lateinit var database: NotesDatabase

    private val databaseName = "notes.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Удаляем предыдущую тестовую базу
        context.deleteDatabase(databaseName)

        // Создаём базу старой версии 1
        val oldDatabase = context.openOrCreateDatabase(
            databaseName,
            Context.MODE_PRIVATE,
            null
        )

        oldDatabase.execSQL(
            """
            CREATE TABLE notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                photo_path TEXT,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Добавляем заметку до миграции
        oldDatabase.execSQL(
            """
            INSERT INTO notes (id, title, body, photo_path, updated_at)
            VALUES (42, 'Старая заметка', 'Текст заметки', NULL, 1000)
            """.trimIndent()
        )

        // Устанавливаем версию базы 1
        oldDatabase.version = 1
        oldDatabase.close()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }

        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFromVersion1To3_preservesDataAndAddsColumns() {
        // Открываем старую базу через Room.
        // Room должен выполнить миграции 1 -> 2 -> 3.
        database = NotesDatabase.create(context)

        val cursor = database.openHelper.writableDatabase.query(
            """
            SELECT id, title, body, photo_path, updated_at,
                   tags, is_secret, is_pending_delete
            FROM notes
            WHERE id = 42
            """.trimIndent()
        )

        cursor.use {
            TestCase.assertTrue("Заметка должна сохраниться", it.moveToFirst())

            // Проверяем старые поля
            TestCase.assertEquals(42L, it.getLong(it.getColumnIndexOrThrow("id")))
            TestCase.assertEquals("Старая заметка", it.getString(it.getColumnIndexOrThrow("title")))
            TestCase.assertEquals("Текст заметки", it.getString(it.getColumnIndexOrThrow("body")))
            TestCase.assertTrue(it.isNull(it.getColumnIndexOrThrow("photo_path")))
            TestCase.assertEquals(1000L, it.getLong(it.getColumnIndexOrThrow("updated_at")))

            // Проверяем поля, добавленные миграцией 1 -> 2
            TestCase.assertEquals("", it.getString(it.getColumnIndexOrThrow("tags")))
            TestCase.assertEquals(0, it.getInt(it.getColumnIndexOrThrow("is_secret")))

            // Проверяем поле, добавленное миграцией 2 -> 3
            TestCase.assertEquals(
                0,
                it.getInt(it.getColumnIndexOrThrow("is_pending_delete"))
            )
        }
    }
}

