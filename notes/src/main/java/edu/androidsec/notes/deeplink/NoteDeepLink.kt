package edu.androidsec.notes.deeplink

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

/** Что пришло во входящем интенте Activity. */
sealed interface IncomingNote {
    /** Открыть заметку с корректным id (существует ли она, проверит экран заметки). */
    data class Open(val id: Long) : IncomingNote

    /** Ссылка на заметку есть, но параметр некорректен: показать сообщение, ничего не открывать. */
    data object Invalid : IncomingNote

    /** Обычный запуск без ссылки. */
    data object None : IncomingNote
}

/**
 * TODO(ipc-3): deep link `notes://note/<id>`.
 *
 * Лекция 6, блок «Deep links и App Links»; проверяется шагом 4 чек-листа.
 *
 * 1. В манифесте добавьте `MainActivity` intent-filter с действием VIEW, категориями DEFAULT и
 *    BROWSABLE и данными `scheme="notes"`, `host="note"`.
 * 2. Реализуйте [parse]: данные интента — недоверенный ввод. Принимайте только схему `notes`,
 *    хост `note` и ровно один сегмент пути, который разбирается в положительное Long.
 *    Всё остальное — [IncomingNote.Invalid]: приложение показывает сообщение и не падает
 *    (обработка [IncomingNote] в `NotesNavHost` уже есть).
 * 3. Несуществующий id (`notes://note/999999`) не должен ронять экран заметки и не должен
 *    открывать пустой редактор: см. TODO(ipc-3) в `NoteViewModel`.
 *
 * Проверка: `adb shell am start -a android.intent.action.VIEW -d notes://note/1`,
 * затем то же с `999999`, `abc` и пустым id; в logcat не должно быть `FATAL EXCEPTION`.
 *
 * Сейчас это заглушка: любой интент считается обычным запуском.
 */
object NoteDeepLink {
    const val SCHEME = "notes"
    const val HOST = "note"

    const val APP_LINK_HOST = "justannnnnn.github.io"

    /** Ссылка на заметку в фиксированном формате `notes://note/<id>`. */
    fun uriFor(noteId: Long): Uri = "$SCHEME://$HOST/$noteId".toUri()

    fun parse(intent: Intent?): IncomingNote {
        val uri = intent?.data ?: return IncomingNote.None

        if (intent.action != Intent.ACTION_VIEW || uri.query != null || uri.fragment != null) {
            return IncomingNote.Invalid
        }

        val segments = uri.pathSegments

        val idString = when {
            uri.scheme == SCHEME && uri.host == HOST && segments.size == 1 -> {
                segments[0]
            }

            uri.scheme == "https" && uri.host == APP_LINK_HOST && segments.size == 2 && segments[0] == "note" -> {
                segments[1]
            }

            else -> return IncomingNote.Invalid
        }

        if (!idString.matches(Regex("[0-9]+"))) return IncomingNote.Invalid

        val id = idString.toLongOrNull() ?: return IncomingNote.Invalid
        if (id <= 0L) return IncomingNote.Invalid

        return IncomingNote.Open(id)
    }
}
