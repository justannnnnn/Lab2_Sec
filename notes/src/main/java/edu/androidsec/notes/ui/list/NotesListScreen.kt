package edu.androidsec.notes.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.androidsec.notes.R
import edu.androidsec.notes.appContainer
import edu.androidsec.notes.domain.Note
import edu.androidsec.notes.ui.navigation.NoteRoute
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun NotesListScreen(onOpenNote: (Long) -> Unit) {
    val container = LocalContext.current.appContainer

    val viewModel: NotesListViewModel = viewModel {
        NotesListViewModel(container.notesRepository, container.deleteCoordinator)
    }

    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    NotesListContent(
        notes = notes,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onOpen = onOpenNote,
        onAdd = { onOpenNote(NoteRoute.NEW_NOTE_ID) },
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesListContent(
    modifier: Modifier = Modifier,
    notes: List<Note>?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    val searchState = rememberTextFieldState(initialText = searchQuery)

    LaunchedEffect(searchQuery) {
        if (searchState.text.toString() != searchQuery) {
            searchState.setTextAndPlaceCursorAtEnd(searchQuery)
        }
    }

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .distinctUntilChanged()
            .collect(onSearchQueryChange)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.list_title))
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.list_add)
                )
            }
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                state = searchState,
                label = { Text("Поиск заметок") },
                placeholder = { Text("Название, текст или тег") },
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    notes == null -> {
                        CircularProgressIndicator(
                            Modifier.align(Alignment.Center)
                        )
                    }

                    notes.isEmpty() -> {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                stringResource(R.string.list_empty)
                            } else {
                                "Ничего не найдено"
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp)
                        )
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(notes, key = { it.id }) { note ->
                                NoteRow(
                                    note = note,
                                    onClick = { onOpen(note.id) },
                                    onDelete = { onDelete(note.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 4.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank {
                        stringResource(R.string.note_untitled)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (note.isSecret) {
                    // Содержимое секретной заметки не показываем
                } else if (note.body.isNotBlank()) {
                    Text(
                        text = note.body,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (note.isSecret) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Секретная заметка",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.list_delete)
                )
            }
        }
    }
}
