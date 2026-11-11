import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.androidsec.notes.data.RoomNotesRepository
import edu.androidsec.notes.data.db.NoteDao
import edu.androidsec.notes.data.db.NoteEntity
import edu.androidsec.notes.data.db.NotesDatabase
import junit.framework.TestCase
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var database: NotesDatabase
    private lateinit var dao: NoteDao
    private lateinit var repository: RoomNotesRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        database = Room.inMemoryDatabaseBuilder(
            context,
            NotesDatabase::class.java
        ).build()

        dao = database.noteDao()
        repository = RoomNotesRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertNote(
        title: String,
        body: String = "",
        tags: String = "",
        updatedAt: Long = System.currentTimeMillis()
    ) {
        dao.upsert(
            NoteEntity(
                title = title,
                body = body,
                photoPath = null,
                updatedAt = updatedAt,
                tags = tags
            )
        )
    }

    @Test
    fun searchByTitle_returnsMatchingNote() = runBlocking {
        insertNote(title = "Уникальная заметка для поиска")
        insertNote(title = "Другая заметка")

        val result = dao.search("Уникальная").first()

        TestCase.assertEquals(1, result.size)
        TestCase.assertEquals("Уникальная заметка для поиска", result.first().title)
    }

    @Test
    fun searchByBody_returnsMatchingNote() = runBlocking {
        insertNote(
            title = "Заметка",
            body = "Подготовить проект к защите"
        )

        val result = dao.search("проект").first()

        TestCase.assertTrue(result.any { it.title == "Заметка" })
    }

    @Test
    fun searchByTags_returnsMatchingNote() = runBlocking {
        insertNote(
            title = "Заметка с тегом",
            tags = "android security"
        )

        val result = dao.search("security").first()

        TestCase.assertTrue(result.any { it.title == "Заметка с тегом" })
    }

    @Test
    fun searchWithoutMatches_returnsEmptyList() = runBlocking {
        insertNote(title = "Обычная заметка")

        val result = dao.search("несуществующий_запрос").first()

        TestCase.assertTrue(result.isEmpty())
    }

    @Test
    fun emptyQuery_returnsAllNotes() = runBlocking {
        insertNote(title = "Первая заметка")
        insertNote(title = "Вторая заметка")

        val result = dao.search("").first()

        TestCase.assertEquals(2, result.size)
    }

    @Test
    fun searchPercentSymbol_findsLiteralPercent() = runBlocking {
        insertNote(title = "Скидка 100%")
        insertNote(title = "Скидка 100x")

        val result = repository.search("%").first()

        assertTrue(result.any { it.title == "Скидка 100%" })
        assertFalse(result.any { it.title == "Скидка 100x" })
    }

    @Test
    fun searchUnderscoreSymbol_findsLiteralUnderscore() = runBlocking {
        insertNote(title = "user_name")
        insertNote(title = "username")

        val result = repository.search("_").first()

        assertTrue(result.any { it.title == "user_name" })
        assertFalse(result.any { it.title == "username" })
    }

    @Test
    fun searchBackslash_findsLiteralBackslash() = runBlocking {
        insertNote(title = "path\\to\\file")
        insertNote(title = "pathtofile")

        val repository = RoomNotesRepository(dao)

        val result = repository.search("\\").first()

        assertTrue(result.any { it.title == "path\\to\\file" })
        assertFalse(result.any { it.title == "pathtofile" })
    }

    @Test
    fun searchResults_areSortedByUpdatedAtDescending() = runBlocking {
        insertNote(
            title = "Старая заметка",
            updatedAt = 100L
        )
        insertNote(
            title = "Новая заметка",
            updatedAt = 300L
        )
        insertNote(
            title = "Средняя заметка",
            updatedAt = 200L
        )

        val result = dao.search("заметка").first()

        val testNotes = result.filter {
            it.title in setOf(
                "Старая заметка",
                "Новая заметка",
                "Средняя заметка"
            )
        }

        TestCase.assertEquals(
            listOf(
                "Новая заметка",
                "Средняя заметка",
                "Старая заметка"
            ),
            testNotes.map { it.title }
        )
    }
}
