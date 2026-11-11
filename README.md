# Лабораторная работа № 2. Разрешения, компоненты и IPC. Тестирование

## 1. Deep Links

Проверка обработки Deep Links выполнялась с помощью ADB.

### 1.1. Открытие существующей заметки

**Команда:**

```bash
adb shell am start -a android.intent.action.VIEW -d "notes://note/1"
```

**Вывод консоли:**

```text
Starting: Intent { act=android.intent.action.VIEW dat=notes://note/... }
Warning: Activity not started, its current task has been brought to the front
```

**Результат:** успешно открывается существующая заметка с ID `1`.

**Демонстрация:**

https://github.com/user-attachments/assets/29f653a8-cbb4-4c35-a372-9a1dd1b45493

---

### 1.2. Открытие несуществующей заметки

**Команда:**

```bash
adb shell am start -a android.intent.action.VIEW -d "notes://note/999999"
```

**Вывод консоли:**

```text
Starting: Intent { act=android.intent.action.VIEW dat=notes://note/... }
Warning: Activity not started, intent has been delivered to currently running top-most instance.
```

**Результат:** приложение обрабатывает Deep Link и отображает сообщение о том, что заметка не найдена.

**Демонстрация:**

https://github.com/user-attachments/assets/ce537718-14ca-43ee-b37a-be3d916e6446

---

### 1.3. Проверка некорректного ID заметки

**Команда:**

```bash
adb shell am start -a android.intent.action.VIEW -d "notes://note/abc"
```

**Вывод консоли:**

```text
Starting: Intent { act=android.intent.action.VIEW dat=notes://note/... }
Warning: Activity not started, intent has been delivered to currently running top-most instance.
```

**Результат:** приложение распознаёт некорректный идентификатор и отображает сообщение об ошибке ссылки.

**Демонстрация:**

https://github.com/user-attachments/assets/d53164d1-62e5-48d0-baa1-ad098d1c64a5

---

### 1.4. Проверка ссылки без ID заметки

**Команда:**

```bash
adb shell am start -a android.intent.action.VIEW -d "notes://note/"
```

**Вывод консоли:**

```text
Starting: Intent { act=android.intent.action.VIEW dat=notes://note/... }
Warning: Activity not started, intent has been delivered to currently running top-most instance.
```

**Результат:** приложение обрабатывает ссылку без идентификатора и отображает сообщение о некорректной ссылке.

**Демонстрация:**

https://github.com/user-attachments/assets/74c74be1-769f-460d-9b1e-7e3c73a2606c

---

**Примечание:** сообщение `Activity not started` не означает ошибку обработки Deep Link. Оно указывает на то, что Android повторно использовал уже запущенный экземпляр Activity и передал ему новый Intent.


## 2. ReminderActionReceiver и FileProvider

### 2.1. Попытка вызвать BroadcastReceiver извне

**Команда:**

```bash
adb shell am broadcast -n edu.androidsec.notes/.reminder.ReminderActionReceiver -a edu.androidsec.notes.action.DONE
```

**Вывод консоли:**

```text
Broadcasting: Intent {
    act=edu.androidsec.notes.action.DONE
    flg=0x400000
    cmp=edu.androidsec.notes/.reminder.ReminderActionReceiver
}

Broadcast completed: result=0
```

**Результат:** отправка broadcast выполняется, однако сам по себе `result=0` не подтверждает успешное выполнение логики обработчика.

### 2.2. Проверка работы ReminderActionReceiver через LogCat

**Команда:**

```bash
adb logcat -d -s ReminderActionReceiver:I
```

**Результат:** проверяется наличие сообщений от `ReminderActionReceiver` в системном журнале LogCat.

---

### 2.3. Попытка прочитать файл через FileProvider извне приложения

**Команда:**

```bash
adb shell content read --uri content://edu.androidsec.notes.files/exports/1.txt
```

**Вывод консоли:**

```text
Error while accessing provider:edu.androidsec.notes.files

java.lang.SecurityException: Permission Denial: opening provider androidx.core.content.FileProvider from (null) (pid=10433, uid=2000) that is not exported from UID 10862
```

