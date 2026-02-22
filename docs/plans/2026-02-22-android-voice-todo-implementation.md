# Android Voice Todo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a minimal Android app that creates todos via text or voice, parses Chinese time commands, schedules reminders, and keeps reminding every 5 minutes until dismissed.

**Architecture:** Use a single Android app module with a Compose UI, Room persistence, a parser domain layer, and alarm/notification receivers. Keep business rules in use cases and pure Kotlin helpers so they can be unit-tested. Use AlarmManager + BroadcastReceiver for reminder precision and reboot recovery.

**Tech Stack:** Kotlin, Jetpack Compose, Room, AlarmManager, BroadcastReceiver, NotificationCompat, SpeechRecognizer, MediaRecorder.

---

### Task 1: Project Bootstrap

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`

**Step 1: Write the failing test scaffold command**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: FAIL because project files are missing.

**Step 2: Create minimal Gradle and Android app scaffolding**

- Add Android application plugin and Kotlin plugin.
- Add Compose, Room, lifecycle, and test dependencies.

**Step 3: Run the test command again**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: Task graph resolves (tests may still fail due to missing app code).

**Step 4: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties app/build.gradle.kts app/proguard-rules.pro app/src/main/AndroidManifest.xml
git commit -m "chore: bootstrap android compose project"
```

### Task 2: Time Parser (TDD)

**Files:**
- Create: `app/src/main/java/com/example/voicetodo/parser/ChineseTimeParser.kt`
- Create: `app/src/main/java/com/example/voicetodo/parser/ParseResult.kt`
- Test: `app/src/test/java/com/example/voicetodo/parser/ChineseTimeParserTest.kt`

**Step 1: Write failing tests for relative and absolute expressions**

```kotlin
@Test
fun parseRelativeMinutes() { /* "10分钟后提醒喝水" */ }

@Test
fun parseAbsoluteTonight() { /* "今晚8点提醒开会" */ }
```

**Step 2: Run only parser tests**

```bash
./gradlew :app:testDebugUnitTest --tests "*ChineseTimeParserTest"
```

Expected: FAIL with unresolved parser class/method.

**Step 3: Implement minimal parser to satisfy tests**

- Relative: 分钟/小时.
- Absolute: 今天/今晚/明天/后天 + 时分.

**Step 4: Re-run parser tests**

Expected: PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/voicetodo/parser app/src/test/java/com/example/voicetodo/parser/ChineseTimeParserTest.kt
git commit -m "feat: add chinese time parser with unit tests"
```

### Task 3: Reminder Repeat Policy (TDD)

**Files:**
- Create: `app/src/main/java/com/example/voicetodo/reminder/ReminderPolicy.kt`
- Test: `app/src/test/java/com/example/voicetodo/reminder/ReminderPolicyTest.kt`

**Step 1: Write failing tests for snooze and auto-repeat rules**

```kotlin
@Test
fun snoozeAlwaysAddsFiveMinutes() {}

@Test
fun noActionSchedulesFiveMinutesLater() {}
```

**Step 2: Run reminder policy tests**

```bash
./gradlew :app:testDebugUnitTest --tests "*ReminderPolicyTest"
```

Expected: FAIL.

**Step 3: Implement minimal pure Kotlin policy**

- `nextAtAfterNoAction(now)` => `now + 5min`
- `nextAtAfterSnooze(now)` => `now + 5min`

**Step 4: Run tests again**

Expected: PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/voicetodo/reminder/ReminderPolicy.kt app/src/test/java/com/example/voicetodo/reminder/ReminderPolicyTest.kt
git commit -m "feat: add reminder repeat policy"
```

### Task 4: Room Data Layer

**Files:**
- Create: `app/src/main/java/com/example/voicetodo/data/entity/TodoEntity.kt`
- Create: `app/src/main/java/com/example/voicetodo/data/entity/ReminderEntity.kt`
- Create: `app/src/main/java/com/example/voicetodo/data/entity/VoiceNoteEntity.kt`
- Create: `app/src/main/java/com/example/voicetodo/data/dao/TodoDao.kt`
- Create: `app/src/main/java/com/example/voicetodo/data/dao/ReminderDao.kt`
- Create: `app/src/main/java/com/example/voicetodo/data/dao/VoiceNoteDao.kt`
- Create: `app/src/main/java/com/example/voicetodo/data/AppDatabase.kt`
- Create: `app/src/main/java/com/example/voicetodo/data/repo/TodoRepository.kt`

**Step 1: Write a failing repository behavior test (or DAO contract test)**

```kotlin
@Test
fun createTodoWithReminderPersistsAllRows() {}
```

**Step 2: Run targeted test**

```bash
./gradlew :app:testDebugUnitTest --tests "*Repository*"
```

