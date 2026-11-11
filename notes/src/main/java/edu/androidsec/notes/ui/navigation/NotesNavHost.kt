package edu.androidsec.notes.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import edu.androidsec.notes.R
import edu.androidsec.notes.appContainer
import edu.androidsec.notes.deeplink.IncomingNote
import edu.androidsec.notes.ui.list.NotesListScreen
import edu.androidsec.notes.ui.note.NoteScreen
import kotlinx.coroutines.flow.StateFlow

/**
 * Граф навигации. [incoming] — результат разбора входящего интента Activity
 * (deep link или нажатие на уведомление); после обработки вызывается [onIncomingConsumed].
 */
@Composable
fun NotesNavHost(
    incoming: StateFlow<IncomingNote>,
    onIncomingConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val container = context.appContainer
    val incomingValue by incoming.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(container.deleteCoordinator) {
        container.deleteCoordinator.events.collect { id ->
            val result = snackbarHostState.showSnackbar(
                message = "Заметка удалена",
                actionLabel = "Отменить",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )

            when (result) {
                SnackbarResult.ActionPerformed -> {
                    container.deleteCoordinator.undo(id)
                }

                SnackbarResult.Dismissed -> {
                    container.deleteCoordinator.finalize(id)
                }
            }
        }
    }

    LaunchedEffect(incomingValue) {
        when (val value = incomingValue) {
            is IncomingNote.Open -> {
                navController.navigate(NoteRoute(value.id)) {
                    launchSingleTop = true
                }
                onIncomingConsumed()
            }

            IncomingNote.Invalid -> {
                Toast.makeText(
                    context,
                    R.string.deeplink_invalid,
                    Toast.LENGTH_LONG
                ).show()
                onIncomingConsumed()
            }

            IncomingNote.None -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = ListRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<ListRoute> {
                NotesListScreen(
                    onOpenNote = { id ->
                        navController.navigate(NoteRoute(id))
                    },
                )
            }

            composable<NoteRoute> { entry ->
                val route = entry.toRoute<NoteRoute>()

                NoteScreen(
                    noteId = route.id,
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