**Результат:** доступ к файлу запрещён. `FileProvider` не экспортирован (`exported="false"`), поэтому внешнее приложение или процесс не может напрямую получить доступ к файлу через Content URI без предоставленного разрешения.

Это подтверждает, что файлы приложения не доступны для произвольного внешнего чтения.

---

**Итог:** проверены попытка внешнего вызова `ReminderActionReceiver`, наличие записей обработчика в LogCat и защита файлов через `FileProvider` от несанкционированного доступа.


## 3. TitlesProvider

Проверка защиты `ContentProvider` с помощью signature-level разрешения `edu.androidsec.notes.permission.READ_NOTES`.

### 3.1. Попытка получить заголовки заметок без signature-разрешения

**Команда:**

```bash
adb shell content query --uri content://edu.androidsec.notes.provider/notes
```

**Вывод консоли:**

```text
Error while accessing provider:edu.androidsec.notes.provider

java.lang.SecurityException: Permission Denial: opening provider
edu.androidsec.notes.provider.TitlesProvider from (null)
(pid=10936, uid=2000) requires
edu.androidsec.notes.permission.READ_NOTES
```

**Результат:** доступ к заголовкам заметок запрещён, поскольку вызывающий процесс не обладает необходимым signature-разрешением.

---

### 3.2. Попытка вставить запись без signature-разрешения

**Команда:**

```bash
adb shell content insert --uri content://edu.androidsec.notes.provider/notes --bind title:s:Test
```

**Вывод консоли:**

```text
Error while accessing provider:edu.androidsec.notes.provider

java.lang.SecurityException: Permission Denial: opening provider
edu.androidsec.notes.provider.TitlesProvider from (null)
(pid=10989, uid=2000) requires
edu.androidsec.notes.permission.READ_NOTES
```

**Результат:** операция вставки недоступна. Провайдер отклоняет обращение из-за отсутствия необходимого разрешения.

---

### 3.3. Попытка удалить записи без signature-разрешения

**Команда:**

```bash
adb shell content delete --uri content://edu.androidsec.notes.provider/notes
```

**Вывод консоли:**

```text
Error while accessing provider:edu.androidsec.notes.provider

java.lang.SecurityException: Permission Denial: opening provider
edu.androidsec.notes.provider.TitlesProvider from (null)
(pid=11029, uid=2000) requires
edu.androidsec.notes.permission.READ_NOTES
```

**Результат:** операция удаления также запрещена. Внешний процесс не может получить доступ к провайдеру без необходимого разрешения.

---

### Итог

Проверены три операции над `TitlesProvider` без предоставленного signature-разрешения:

- **Query** — чтение заголовков заметок запрещено.
- **Insert** — вставка записей запрещена.
- **Delete** — удаление записей запрещено.

Во всех трёх случаях система выбрасывает `SecurityException`, подтверждая защиту `ContentProvider` с помощью signature-level разрешения `READ_NOTES`.

## 4. Автоматические проверки

