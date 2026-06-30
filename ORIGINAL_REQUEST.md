# Original User Request

## Initial Request — 2026-06-13T16:01:46+05:30

Rebuild the **Paani** senior hydration assistant app (currently native Jetpack Compose Android) into a Flutter app, faithfully implementing the Google Stitch redesign with 5 screens: Onboarding, Dashboard, Hydration History, Reminder Settings, and Goal Celebration.

Working directory: `d:\AI sandbox\Paani`

Integrity mode: development

---

## Context

### Design System (from Google Stitch)
**App Name:** Paani — Senior Hydration Assistant  
**Audience:** Senior citizens requiring extreme legibility and ease of use  
**Design principles:** High-utility minimalism, WCAG AAA contrast, functional clarity, "one task at a time" philosophy

**Color Palette:**
- Primary (Deep Sea Blue): `#00327d` / Primary container: `#0047ab`
- Secondary (Vitality Green): `#006d35` / Secondary container: `#8df9a8`
- Background / Surface: `#f7f9fb`
- On-Surface: `#191c1e`
- Tertiary: `#363636` / Tertiary container: `#4d4d4d`
- Error: `#ba1a1a`
- Outline: `#737784` / Outline variant: `#c3c6d5`

**Typography:** Atkinson Hyperlegible Next (designed for aging eyes)
- Headline LG: 32px, ExtraBold (800), -0.01em letter spacing
- Headline MD: 28px, Bold (700)
- Body LG: 22px, Regular (400), 32px line height
- Body MD: 18px, Regular (400), 28px line height  
- Label LG: 20px, Bold (700), +0.02em letter spacing
- Label MD: 16px, Bold (700)

**Spacing & Touch Targets:**
- Minimum touch target: **56×56dp** (critical for seniors)
- Page margin: 24dp
- Stack gap between sections: 32dp
- Internal card padding: minimum 24dp

**Shape:**
- Buttons & inputs: 8dp radius
- Cards & containers: 16dp radius
- Circular badges: full circle

**Components:**
- Buttons: Full-width, 64dp height minimum, Primary color with White text, 20px Bold label
- Input fields: Always-visible labels ABOVE the field (no floating labels), 2px Primary border
- List items: Minimum 72dp height, chevron icons, separator lines
- Progress ring: Segmented circular ring using Secondary (Vitality Green) color
- Toggles: Must show "On/Off" text labels, not just color
- Icons: 2px+ stroke weight, always accompanied by text label

### Screens to Implement (5 total)

**1. Onboarding Screen** — First launch only
- Full-page gradient background
- Large water drop emoji hero (96dp circle)
- "Welcome to Paani" headline + subtitle text
- Name input field (always-visible label, 2px primary border, rounded corners)
- "Get Started" CTA button (full-width, 64dp, disabled until 2+ chars entered)

**2. Dashboard (Main) Screen** — Home screen
- Header row: App title "💧 Paani piyo!" + greeting + Settings icon button
- Greeting banner card: time-of-day emoji + "Good morning/afternoon/evening, [Name]!"
- Large glass counter display: 58sp number, bold, primary color
- Segmented progress ring: 270° arc, split into N segments (one per target glass), filled in Vitality Green as user logs drinks — clickable
- Water bottle visual indicator (custom painted bottle that fills up)
- "💧 I Drank a Glass" primary CTA button — 64dp tall, full-width
- Next reminder pill badge: pulsing dot + timestamp
- Snooze buttons row: 1 min, 2 min, 5 min options
- Notification permission warning banner (if permissions missing)
- Goal completion celebration card (appears when target is met)

**3. Hydration History Screen** — Accessible from settings/history
- List of today's drink logs with timestamps
- 72dp minimum height per list item
- Chevron icons on each item
- Empty state with encouraging message

**4. Reminder Settings Screen** — Accessible from Dashboard gear icon
- Start time / End time pickers (hour sliders or time pickers)
- Reminder interval picker (e.g., every 30 min, 60 min, 90 min, 120 min)
- Daily target glasses picker (stepper: 1–16)
- Voice reminder toggle (with "On/Off" text label)
- Clear today's logs button (destructive, with confirmation)
- Custom alarm sound selector (system ringtones)
- All inputs: always-visible labels, large touch targets (56dp min)

**5. Goal Celebration Screen / Overlay** — Shown when daily target is hit
- Celebration animation (confetti or sparkle effect)
- "🎉 Spectacular! You've hit your daily goal!" headline
- Summary stats card
- "Keep Going!" or "Reset" options

