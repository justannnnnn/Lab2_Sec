package edu.androidsec.notes.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import androidx.annotation.RequiresApi
import edu.androidsec.notes.appContainer
import edu.androidsec.notes.provider.TitlesProvider.Companion.AUTHORITY
import edu.androidsec.notes.provider.TitlesProvider.Companion.PERMISSION

/**
 * TODO(ipc-6): провайдер заголовков «для своих».
 *
 * Лекция 6, блоки «ContentProvider: exported, read/write permissions» и «Кто вызывает:
 * Binder.getCallingUid»; лекция 5, блок «Custom permissions и signature»; проверяется шагом 7
 * чек-листа.
 *
 * - Объявите в манифесте `<permission>` с именем [PERMISSION] и `protectionLevel="signature"`:
 *   его получат только приложения, подписанные тем же ключом, что и «Заметки».
 * - Объявите провайдер: authority [AUTHORITY], экспортирован (иначе клиент его не увидит)
 *   и защищён этим разрешением. Одного `readPermission` мало: запись останется открытой.
 * - Реализуйте [query]: только путь `/notes` (`UriMatcher`), только колонки `_id` и `title`
 *   (`MatrixCursor`; данные — `requireContext().appContainer.database.noteDao().titlesBlocking()`).
 *   Полезно записать в logcat `Binder.getCallingUid()` и `callingPackage`: кто спрашивает.
 * - Провайдер только для чтения: [insert], [update], [delete] отвергают вызов
 *   (`UnsupportedOperationException`).
 *
 * Проверка: `adb shell content query --uri content://edu.androidsec.notes.provider/notes`
 * даёт `SecurityException` (у shell нет разрешения); модуль `:client` с той же подписью
 * показывает заголовки; тот же модуль с другой подписью получает `SecurityException`.
 *
 * Сейчас это заглушка: `query()` возвращает `null`, провайдер не объявлен в манифесте.
 */
class TitlesProvider : ContentProvider() {
    companion object {
        const val TAG = "TitlesProvider"

        const val AUTHORITY =
            "edu.androidsec.notes.provider"

        const val PERMISSION =
            "edu.androidsec.notes.permission.READ_NOTES"

        const val PATH_NOTES = "notes"

        const val COLUMN_TITLE = "title"

        private const val NOTES = 1
    }

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(
            AUTHORITY,
            PATH_NOTES,
            NOTES
        )
    }

    override fun onCreate(): Boolean = true

    @RequiresApi(Build.VERSION_CODES.R)
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        if (uriMatcher.match(uri) != NOTES) throw IllegalArgumentException("Unknown URI: $uri")

        val columns = projection ?: arrayOf(BaseColumns._ID, COLUMN_TITLE)
        val allowedColumns = setOf(BaseColumns._ID, COLUMN_TITLE)

        if (columns.any { it !in allowedColumns }) throw IllegalArgumentException("Unsupported projection")

        val cursor = MatrixCursor(columns)
        val titles = requireContext()
            .appContainer
            .database
            .noteDao()
            .titlesBlocking()

        titles.forEach { note ->
            val row: Array<Any?> = columns.map { column ->
                when (column) {
                    BaseColumns._ID -> note.id
                    COLUMN_TITLE -> note.title
                    else -> null
                }
            }.toTypedArray()
            cursor.addRow(row)
        }

        return cursor
    }

    override fun getType(uri: Uri): String {
        if (uriMatcher.match(uri) != NOTES) throw IllegalArgumentException("Unknown URI: $uri")
        return "vnd.android.cursor.dir/vnd.$AUTHORITY.notes"
    }

    override fun insert(
        uri: Uri,
        values: ContentValues?
    ): Uri {
        throw UnsupportedOperationException("Provider is read-only")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("Provider is read-only")
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("Provider is read-only")
    }
}

