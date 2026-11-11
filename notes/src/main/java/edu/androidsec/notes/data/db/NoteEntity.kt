package edu.androidsec.notes.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.androidsec.notes.domain.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val body: String,
    @ColumnInfo(name = "photo_path")
    val photoPath: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "''")
    val tags: String = "",
    @ColumnInfo(name = "is_secret", defaultValue = "0")
    val isSecret: Boolean = false,
    @ColumnInfo(name = "is_pending_delete", defaultValue = "0")
    val isPendingDelete: Boolean = false
)

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    body = body,
    photoPath = photoPath,
    updatedAt = updatedAt,
    tags = tags,
    isSecret = isSecret,
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    body = body,
    photoPath = photoPath,
    updatedAt = updatedAt,
    tags = tags,
    isSecret = isSecret,
)
