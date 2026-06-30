# Project: Paani — Senior Hydration Assistant Rebuild

## Architecture
- **Language/Framework:** Flutter (Dart)
- **Package Name:** `com.paani.hydration`
- **Target OS:** Android (Min API 21, Compile/Target API 35)
- **Font:** Atkinson Hyperlegible Next (designed for aging eyes, loaded as asset)
- **State Management:** Provider (reactive state for UI updates)
- **Persistence:** SQLite (`sqflite` package) for database persistence:
  - `DrinkLog` table: `id` (INTEGER PRIMARY KEY), `timestamp` (TEXT)
  - `ReminderSettings` table: `id` (INTEGER PRIMARY KEY), `start_time` (TEXT), `end_time` (TEXT), `interval_minutes` (INTEGER), `target_glasses` (INTEGER), `voice_reminder_enabled` (INTEGER), `custom_sound_uri` (TEXT), `user_name` (TEXT)
- **Notifications & Alarms:**
  - `flutter_local_notifications` + `android_alarm_manager_plus` (or native alarm scheduler wrapper) for background reminders.
  - Custom alarm looping service triggered on notification.
  - Personal TTS reminders using `flutter_tts`.
  - Notification Actions: "✅ DRANK WATER NOW", "⏲️ SNOOZE 5 MINS".
- **Haptics:** Strong haptic on drink log, light haptic on settings and snooze.

## Code Layout
```
lib/
├── core/
│   ├── theme.dart            # Design system, Atkinson Hyperlegible Next font scale, colors
│   ├── database_helper.dart  # sqflite connection and table operations
│   └── constants.dart        # Padding, radii, key strings
├── features/
│   ├── onboarding/           # Onboarding UI (gradient, emoji, name input, validation)
│   ├── dashboard/            # Home dashboard (greeting, progress ring, custom cup canvas, CTAs, snooze)
│   ├── history/              # History list (today's logs, chevron separators, minimum 72dp heights)
│   ├── settings/             # Settings UI (start/end pickers, target stepper, voice toggle, sound selector)
│   └── celebration/          # Goal completion overlay/screen (confetti animation, stats summary)
├── services/
│   ├── alarm_service.dart    # Manages custom looping sound & stops on log
│   ├── tts_service.dart      # Speaks personalized reminder via flutter_tts
│   ├── notification_service.dart # Handles notifications & action buttons
│   └── background_alarm_manager.dart # Coordinates alarm manager triggers during active hours
└── main.dart                 # Application entry point, Provider initialization, routing
```

## Milestones
| # | Name | Track | Scope | Dependencies | Status |
|---|------|-------|-------|-------------|--------|
| 1 | E2E Test Suite | E2E Testing | Design E2E test infrastructure, 4-tier test cases, and publish `TEST_READY.md`. | None | DONE |
| 2 | Setup & Backup | Implementation | Puro installation, native Kotlin backup, `flutter create` in root, package ID setup. | None | DONE |
| 3 | Design System | Implementation | ThemeData, color palette, Atkinson Hyperlegible Next integration, touch targets. | M2 | DONE |
| 4 | Database & State | Implementation | SQLite database creation, models, Provider for reactive logs & settings, Undo mechanism. | M2 | PLANNED |
| 5 | UI & Navigation | Implementation | Implement Onboarding, Dashboard, History, Settings, Celebration UI and routing. | M3, M4 | PLANNED |
| 6 | Background & Reminders | Implementation | Local notifications, alarms, custom sound loop, voice reminder, snooze logic, haptics. | M4 | PLANNED |
| 7 | E2E Integration & Audit | Implementation | Phase 1: Pass 100% E2E tests (Milestone 1). Phase 2: Adversarial coverage hardening (Challenger/Auditor loops). | M1, M5, M6 | PLANNED |

## Interface Contracts

### State Provider Interface
- `List<DrinkLog> drinkLogs`
- `ReminderSettings settings`
- `Future<void> fetchLogs()`
- `Future<void> fetchSettings()`
- `Future<void> logDrink()` (Triggers strong haptic, schedules undo timer, registers SQLite log)
- `Future<void> undoLastDrink()` (Clears last log, updates UI state)
- `Future<void> updateSettings(ReminderSettings newSettings)`
- `Future<void> clearLogs()` (Wipes DrinkLog table, updates UI state)

### Background Reminders ↔ Alarm/TTS/Notification Services
- Trigger callback runs on alarm manager fire.
- Reads `ReminderSettings` from sqlite.
- Checks if current time is within active hours (`start_time` to `end_time`).
- If active:
  - Plays looping alarm sound.
  - Speaks voice reminder: "Time to drink some water, [Name]!" (if voice reminder enabled).
  - Fires persistent notification with action buttons "✅ DRANK WATER NOW" and "⏲️ SNOOZE 5 MINS".
- Stopping:
  - Invoking `logDrink()` from notification action or app stops looping sound and dismisses notification.
  - Invoking `snooze(minutes)` stops looping sound, dismisses notification, and schedules next alarm in `minutes`.
