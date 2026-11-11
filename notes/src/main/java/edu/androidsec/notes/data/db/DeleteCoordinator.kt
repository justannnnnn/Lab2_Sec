package edu.androidsec.notes.data.db

import edu.androidsec.notes.data.NotesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow


class DeleteCoordinator(
    private val repository: NotesRepository,
) {
    private val deleteEvents = Channel<Long>(Channel.BUFFERED)

    val events: Flow<Long> = deleteEvents.receiveAsFlow()

    suspend fun requestDelete(id: Long) {
        repository.markPendingDelete(id)
        deleteEvents.send(id)
    }

    suspend fun undo(id: Long) {
        repository.restore(id)
    }

    suspend fun finalize(id: Long) {
        repository.finalizeDelete(id)
    }
}

