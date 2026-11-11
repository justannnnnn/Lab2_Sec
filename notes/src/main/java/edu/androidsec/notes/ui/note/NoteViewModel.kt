package edu.androidsec.notes.ui.note

import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.androidsec.notes.R
import edu.androidsec.notes.data.NotesRepository
import edu.androidsec.notes.data.db.DeleteCoordinator
import edu.androidsec.notes.domain.Note
import edu.androidsec.notes.photo.PhotoStore
import edu.androidsec.notes.reminder.ReminderScheduler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Всё, что экран заметки рисует. `noteId == 0` — новая, ещё не сохранённая заметка. */
data class NoteUiState(
    val noteId: Long,
    val isLoading: Boolean = true,
    val title: String = "",
    val body: String = "",
    val tags: String = "",
    val photoPath: String? = null,
    val notFound: Boolean = false,
    val isSecret: Boolean = false
) {
    val isNew: Boolean get() = noteId == 0L

    fun toNote(): Note = Note(
        id = noteId,
        title = title,
        body = body,
        photoPath = photoPath,
        tags = tags,
        isSecret = isSecret
    )
}

/** Одноразовые события для экрана: показать сообщение или закрыть экран. */
sealed interface NoteEvent {
    data class Message(@param:StringRes val resId: Int) : NoteEvent
    data object Close : NoteEvent
}

class NoteViewModel(
    noteId: Long,
    private val repository: NotesRepository,
    private val photoStore: PhotoStore,
    private val reminderScheduler: ReminderScheduler,
    private val deleteCoordinator: DeleteCoordinator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteUiState(noteId = noteId))
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    private val _events = Channel<NoteEvent>(Channel.BUFFERED)
    val events: Flow<NoteEvent> = _events.receiveAsFlow()

    init {
        load(noteId)
    }

    private fun load(noteId: Long) {
        if (noteId == 0L) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        viewModelScope.launch {
            val note = repository.get(noteId)
            if (note == null) {
                // Сейчас открывается пустой редактор, как будто заметка существует, и «Сохранить»
                // создаст запись с чужим id. Нужно показать сообщение «Заметка не найдена»
                // (строки note_not_found* уже есть) и не дать редактировать несуществующую запись.
                // Потребуется поле в NoteUiState и ветка в NoteScreen.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notFound = true
                    )
                }
                return@launch

            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    title = note.title,
                    body = note.body,
                    photoPath = note.photoPath,
                    tags = note.tags,
                    isSecret = note.isSecret
                )
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

    fun onBodyChange(value: String) = _uiState.update { it.copy(body = value) }

    fun onSecretChange(value: Boolean) = _uiState.update { it.copy(isSecret = value) }
    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.notFound) return@launch
            val id = repository.save(state.toNote())
            _uiState.update {
                it.copy(noteId = id)
            }
            _events.send(NoteEvent.Message(R.string.note_saved))
        }
    }


    fun delete() {
        val state = _uiState.value

        viewModelScope.launch {
            if (!state.isNew && !state.notFound) {
                deleteCoordinator.requestDelete(state.noteId)
            }

            _events.send(NoteEvent.Close)
        }
    }


    /** Выбор фото вернул `content://`-URI: копируем файл к себе, право на чтение URI временное. */
    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            runCatching { photoStore.copyToPrivateStorage(uri) }
                .onSuccess { path -> _uiState.update { it.copy(photoPath = path) } }
                .onFailure {
                    Log.w(TAG, "Cannot store photo", it)
                    _events.send(NoteEvent.Message(R.string.note_photo_failed))
                }
        }
    }

    fun onTagsChange(value: String) = _uiState.update { it.copy(tags = value) }

    fun removePhoto() = _uiState.update { it.copy(photoPath = null) }

    /** Вызывается экраном, когда разрешение на уведомления уже получено. */
    fun scheduleReminder() {
        val state = _uiState.value
        if (state.isNew) return

        reminderScheduler.schedule(
            noteId = state.noteId,
            title = state.title,
            isSecret = state.isSecret
        )

        _events.trySend(NoteEvent.Message(R.string.reminder_scheduled))
    }

    private companion object {
        const val TAG = "NoteViewModel"
    }
}
