package edu.androidsec.notes.ui.note

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.os.PersistableBundle
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.androidsec.notes.R
import edu.androidsec.notes.appContainer
import edu.androidsec.notes.photo.PhotoStore
import edu.androidsec.notes.photo.rememberPhotoPicker
import edu.androidsec.notes.reminder.rememberNotificationPermission
import edu.androidsec.notes.share.ShareNote
import kotlinx.coroutines.flow.distinctUntilChanged

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NoteScreen(noteId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val viewModel: NoteViewModel = viewModel(key = "note-$noteId") {
        NoteViewModel(
            noteId,
            container.notesRepository,
            container.photoStore,
            container.reminderScheduler,
            container.deleteCoordinator
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val activity = context.findActivity()
    val window = activity?.window

    SideEffect {
        if (window != null) {
            if (!state.isLoading && !state.notFound && state.isSecret) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            }
        }
    }

    DisposableEffect(window) {
        onDispose {
            window?.clearFlags(
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    // Системные диалоги (разрешение, выбор фото) живут в UI; результат уходит во ViewModel.
    val requestNotificationPermission =
        rememberNotificationPermission(onGranted = viewModel::scheduleReminder)
    val pickPhoto = rememberPhotoPicker(onPicked = viewModel::onPhotoPicked)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NoteEvent.Message -> Toast.makeText(context, event.resId, Toast.LENGTH_SHORT)
                    .show()

                NoteEvent.Close -> onBack()
            }
        }
    }

    NoteContent(
        state = state,
        photoStore = container.photoStore,
        onBack = onBack,
        onDelete = viewModel::delete,
        onTitleChange = viewModel::onTitleChange,
        onBodyChange = viewModel::onBodyChange,
        onAttachPhoto = pickPhoto,
        onRemovePhoto = viewModel::removePhoto,
        onSave = viewModel::save,
        onRemind = requestNotificationPermission,
        onTagsChange = viewModel::onTagsChange,
        onSecretChange = viewModel::onSecretChange,
        onShare = { ShareNote.shareNoteAsFile(context, state.toNote()) },
        onCopy = {
            val clipboard = context.getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

            val clip = ClipData.newPlainText(
                if (state.isSecret) "Секретная заметка" else state.title,
                state.body
            )

            if (state.isSecret) {
                clip.description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }

            clipboard.setPrimaryClip(clip)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteContent(
    state: NoteUiState,
    photoStore: PhotoStore,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onSecretChange: (Boolean) -> Unit,
    onAttachPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
    onRemind: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit
) {
    val titleState = rememberTextFieldState(initialText = state.title)
    val bodyState = rememberTextFieldState(initialText = state.body)
    val tagsState = rememberTextFieldState(initialText = state.tags)

    LaunchedEffect(state.title) {
        if (titleState.text.toString() != state.title) {
            titleState.setTextAndPlaceCursorAtEnd(state.title)
        }
    }

    LaunchedEffect(state.body) {
        if (bodyState.text.toString() != state.body) {
            bodyState.setTextAndPlaceCursorAtEnd(state.body)
        }
    }

    LaunchedEffect(state.tags) {
        if (tagsState.text.toString() != state.tags) {
            tagsState.setTextAndPlaceCursorAtEnd(state.tags)
        }
    }

    // Передаём изменения из TextFieldState в ViewModel.
    LaunchedEffect(titleState) {
        snapshotFlow { titleState.text.toString() }
            .distinctUntilChanged()
            .collect(onTitleChange)
    }

    LaunchedEffect(bodyState) {
        snapshotFlow { bodyState.text.toString() }
            .distinctUntilChanged()
            .collect(onBodyChange)
    }

    LaunchedEffect(tagsState) {
        snapshotFlow { tagsState.text.toString() }
            .distinctUntilChanged()
            .collect(onTagsChange)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isNew) R.string.note_title_new else R.string.note_title_edit))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.note_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.note_delete)
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.notFound -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = stringResource(R.string.note_not_found))
                        Button(onClick = onBack) {
                            Text(stringResource(R.string.note_back))
                        }
                    }
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        state = titleState,
                        inputTransformation = TitleInputTransformation,
                        label = {
                            Text(stringResource(R.string.note_field_title))
                        },
                        supportingText = {
                            Text("До 100 символов, без переносов строк")
                        },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        state = bodyState,
                        inputTransformation = BodyInputTransformation,
                        label = {
                            Text(stringResource(R.string.note_field_body))
                        },
                        supportingText = {
                            Text("Максимум 5000 символов")
                        },
                        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 4),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        state = tagsState,
                        inputTransformation = TagsInputTransformation,
                        label = {
                            Text("Теги")
                        },
                        supportingText = {
                            Text("До 5 тегов через пробел. Каждый тег: 1–20 символов, a-z, 0-9 и дефис")
                        },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Секретная заметка",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Содержимое заметки будет скрываться",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Switch(
                            checked = state.isSecret,
                            onCheckedChange = onSecretChange
                        )
                    }

                    OutlinedButton(
                        onClick = onCopy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Копировать текст")
                    }

                    state.photoPath?.let { NotePhoto(path = it, photoStore = photoStore) }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAttachPhoto) { Text(stringResource(R.string.note_attach_photo)) }
                        if (state.photoPath != null) {
                            OutlinedButton(onClick = onRemovePhoto) { Text(stringResource(R.string.note_remove_photo)) }
                        }
                    }

                    Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.note_save))
                    }
                    OutlinedButton(
                        onClick = onRemind,
                        enabled = !state.isNew,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.note_remind))
                    }
                    OutlinedButton(
                        onClick = onShare,
                        enabled = !state.isNew,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.note_share))
                    }
                    if (state.isNew) {
                        Text(
                            stringResource(R.string.note_save_first),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotePhoto(path: String, photoStore: PhotoStore) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = path) {
        value = photoStore.loadPreview(path)
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = stringResource(R.string.note_photo_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
    }
}

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
