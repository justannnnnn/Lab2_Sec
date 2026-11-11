package edu.androidsec.notes.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.androidsec.notes.data.NotesRepository
import edu.androidsec.notes.data.db.DeleteCoordinator
import edu.androidsec.notes.domain.Note
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class NotesListViewModel(
    private val repository: NotesRepository,
    private val deleteCoordinator: DeleteCoordinator,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val notes: StateFlow<List<Note>?> = _searchQuery
        .debounce(250.milliseconds)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            repository.search(query)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // Помечаем заметку как ожидающую удаления
    fun delete(id: Long) {
        viewModelScope.launch {
            deleteCoordinator.requestDelete(id)
        }
    }
}