```text
PS C:\Users\User0\StudioProjects\Lab2_Sec> & "C:\Program Files\Git\bin\bash.exe" ./scripts/check.sh all                                                                                                                             
SDK: C:\Users\User0\AppData\Local\Android\Sdk                                  
Устройство: ABEYVB3321004225, Android 14 (API 34)                                                                                                                                                                                   
                                                                                                                                                                                                                                    
=== manifest (шаг 1) ===                                                                                                                                                                                                            
/data/app/~~ZuWa7XIMEyCOC9miHZpZVA==/edu.androidsec.notes-pn_Qfwxnwz6rpfNwU8-fAA==/base.apk: 1 file pulled, 0 skipped. 38.3 MB/s (12460709 bytes in 0.310s)                                                                         
Компоненты (тип|имя|exported|permission):
    | activity|edu.androidsec.notes.MainActivity|true|-
    | receiver|edu.androidsec.notes.reminder.ReminderActionReceiver|false|-
    | provider|androidx.core.content.FileProvider|false|-
    | provider|edu.androidsec.notes.provider.TitlesProvider|true|-
    | provider|androidx.startup.InitializationProvider|false|-
    | service|androidx.room.MultiInstanceInvalidationService|false|-
    | receiver|androidx.profileinstaller.ProfileInstallReceiver|true|android.permission.DUMP
PASS  у каждого компонента явно задан android:exported
PASS  экспортированы только: edu.androidsec.notes.MainActivity edu.androidsec.notes.provider.TitlesProvider и компоненты под signature-разрешением
    | edu.androidsec.notes.MainActivity
    | edu.androidsec.notes.provider.TitlesProvider
    | androidx.profileinstaller.ProfileInstallReceiver
    | NOTE: androidx.profileinstaller.ProfileInstallReceiver экспортирован, но закрыт android.permission.DUMP (signature|privileged|development)
PASS  edu.androidsec.notes.reminder.ReminderActionReceiver объявлен, exported=false
    | receiver|edu.androidsec.notes.reminder.ReminderActionReceiver|false|-
PASS  androidx.core.content.FileProvider объявлен, exported=false
    | provider|androidx.core.content.FileProvider|false|-
PASS  edu.androidsec.notes.provider.TitlesProvider объявлен, exported=true
    | provider|edu.androidsec.notes.provider.TitlesProvider|true|-
PASS  edu.androidsec.notes.permission.READ_NOTES объявлено с protectionLevel=signature
    | <permission android:name="edu.androidsec.notes.permission.READ_NOTES" android:protectionLevel="0x2" /> 
PASS  TitlesProvider защищён readPermission и writePermission=edu.androidsec.notes.permission.READ_NOTES
Запрошенные разрешения:
    | android.permission.POST_NOTIFICATIONS
    | edu.androidsec.notes.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
PASS  лишних разрешений нет (допустимы: android.permission.POST_NOTIFICATIONS edu.androidsec.notes.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION)
PASS  POST_NOTIFICATIONS запрошено (нужно для напоминания)

=== closed (шаг 2) ===
PASS  am broadcast (явный и в пакет): ресивер не вызывался
    | Broadcasting: Intent { act=edu.androidsec.notes.action.DONE flg=0x400000 cmp=edu.androidsec.notes/.reminder.ReminderActionReceiver (has extras) }
    | Broadcast completed: result=0
    | Broadcasting: Intent { act=edu.androidsec.notes.action.DONE flg=0x400000 pkg=edu.androidsec.notes (has extras) }
    | Broadcast completed: result=0
    | <logcat: строк ReminderActionReceiver нет>
PASS  content read на FileProvider отклонён
    | Error while accessing provider:edu.androidsec.notes.files
    | java.lang.SecurityException: Permission Denial: opening provider androidx.core.content.FileProvider from (null) (pid=11331, uid=2000) that is not exported from UID 10862
    |   at android.os.Parcel.createExceptionOrNull(Parcel.java:3098)
    |   at android.os.Parcel.createException(Parcel.java:3082)
    |   at android.os.Parcel.readException(Parcel.java:3065)
    |   at android.os.Parcel.readException(Parcel.java:3007)
    |   at android.app.IActivityManager$Stub$Proxy.getContentProviderExternal(IActivityManager.java:7825)
    |   at com.android.commands.content.Content$Command.execute(Content.java:516)
    |   at com.android.commands.content.Content.main(Content.java:735)
    |   at com.android.internal.os.RuntimeInit.nativeFinishInit(Native Method)
    |   at com.android.internal.os.RuntimeInit.main(RuntimeInit.java:408)
    | Caused by: android.os.RemoteException: Remote stack trace:
    |   at com.android.server.am.ContentProviderHelper.checkAssociationAndPermissionLocked(ContentProviderHelper.java:803)
    |   at com.android.server.am.ContentProviderHelper.getContentProviderImpl(ContentProviderHelper.java:334)
    |   at com.android.server.am.ContentProviderHelper.getContentProviderExternalUnchecked(ContentProviderHelper.java:172)
    |   at com.android.server.am.ContentProviderHelper.getContentProviderExternal(ContentProviderHelper.java:166)
    |   at com.android.server.am.ActivityManagerService.getContentProviderExternal(ActivityManagerService.java:7494)
    | callee: null 1959/5115

=== deeplink (шаг 4) ===
PASS  notes://note/1: открыто приложением, падения нет
    | Starting: Intent { act=android.intent.action.VIEW dat=notes://note/... }
    | Status: ok
    | LaunchState: COLD
    | Activity: edu.androidsec.notes/.MainActivity
    | TotalTime: 1250
    | WaitTime: 1257
    | Complete
PASS  notes://note/999999: открыто приложением, падения нет
    | Starting: Intent { act=android.intent.action.VIEW dat=notes://note/... }
    | Status: ok
    | LaunchState: COLD
    | Activity: edu.androidsec.notes/.MainActivity
    | TotalTime: 1242
    | WaitTime: 1249
    | Complete
PASS  notes://note/abc: открыто приложением, падения нет
    | Starting: Intent { act=android.intent.action.VIEW dat=notes://note/... }
    | Status: ok
    | LaunchState: COLD
    | Activity: edu.androidsec.notes/.MainActivity
    | TotalTime: 1267
    | WaitTime: 1273
    | Complete
PASS  notes://note/: открыто приложением, падения нет
    | Starting: Intent { act=android.intent.action.VIEW dat=notes://note/... }
    | Status: ok
    | LaunchState: COLD
    | Activity: edu.androidsec.notes/.MainActivity
    | TotalTime: 1278
    | WaitTime: 1286
    | Complete
Что показано на экране (сообщение об ошибке для abc и пустого id) — проверяется вручную.

=== provider (шаг 7) ===
PASS  content query от shell отклонён (SecurityException)
    | Error while accessing provider:edu.androidsec.notes.provider
    | java.lang.SecurityException: Permission Denial: opening provider edu.androidsec.notes.provider.TitlesProvider from (null) (pid=11669, uid=2000) requires edu.androidsec.notes.permission.READ_NOTES or edu.androidsec.notes.permission.READ_NOTES
    |   at android.os.Parcel.createExceptionOrNull(Parcel.java:3098)
    |   at android.os.Parcel.createException(Parcel.java:3082)
    |   at android.os.Parcel.readException(Parcel.java:3065)
    |   at android.os.Parcel.readException(Parcel.java:3007)
    |   at android.app.IActivityManager$Stub$Proxy.getContentProviderExternal(IActivityManager.java:7825)
    |   at com.android.commands.content.Content$Command.execute(Content.java:516)
    |   at com.android.commands.content.Content.main(Content.java:735)
    |   at com.android.internal.os.RuntimeInit.nativeFinishInit(Native Method)
    |   at com.android.internal.os.RuntimeInit.main(RuntimeInit.java:408)
    | Caused by: android.os.RemoteException: Remote stack trace:
    |   at com.android.server.am.ContentProviderHelper.checkAssociationAndPermissionLocked(ContentProviderHelper.java:803)
    |   at com.android.server.am.ContentProviderHelper.getContentProviderImpl(ContentProviderHelper.java:334)
    |   at com.android.server.am.ContentProviderHelper.getContentProviderExternalUnchecked(ContentProviderHelper.java:172)
    |   at com.android.server.am.ContentProviderHelper.getContentProviderExternal(ContentProviderHelper.java:166)
    |   at com.android.server.am.ActivityManagerService.getContentProviderExternal(ActivityManagerService.java:7494)
    | callee: null 1959/4135
PASS  content insert отклонён
    | Error while accessing provider:edu.androidsec.notes.provider
    | java.lang.SecurityException: Permission Denial: opening provider edu.androidsec.notes.provider.TitlesProvider from (null) (pid=11685, uid=2000) requires edu.androidsec.notes.permission.READ_NOTES or edu.androidsec.notes.permission.READ_NOTES
    |   at android.os.Parcel.createExceptionOrNull(Parcel.java:3098)
    |   at android.os.Parcel.createException(Parcel.java:3082)
    |   at android.os.Parcel.readException(Parcel.java:3065)
    |   at android.os.Parcel.readException(Parcel.java:3007)
    |   at android.app.IActivityManager$Stub$Proxy.getContentProviderExternal(IActivityManager.java:7825)
    |   at com.android.commands.content.Content$Command.execute(Content.java:516)
    |   at com.android.commands.content.Content.main(Content.java:735)
    |   at com.android.internal.os.RuntimeInit.nativeFinishInit(Native Method)
    |   at com.android.internal.os.RuntimeInit.main(RuntimeInit.java:408)
    | Caused by: android.os.RemoteException: Remote stack trace:
    |   at com.android.server.am.ContentProviderHelper.checkAssociationAndPermissionLocked(ContentProviderHelper.java:803)
    |   at com.android.server.am.ContentProviderHelper.getContentProviderImpl(ContentProviderHelper.java:334)
    |   at com.android.server.am.ContentProviderHelper.getContentProviderExternalUnchecked(ContentProviderHelper.java:172)
    |   at com.android.server.am.ContentProviderHelper.getContentProviderExternal(ContentProviderHelper.java:166)
    |   at com.android.server.am.ActivityManagerService.getContentProviderExternal(ActivityManagerService.java:7494)
    | callee: null 1959/4135
PASS  content delete отклонён
    | Error while accessing provider:edu.androidsec.notes.provider
    | java.lang.SecurityException: Permission Denial: opening provider edu.androidsec.notes.provider.TitlesProvider from (null) (pid=11698, uid=2000) requires edu.androidsec.notes.permission.READ_NOTES or edu.androidsec.notes.permission.READ_NOTES
    |   at android.os.Parcel.createExceptionOrNull(Parcel.java:3098)
    |   at android.os.Parcel.createException(Parcel.java:3082)
    |   at android.os.Parcel.readException(Parcel.java:3065)
    |   at android.os.Parcel.readException(Parcel.java:3007)
    |   at android.app.IActivityManager$Stub$Proxy.getContentProviderExternal(IActivityManager.java:7825)
    |   at com.android.commands.content.Content$Command.execute(Content.java:516)
    |   at com.android.commands.content.Content.main(Content.java:735)
    |   at com.android.internal.os.RuntimeInit.nativeFinishInit(Native Method)
    |   at com.android.internal.os.RuntimeInit.main(RuntimeInit.java:408)
    | Caused by: android.os.RemoteException: Remote stack trace:
    |   at com.android.server.am.ContentProviderHelper.checkAssociationAndPermissionLocked(ContentProviderHelper.java:803)
    |   at com.android.server.am.ContentProviderHelper.getContentProviderImpl(ContentProviderHelper.java:334)
    |   at com.android.server.am.ContentProviderHelper.getContentProviderExternalUnchecked(ContentProviderHelper.java:172)
    |   at com.android.server.am.ContentProviderHelper.getContentProviderExternal(ContentProviderHelper.java:166)
    |   at com.android.server.am.ActivityManagerService.getContentProviderExternal(ActivityManagerService.java:7494)
    | callee: null 1959/4135
Клиент с той же подписью (видит заголовки) и клиент с другой подписью (SecurityException) — проверяются вручную.

ИТОГО: PASS=18 FAIL=0
```

