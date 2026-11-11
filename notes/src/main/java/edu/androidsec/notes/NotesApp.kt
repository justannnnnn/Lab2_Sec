package edu.androidsec.notes

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import edu.androidsec.notes.di.AppContainer
import edu.androidsec.notes.reminder.ReminderScheduler
import kotlinx.coroutines.launch

class NotesApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        createReminderChannel()
        container.appScope.launch {
            container.notesRepository.finalizeAllPendingDeletes()
        }
    }

    /** Канал уведомлений создаётся один раз при старте; id канала фиксирован заданием. */
    private fun createReminderChannel() {
        val channel = NotificationChannel(
            ReminderScheduler.CHANNEL_ID,
            getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.channel_reminders_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

/** Доступ к контейнеру из Activity, ресивера и провайдера. */
val android.content.Context.appContainer: AppContainer
    get() = (applicationContext as NotesApp).container