### Existing App Functionality to Preserve
- **Persistence:** SQLite (via sqflite) — DrinkLog table (id, timestamp) and ReminderSettings table
- **Scheduling:** Background alarms using `android_alarm_manager_plus` or `flutter_local_notifications` with scheduled notifications
- **Voice reminders:** `flutter_tts` — speaks "Time to drink some water, [Name]!" when reminder fires
- **Alarm service:** Looping alarm sound when reminder triggers, auto-stopped when user logs a drink
- **Undo:** 5-second snackbar with UNDO option after logging a drink
- **Snooze:** 1/2/5-minute snooze delays for reminders
- **Haptic feedback:** Strong haptic on drink log, light haptic on settings/snooze
- **Notification actions:** "✅ DRANK WATER NOW" and "⏲️ SNOOZE 5 MINS" actions in notification

### Setup Instructions
1. Install Puro (Flutter version manager): `winget install pingbird.Puro`
2. Use Puro to install Flutter stable: `puro use stable`
3. Back up existing native Android files to `./native_android_backup/` — move these files/folders: `app/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/`, `gradlew`, `gradlew.bat`, `gradle.properties`, `.gradle/`, `.idea/`, `.kotlin/`, `local.properties`
4. Initialize Flutter project in the root: `flutter create --org com.paani --project-name paani .` (or use puro's flutter command)
5. App package ID: `com.paani.hydration`

---

## Requirements

### R1. Flutter Project Setup
Install Flutter via Puro, back up existing native Kotlin/Gradle files to `native_android_backup/`, initialize a Flutter project in `d:\AI sandbox\Paani` with package name `com.paani.hydration`. The app must target Android API 21+ (minimum) and API 35 (target/compile).

### R2. Design System Implementation
Implement a Flutter theme (ThemeData) that faithfully matches the Stitch design system: color palette, typography scale using the Atkinson Hyperlegible Next font (bundled as an asset), spacing constants, touch target sizes (56dp minimum for all interactive elements), and shape radii. This must be the single source of truth for all visual styling.

### R3. All 5 Screens
Implement all 5 screens (Onboarding, Dashboard, History, Settings, Goal Celebration) in Flutter/Dart, faithfully matching the Stitch redesign specifications and component guidelines described above. The app must navigate between screens using Flutter's built-in navigation (Navigator or go_router). First-launch shows Onboarding; subsequent launches show Dashboard.

### R4. State Management & Persistence
Persist drink logs and reminder settings using sqflite (SQLite). Implement a state management solution (Provider, Riverpod, or BLoC) that keeps the UI reactive — when a drink is logged, the counter and progress ring update immediately. Support undo within a 5-second snackbar window.

### R5. Background Reminders & Notifications
Implement scheduled background reminders using `flutter_local_notifications` with the `android_alarm_manager_plus` package (or equivalent). Reminders must: (a) fire within the configured active hours window, (b) trigger a looping alarm sound (stopped when user logs a drink), (c) speak a personalised TTS voice cue via `flutter_tts`, and (d) show a persistent notification with "Drank Water" and "Snooze 5 min" action buttons.

---

## Acceptance Criteria

### Setup & Build
- [ ] `puro` installs successfully and `flutter doctor` shows no critical Android issues
- [ ] `flutter build apk --debug` completes without errors
- [ ] The APK installs and launches on an Android emulator/device

### UI & Navigation
- [ ] App shows Onboarding on first launch (when no name is stored); shows Dashboard on subsequent launches
- [ ] All 5 screens are navigable as described
- [ ] All interactive elements have touch targets ≥ 56dp × 56dp
- [ ] Typography uses Atkinson Hyperlegible Next font (verifiable by inspecting font rendering)
- [ ] Color palette matches Stitch specs (Deep Sea Blue primary, Vitality Green secondary)
- [ ] `flutter analyze` reports 0 errors and 0 warnings

### Core Functionality
- [ ] Tapping "I Drank a Glass" increments the counter, fills one progress ring segment, and shows a 5-second UNDO snackbar
- [ ] UNDO removes the last log entry and decrements the counter
- [ ] Settings are persisted across app restarts (verified by killing and relaunching the app)
- [ ] Drink logs persist across app restarts

### Reminders
- [ ] A scheduled reminder fires at the configured interval during active hours
- [ ] The notification has both "Drank Water" and "Snooze 5 min" action buttons
- [ ] Logging a drink from the app or notification stops any looping alarm sound
- [ ] Snooze delays the next reminder by the correct duration