# Лабораторная работа #2. Разрешения, компоненты и IPC

## Фиксированные идентификаторы

Их нельзя менять: по ним работают `scripts/check.sh` и клиент преподавателя.

| Что | Значение |
|---|---|
| applicationId «Заметок» | `edu.androidsec.notes` |
| applicationId клиента | `edu.androidsec.notes.client` |
| Deep link | `notes://note/<id>` |
| Authority FileProvider | `edu.androidsec.notes.files` |
| Authority провайдера заголовков | `edu.androidsec.notes.provider`, путь `/notes` |
| Signature-разрешение | `edu.androidsec.notes.permission.READ_NOTES` |
| Класс ресивера | `edu.androidsec.notes.reminder.ReminderActionReceiver` |
| Действие ресивера | `edu.androidsec.notes.action.DONE` |
| Id канала уведомлений | `reminders` |

## Что где доделывать

Каждый пункт задания помечен в коде комментарием `TODO(ipc-N)`. В комментарии написано, что
сделать, к какому блоку лекции это относится и каким шагом чек-листа проверяется.

| TODO | Файлы | Состояние в заготовке |
|---|---|---|
| ipc-1 | `notes/src/main/AndroidManifest.xml` | закомментированный блок разрешений: нужные и ненужные; `exported` только у лаунчера |
| ipc-2 | `reminder/ReminderPermission.kt`, `photo/PhotoPicker.kt` | функции с сигнатурами и заглушками, кнопки показывают Toast «TODO(ipc-2)» |
| ipc-3 | манифест, `deeplink/NoteDeepLink.kt`, `ui/note/NoteViewModel.kt` | нет intent-filter, `parse()` ничего не разбирает, несуществующий id не обрабатывается |
| ipc-4 | `reminder/ReminderScheduler.kt`, `reminder/ReminderActionReceiver.kt` | уведомление без `PendingIntent`, пустой `onReceive`, ресивера нет в манифесте |
| ipc-5 | `share/ShareNote.kt`, `res/xml/file_paths.xml` | функция пишет файл в `cacheDir/exports/` и возвращает его; `file_paths.xml` нет; провайдера нет в манифесте |
| ipc-6 | `provider/TitlesProvider.kt`, манифест | `query()` возвращает `null`; провайдера и `<permission>` нет в манифесте |

