package edu.androidsec.notes.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import edu.androidsec.notes.R

/**
 * TODO(ipc-2): разрешение на уведомления.
 *
 * Лекция 5, блок «Runtime-разрешения: rationale, отказ, повторный отказ»;
 * проверяется шагом 3 чек-листа.
 *
 * Функция должна возвращать лямбду «запросить разрешение и, если оно есть, выполнить [onGranted]».
 * Правила:
 * - разрешение запрашивается только в момент, когда оно понадобилось (нажата кнопка «Напомнить»),
 *   а не при запуске приложения;
 * - до Android 13 (API 33) разрешения `POST_NOTIFICATIONS` не существует: сразу [onGranted];
 * - если система советует объяснить (`shouldShowRequestPermissionRationale`), сначала показать
 *   объяснение (например, `AlertDialog`), затем системный диалог;
 * - после отказа приложение продолжает работать (сообщение, без падения);
 * - после повторного отказа система больше не спрашивает: предложить открыть настройки
 *   (`Settings.ACTION_APP_NOTIFICATION_SETTINGS`).
 *
 * Пригодятся `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`
 * и строки `permission_*` из `res/values/strings.xml`.
 * Не забудьте `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` (ipc-1).
 *
 * Сейчас это заглушка: показывает Toast и ничего не запрашивает.
 */

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun rememberNotificationPermission(
    onGranted: () -> Unit
): () -> Unit {
    val context = LocalContext.current

    val preferences = remember {
        context.getSharedPreferences(
            "notification_permission",
            Context.MODE_PRIVATE
        )
    }

    var deniedCount by remember { mutableIntStateOf(preferences.getInt("denied_count", 0)) }
    var showRationale by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            deniedCount = 0
            preferences.edit { putInt("denied_count", 0) }
            onGranted()
        } else {
            deniedCount++
            preferences.edit { putInt("denied_count", deniedCount) }
            Toast.makeText(
                context,
                context.getString(R.string.permission_denied_toast),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onGranted()
            return
        }

        when {
            context.hasNotificationPermission() -> onGranted()

            deniedCount >= 2 -> showSettingsDialog = true

            ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) -> {
                showRationale = true
            }

            else -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.permission_rationale_title)) },
            text = { Text(stringResource(R.string.permission_rationale_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) {
                    Text(stringResource(R.string.permission_rationale_continue))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRationale = false }
                ) {
                    Text(stringResource(R.string.permission_rationale_cancel))
                }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(stringResource(R.string.permission_settings_title)) },
            text = { Text(stringResource(R.string.permission_settings_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSettingsDialog = false
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(
                                Settings.EXTRA_APP_PACKAGE,
                                context.packageName
                            )
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(stringResource(R.string.permission_settings_open))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSettingsDialog = false }
                ) {
                    Text(stringResource(R.string.permission_settings_cancel))
                }
            }
        )
    }

    return {
        requestPermission()
    }
}


/** Есть ли у приложения право показывать уведомления (до API 33 — всегда). */
fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
