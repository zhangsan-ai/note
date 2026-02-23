# Android Reminder Reliability And Voice-First Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Deliver full reminder reliability scope: alarm-like ring strategy, detailed lock-screen notifications, in-notification voice playback, voice-first creation flow, Chinese quick-time options with 2/3 days, and one-tap clear-completed with confirmation.

**Architecture:** Keep current single-module Compose + Room structure, add a foreground media playback service for notification-triggered audio, and expand repository/DAO queries to provide reminder-linked content and audio. Use AlarmManager alarm-clock strategy first, with existing fallback scheduling paths retained. UI remains Compose with state-driven confirmation and controls.

**Tech Stack:** Kotlin, Jetpack Compose, Room, AlarmManager, NotificationCompat, Android Service/MediaPlayer, BroadcastReceiver.

---

### Task 1: Add Failing Unit Tests For New Pure Policies

**Files:**
- Modify: `app/src/test/java/com/example/voicetodo/ui/TodoPresentationPolicyTest.kt`
- Modify: `app/src/main/java/com/example/voicetodo/ui/TodoPresentationPolicy.kt`

**Step 1: Write failing tests**
- Add tests for quick option labels in Chinese (`分钟/小时/天`) and for new `2天/3天` options.

**Step 2: Run test to verify RED**
- Run: `./gradlew :app:testDebugUnitTest --tests "*TodoPresentationPolicyTest"`
- Expected: FAIL before policy function implementation.

**Step 3: Implement minimal policy code**
- Add `formatQuickOptionLabel(minutes: Long)`.

**Step 4: Run test to verify GREEN**
- Run: `./gradlew :app:testDebugUnitTest --tests "*TodoPresentationPolicyTest"`
- Expected: PASS.

### Task 2: Expand Data APIs For Reminder Detail/Audio/Clear-Completed

**Files:**
- Modify: `app/src/main/java/com/example/voicetodo/data/dao/TodoDao.kt`
- Modify: `app/src/main/java/com/example/voicetodo/data/dao/ReminderDao.kt`
- Modify: `app/src/main/java/com/example/voicetodo/data/dao/VoiceNoteDao.kt`
- Modify: `app/src/main/java/com/example/voicetodo/data/AppRepository.kt`

**Step 1: Add failing test placeholders (if feasible) or document environment limitation**
- Preferred: add unit tests for repository helper policies.

**Step 2: Implement DAO queries**
- Add completed count/delete.
- Add reminder -> enabled done reminder ids.
- Add latest voice path by todo id.

**Step 3: Implement repository methods**
- `completedTodoCount()`
- `clearCompletedTodos()` (returns deleted count + canceled reminder ids)
- reminder detail lookup for notification (`todo content + audio path`).

### Task 3: Alarm Strategy + Notification Detail + Play Audio Action

**Files:**
- Modify: `app/src/main/java/com/example/voicetodo/alarm/AlarmScheduler.kt`
- Modify: `app/src/main/java/com/example/voicetodo/notification/ReminderNotifier.kt`
- Modify: `app/src/main/java/com/example/voicetodo/alarm/ReminderReceiver.kt`
- Modify: `app/src/main/java/com/example/voicetodo/alarm/ReminderActionReceiver.kt`

**Step 1: Implement alarm-clock-first scheduling**
- Prefer `setAlarmClock` on supported API.
- Keep exact/idle fallback.

**Step 2: Update reminder notification content**
- Show real todo text with `BigTextStyle`.
- Keep lock-screen visibility.
- Add full-screen intent.

**Step 3: Add action `播放语音`**
- Add action intent in notifier.
- Handle action in receiver.

### Task 4: Add Foreground Voice Playback Service + Test Ring

**Files:**
- Create: `app/src/main/java/com/example/voicetodo/voice/VoicePlaybackService.kt`
- Modify: `app/src/main/java/com/example/voicetodo/notification/ReminderNotifier.kt`
- Modify: `app/src/main/java/com/example/voicetodo/di/AppContainer.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Step 1: Implement service**
- Start foreground quickly, play provided audio path, stop on completion/error.

**Step 2: Wire action receiver to service**
- Notification action passes reminder/audio context.

**Step 3: Add test alarm sound API**
- Notifier exposes play/stop test alarm tone.

### Task 5: Voice-First UI + Clear-Completed Confirmation + Chinese Quick Labels

**Files:**
- Modify: `app/src/main/java/com/example/voicetodo/ui/MainUiState.kt`
- Modify: `app/src/main/java/com/example/voicetodo/ui/MainViewModel.kt`
- Modify: `app/src/main/java/com/example/voicetodo/ui/MainScreen.kt`
- Modify: `app/src/main/java/com/example/voicetodo/MainActivity.kt`

**Step 1: Update state and callbacks**
- Add clear-completed confirm state + completed count + test tone state.

**Step 2: Reorder UI to voice-first**
- Voice card first, manual as assist.
- Quick options labels from policy helper, include 2天/3天.

**Step 3: Add clear-completed interaction**
- Show `清除已完成（X）`.
- Add confirmation dialog.

**Step 4: Add test ringtone button**
- Toggle play/stop test ring from UI.

### Task 6: Verification + Commit + Push

**Files:**
- Modify: `README.md`

**Step 1: Run verification commands**
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`

**Step 2: If local Java unavailable, report exact limitation and rely on CI evidence**

**Step 3: Commit and push changes**
- Commit with scope-focused message.
- Push to `origin/main`.
