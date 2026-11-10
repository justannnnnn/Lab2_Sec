package edu.androidsec.notes.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import edu.androidsec.notes.appContainer

/**
 * TODO(ipc-4): ресивер кнопки «Готово» в уведомлении.
 *
 * Лекция 6, блоки «PendingIntent и mutability» и «BroadcastReceiver: exported и явные интенты»;
 * проверяется шагами 2 и 5 чек-листа.
 *
 * - Объявите ресивер в манифесте и закройте его для других приложений: его вызывает только наш
 *   PendingIntent (система отправляет интент от имени приложения, поэтому экспорт не нужен).
 * - В [onReceive] проверяйте действие и extras: это недоверенный ввод, даже если ресивер закрыт.
 * - По «Готово» закройте уведомление (`NotificationManagerCompat.cancel(noteId)`) и отмените
 *   напоминание (`context.appContainer.reminderScheduler.cancel(noteId)`).
 * - Запишите в logcat строку `Log.i(TAG, "DONE received for note $noteId")`. Тег и текст
 *   фиксированы: по ним работает `scripts/check.sh` (шаг 2) и проверка «Готово» (шаг 5).
 *
 * Имя класса и строка действия фиксированы заданием: по ним проходит защита
 * (`adb shell am broadcast -n edu.androidsec.notes/.reminder.ReminderActionReceiver
 *   -a edu.androidsec.notes.action.DONE` не должен ничего делать).
 */
class ReminderActionReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "ReminderActionReceiver"
        const val ACTION_DONE = "edu.androidsec.notes.action.DONE"
        const val EXTRA_NOTE_ID = "edu.androidsec.notes.extra.NOTE_ID"
    }
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action != ACTION_DONE) return

        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (noteId <= 0L || noteId > Int.MAX_VALUE) return

        NotificationManagerCompat.from(context).cancel(noteId.toInt())
        context.appContainer.reminderScheduler.cancel(noteId)
    }
}

