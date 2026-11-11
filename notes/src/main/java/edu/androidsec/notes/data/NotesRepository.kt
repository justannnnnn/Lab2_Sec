package edu.androidsec.notes.data

import edu.androidsec.notes.data.db.NoteDao
import edu.androidsec.notes.data.db.toDomain
import edu.androidsec.notes.data.db.toEntity
import edu.androidsec.notes.domain.Note
import edu.androidsec.notes.domain.NoteValidationRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Порт доступа к заметкам. ViewModel-и зависят только от него. */
interface NotesRepository {
    fun observeAll(): Flow<List<Note>>
    fun search(query: String): Flow<List<Note>>
    suspend fun get(id: Long): Note?
    /** Сохраняет заметку и возвращает её id (для новой заметки — только что выданный). */
    suspend fun save(note: Note): Long

    suspend fun markPendingDelete(id: Long)
    suspend fun restore(id: Long)
    suspend fun finalizeDelete(id: Long)
    suspend fun finalizeAllPendingDeletes()
}

class RoomNotesRepository(
    private val dao: NoteDao
) : NotesRepository {

    override fun observeAll(): Flow<List<Note>> =
        dao.observeAll().map { list ->
            list.map { it.toDomain() }
        }

    override fun search(query: String): Flow<List<Note>> {
        val escapedQuery = query.trim()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

        return dao.search(escapedQuery).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun get(id: Long): Note? =
        dao.getById(id)?.toDomain()

    override suspend fun save(note: Note): Long {
        require(NoteValidationRules.validateNote(note)) {
            "Note contains invalid data"
        }

        val stamped = note.copy(
            updatedAt = System.currentTimeMillis()
        )

        val rowId = dao.upsert(stamped.toEntity())

        return if (note.isNew) rowId else note.id
    }

    override suspend fun markPendingDelete(id: Long) {
        dao.markPendingDelete(id)
    }

    override suspend fun restore(id: Long) {
        dao.restore(id)
    }

    override suspend fun finalizeDelete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun finalizeAllPendingDeletes() {
        dao.deleteAllPending()
    }
}