Что уже работает и трогать не нужно: список и экран заметки, Room (три стартовые заметки с
id 1, 2, 3), канал уведомлений, копирование выбранного фото во внутреннее хранилище
(`photo/PhotoStore.kt`), запись заметки в текстовый файл (`share/NoteExporter.kt`),
модуль `:client` целиком.

### Как устроен код

Один Gradle-проект, два application-модуля, только AndroidX и Kotlin. Каждый экран — это пара
файлов в `ui/<экран>/`:

- `…ViewModel.kt` держит состояние экрана в `StateFlow` и вызывает репозиторий
  (`data/NotesRepository.kt`, под ним Room). Одноразовые события — «показать сообщение»,
  «закрыть экран» — уходят через `Channel`.
- `…Screen.kt` (Compose) рисует состояние и вызывает методы ViewModel. Системные диалоги
  (запрос разрешения, выбор фото, «Поделиться») запускаются отсюда, потому что им нужна Activity.

Зависимости собираются вручную в `di/AppContainer.kt`.

## Окружение

- Android Studio актуальной версии. Gradle-демону нужна Java 25
  (`gradle/gradle-daemon-jvm.properties`); если её нет, Gradle скачает её сам при первой
  сборке — для этого один раз нужен интернет.
- **Эмулятор**: образ **Google Play**, API 34 или новее (заготовка проверена на API 36,
  Pixel 6). Обычный образ без Play тоже подойдёт, но в нём `adb root` доступен, и часть
  проверок теряет смысл.
