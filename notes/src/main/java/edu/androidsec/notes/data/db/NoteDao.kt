package edu.androidsec.notes.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.androidsec.notes.domain.NoteTitle
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query(
        """
        SELECT * FROM notes
        WHERE is_pending_delete = 0
        ORDER BY updated_at DESC
        """
    )
    fun observeAll(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM notes
        WHERE is_pending_delete = 0
          AND (
              :query = ''
              OR title LIKE '%' || :query || '%' ESCAPE '\'
              OR body LIKE '%' || :query || '%' ESCAPE '\'
              OR tags LIKE '%' || :query || '%' ESCAPE '\'
          )
        ORDER BY updated_at DESC
        """
    )
    fun search(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    // Пометить заметку как ожидающую удаления
    @Query("UPDATE notes SET is_pending_delete = 1 WHERE id = :id")
    suspend fun markPendingDelete(id: Long)

    // Восстановить заметку после Undo
    @Query("UPDATE notes SET is_pending_delete = 0 WHERE id = :id")
    suspend fun restore(id: Long)

    // Окончательно удалить одну заметку
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Окончательно удалить все заметки, ожидающие удаления
    @Query("DELETE FROM notes WHERE is_pending_delete = 1")
    suspend fun deleteAllPending()

    @Query("SELECT id, title FROM notes ORDER BY id")
    fun titlesBlocking(): List<NoteTitle>
}