Expected: FAIL.

**Step 3: Implement Room entities/DAO/repository minimally**

**Step 4: Re-run targeted test**

Expected: PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/voicetodo/data
git commit -m "feat: add room entities dao and repository"
```

### Task 5: Alarm and Notification Actions

**Files:**
- Create: `app/src/main/java/com/example/voicetodo/alarm/AlarmScheduler.kt`
- Create: `app/src/main/java/com/example/voicetodo/alarm/ReminderReceiver.kt`
- Create: `app/src/main/java/com/example/voicetodo/alarm/ReminderActionReceiver.kt`
- Create: `app/src/main/java/com/example/voicetodo/notification/ReminderNotifier.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Step 1: Write failing test for intent action routing (pure helper)**

```kotlin
@Test
fun actionCloseDisablesReminder() {}

@Test
fun actionSnoozeReschedulesFiveMinutesLater() {}
```

**Step 2: Run tests**

Expected: FAIL.

**Step 3: Implement scheduler, notifier, receivers**

- Notification actions: `ACTION_DISMISS`, `ACTION_SNOOZE`.
- On timeout/no action: schedule +5 minutes.

**Step 4: Run tests again**

Expected: PASS for pure helper tests.

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/voicetodo/alarm app/src/main/java/com/example/voicetodo/notification app/src/main/AndroidManifest.xml
git commit -m "feat: add alarm scheduling and notification actions"
```

### Task 6: Voice Capture and STT Integration

**Files:**
- Create: `app/src/main/java/com/example/voicetodo/voice/VoiceRecorder.kt`
- Create: `app/src/main/java/com/example/voicetodo/voice/SpeechToTextClient.kt`
- Create: `app/src/main/java/com/example/voicetodo/voice/VoiceStorage.kt`

**Step 1: Write failing unit tests for filename/path and state transitions**

```kotlin
@Test
fun buildAudioPathCreatesM4aName() {}

@Test
fun recorderStateTransitionsAreValid() {}
```

**Step 2: Run tests**

Expected: FAIL.

**Step 3: Implement recording and speech client wrappers**

- Store raw audio file path in app files dir.
- Expose start/stop/cancel and latest recognized text.

**Step 4: Run tests again**

Expected: PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/voicetodo/voice
git commit -m "feat: add voice recording and stt wrappers"
```

### Task 7: Compose UI and ViewModel

**Files:**
- Create: `app/src/main/java/com/example/voicetodo/MainActivity.kt`
- Create: `app/src/main/java/com/example/voicetodo/ui/MainViewModel.kt`
- Create: `app/src/main/java/com/example/voicetodo/ui/MainUiState.kt`
- Create: `app/src/main/java/com/example/voicetodo/ui/MainScreen.kt`
- Create: `app/src/main/java/com/example/voicetodo/ui/theme/*`

**Step 1: Write failing ViewModel unit tests**

```kotlin
@Test
fun manualQuickOptionCreatesReminder() {}

@Test
fun voiceParseFailureCreatesTodoWithoutReminder() {}
```

**Step 2: Run ViewModel tests**

Expected: FAIL.

**Step 3: Implement minimal ViewModel and Compose UI**

- Text input + quick chips.
- Voice controls + playback action.
- Todo list with next reminder time and status.

**Step 4: Run tests again**

Expected: PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/voicetodo/MainActivity.kt app/src/main/java/com/example/voicetodo/ui
git commit -m "feat: add minimalist compose ui and main viewmodel"
```

### Task 8: Boot Recovery and Permissions

**Files:**
- Create: `app/src/main/java/com/example/voicetodo/boot/BootReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Step 1: Write failing test for boot reschedule use case helper**

```kotlin
@Test
fun enabledRemindersAreRescheduledOnBoot() {}
```

**Step 2: Run targeted tests**

Expected: FAIL.

**Step 3: Implement BootReceiver and restore logic**

- Handle `BOOT_COMPLETED`.
- Re-schedule all `is_enabled=true` reminders.

**Step 4: Run tests again**

Expected: PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/voicetodo/boot app/src/main/AndroidManifest.xml
git commit -m "feat: add boot restore for active reminders"
```

### Task 9: Verification and Delivery

**Files:**
- Create: `README.md`
- Create: `.gitignore`

**Step 1: Run full unit test suite**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: PASS.

**Step 2: Run lint baseline (optional if SDK unavailable)**

```bash
./gradlew :app:lintDebug
```

Expected: PASS or documented environment limitation.

**Step 3: Document setup and GitHub upload instructions**

- Include permissions needed (mic, notifications, exact alarms, boot completed).
- Include sample voice commands.

**Step 4: Commit final docs and configuration**

```bash
git add README.md .gitignore
git commit -m "docs: add usage and github publish guide"
```