- В SDK Manager включите **Android SDK Command-line Tools**: оттуда берётся `apkanalyzer`
  для `scripts/check.sh`.
- Сборка: `./gradlew :notes:assembleDebug :client:assembleDebug` (Windows: `gradlew.bat`).
  AGP 9.4, Kotlin 2.4, compileSdk 37, minSdk 26.
- Фото для проверки Photo Picker на эмуляторе: закинуть любую картинку и попросить систему
  её увидеть:

  ```bash
  adb push photo.jpg /sdcard/Pictures/photo.jpg
  adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/photo.jpg
  ```

## Скрипт проверки

`scripts/check.sh` запускает adb-часть чек-листа. Нужны запущенный эмулятор с установленными
«Заметками», переменная `ANDROID_HOME` (или SDK в стандартном месте) и **bash** (на Windows —
Git Bash; на macOS запускайте именно `bash scripts/check.sh`, не `zsh`).

```bash
bash scripts/check.sh all
```

Отдельно: `manifest` (шаг 1), `closed` (шаг 2), `deeplink` (шаг 4), `provider` (шаг 7).
Каждая проверка печатает `PASS`/`FAIL` и сырой вывод adb под ним; код возврата 0 — все
PASS. На заготовке скрипт обязан падать по всем шагам безопасности: заглушки настоящие.

