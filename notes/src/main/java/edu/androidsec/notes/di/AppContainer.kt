package edu.androidsec.notes.di

import android.content.Context
import edu.androidsec.notes.data.NotesRepository
import edu.androidsec.notes.data.RoomNotesRepository
import edu.androidsec.notes.data.db.DeleteCoordinator
import edu.androidsec.notes.data.db.NotesDatabase
import edu.androidsec.notes.photo.PhotoStore
import edu.androidsec.notes.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Ручной DI-контейнер: один экземпляр на процесс, живёт в [edu.androidsec.notes.NotesApp].
 * Сторонние DI-библиотеки в лабораторной не используются, чтобы все зависимости были читаемы.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: NotesDatabase by lazy { NotesDatabase.create(appContext) }

    val notesRepository: NotesRepository by lazy { RoomNotesRepository(database.noteDao()) }

    val photoStore: PhotoStore by lazy { PhotoStore(appContext) }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(appContext, appScope) }

    val deleteCoordinator: DeleteCoordinator by lazy {
        DeleteCoordinator(notesRepository)
    }
}
