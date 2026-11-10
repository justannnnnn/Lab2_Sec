package edu.androidsec.notes.photo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * TODO(ipc-2): выбор фото без разрешений на медиафайлы.
 *
 * Лекция 5, блок «Scoped storage и Photo Picker (2020–2024)»; проверяется шагом 1 чек-листа
 * (в манифесте нет `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE`, `CAMERA`).
 *
 * Системный выбор фото (`ActivityResultContracts.PickVisualMedia`) отдаёт `content://`-URI
 * с временным правом на чтение только выбранного файла. Разрешения на всю галерею не нужны.
 * Функция должна возвращать лямбду «открыть выбор фото»; выбранный URI передаётся в [onPicked]
 * (дальше его копирует `PhotoStore`, это уже сделано).
 *
 * Сейчас это заглушка: показывает Toast.
 */
@Composable
fun rememberPhotoPicker(
    onPicked: (Uri) -> Unit
): () -> Unit {

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onPicked)
    }

    return {
        launcher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }
}