Что делает скрипт:

- `manifest` — скачивает установленный APK, печатает merged manifest через `apkanalyzer`,
  проверяет `exported` у каждого компонента, список экспортированных (допустимы
  `MainActivity`, `TitlesProvider` и компоненты, закрытые разрешением уровня `signature`, —
  они отмечаются `NOTE`), объявление ресивера, `FileProvider`, провайдера с
  signature-разрешением и список `uses-permission` (допустимо только `POST_NOTIFICATIONS`).
- `closed` — два `am broadcast` с действием «Готово»: явный на ресивер и в пакет приложения.
  Ресивер не должен среагировать: в logcat не должно появиться ни одной строки с тегом
  `ReminderActionReceiver`. Затем `content read` на URI FileProvider (должен быть
  `Permission Denial`).
- `deeplink` — `am start -d notes://note/{1,999999,abc,}`; после каждого смотрит logcat на
  `FATAL EXCEPTION`. Если ваш intent-filter строже и ссылки с `abc` или пустым id вообще не
  открываются (`unable to resolve Intent`), это тоже PASS; для `1` и `999999` — FAIL.
- `provider` — `content query`, `content insert`, `content delete`; все должны быть отклонены.

Подсказка к шагу 1: merged manifest — это не ваш `AndroidManifest.xml`, а его слияние с
манифестами библиотек. Посмотрите его в Android Studio (вкладка *Merged Manifest*) или
выводом `check.sh manifest`: библиотеки добавляют свои компоненты, и среди них может быть
экспортированный. Для каждого такого компонента разберитесь, кто может его вызвать. Если он
закрыт разрешением уровня `signature`, это не дыра, и скрипт его пропустит с пометкой `NOTE`.
Если компонент приложению не нужен, его можно убрать из итогового манифеста через
`tools:node="remove"`; на защите будьте готовы объяснить, зачем.

## Клиент с другим ключом

По умолчанию оба модуля подписываются debug-ключом Android Studio
(`~/.android/debug.keystore`), поэтому у вас клиент видит заголовки. Преподаватель соберёт
тот же модуль своим ключом и должен увидеть `SecurityException`. Проверьте это сами:

```bash
keytool -genkeypair -keystore other.jks -storepass other123 -keypass other123 -alias other -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Other, O=Test, C=RU"
./gradlew :client:assembleDebug -Psigning.storeFile=/абсолютный/путь/other.jks -Psigning.storePassword=other123 -Psigning.keyAlias=other -Psigning.keyPassword=other123
adb uninstall edu.androidsec.notes.client
adb install client/build/outputs/apk/debug/client-debug.apk
```

Вместо `-P` можно положить те же четыре параметра в `client/signing.properties`
(файл в `.gitignore`). Удалять установленный клиент перед установкой обязательно: подпись
изменилась, а обновление поверх старой подписи система не разрешает. Чтобы вернуть
debug-подпись — собрать без параметров и снова `uninstall` + `install`.

## Флоу работы

- Создайте свой репозиторий из ветки `starter` (fork или `git clone` + `git push` в свой
  remote) и работайте в нём. **История ветки `starter` должна сохраниться**: не пересоздавайте
  репозиторий и не схлопывайте коммиты заготовки — по истории видно, что именно сделали вы.
- Ссылку на репозиторий загрузите в Moodle до дедлайна. В README своего репозитория добавьте
  раздел с командами adb и их выводом для пунктов 3–6.
- Не коммитьте `local.properties`, ключи (`*.jks`) и `client/signing.properties`.
