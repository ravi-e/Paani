# TEST_INFRA — E2E Testing Infrastructure

This document outlines the testing infrastructure, framework configuration, Widget Keys contract, and execution instructions for the Paani senior hydration assistant rebuild.

---

## 1. Test Framework Configuration

We utilize the official Flutter `integration_test` package along with `flutter_test` for End-to-End (E2E) UI testing. 

### Dependency Specification (to be added to `pubspec.yaml`)
```yaml
dev_dependencies:
  flutter_test:
    sdk: flutter
  integration_test:
    sdk: flutter
  flutter_driver:
    sdk: flutter
```

### Directory Structure
```
integration_test/
├── tiers/
│   ├── tier1_feature_coverage_test.dart
│   ├── tier2_boundary_cases_test.dart
│   ├── tier3_cross_feature_test.dart
│   └── tier4_real_world_scenarios_test.dart
├── mocks/
│   ├── mock_database_helper.dart
│   ├── mock_haptic_service.dart
│   └── mock_tts_service.dart
└── app_test.dart (Main test runner importing and calling all tier suites)
```

### Test Entry Point Configuration (`integration_test/app_test.dart`)
```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'tiers/tier1_feature_coverage_test.dart' as tier1;
import 'tiers/tier2_boundary_cases_test.dart' as tier2;
import 'tiers/tier3_cross_feature_test.dart' as tier3;
import 'tiers/tier4_real_world_scenarios_test.dart' as tier4;

void main() {
  final IntegrationTestWidgetsFlutterBinding binding =
      IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  
  // Ensure that we run on physical devices or emulators with consistent frame rendering
  binding.framePolicy = LiveTestWidgetsFlutterBindingFramePolicy.fullyLive;

  group('Paani E2E Suite', () {
    tier1.main();
    tier2.main();
    tier3.main();
    tier4.main();
  });
}
```

---

## 2. Widget Keys Contract

To ensure E2E test scripts can reliably locate and interact with UI elements across all 5 screens, the following static keys are defined.

### Onboarding Screen
Used during first-time setup for seniors.
- **Screen Scaffold:** `Key('onboarding_screen')`
- **Welcome Title:** `Key('onboarding_title')`
- **Name Input Field:** `Key('onboarding_name_input')`
- **Name Input Error Text:** `Key('onboarding_error_text')`
- **Emoji Grid Container:** `Key('onboarding_emoji_grid')`
- **Emoji Item (Individual buttons):** `Key('onboarding_emoji_item_<emoji_name>')` (e.g. `onboarding_emoji_item_smile`, `onboarding_emoji_item_water`)
- **Save & Proceed Button:** `Key('onboarding_save_button')`

### Dashboard Screen
The primary daily view showing greeting and drinking progress.
- **Screen Scaffold:** `Key('dashboard_screen')`
- **Personalized Greeting Text:** `Key('dashboard_greeting_text')`
- **Hydration Progress Ring:** `Key('dashboard_progress_ring')`
- **Custom Cup Canvas Visualizer:** `Key('dashboard_cup_canvas')`
- **Log Drink Button (Main CTA):** `Key('dashboard_log_drink_button')`
- **Snooze Button (Secondary CTA):** `Key('dashboard_snooze_button')`
- **Undo Snackbar Button:** `Key('dashboard_undo_button')`
- **Navigation Tab - History:** `Key('dashboard_nav_history_button')`
- **Navigation Tab - Settings:** `Key('dashboard_nav_settings_button')`

### History Screen
A scrollable history list of logged drinks for the day.
- **Screen Scaffold:** `Key('history_screen')`
- **Logs ListView:** `Key('history_list_view')`
- **Log Item Row:** `Key('history_item_<index>')` (e.g. `history_item_0`, `history_item_1`)
- **Log Item Timestamp Text:** `Key('history_item_time_<index>')`
- **Log Item Delete Button:** `Key('history_item_delete_button_<index>')`
- **Empty State Display Text:** `Key('history_empty_state_text')`
- **Back Navigation Button:** `Key('history_back_button')`

### Settings Screen
Contains configuration for active hours, targets, and reminder preferences.
- **Screen Scaffold:** `Key('settings_screen')`
- **Editable Username Field:** `Key('settings_username_input')`
- **Target Decrease (Minus) Button:** `Key('settings_target_decrease_button')`
- **Target Increase (Plus) Button:** `Key('settings_target_increase_button')`
- **Target Glasses Value Text:** `Key('settings_target_value_text')`
- **Start Time Picker Row:** `Key('settings_start_time_tile')`
- **End Time Picker Row:** `Key('settings_end_time_tile')`
- **Voice Reminder Switch:** `Key('settings_voice_toggle')`
- **Sound Selector Row:** `Key('settings_sound_tile')`
- **Clear All Logs Button:** `Key('settings_clear_logs_button')`
- **Save Settings Button:** `Key('settings_save_button')`

### Celebration Screen (Overlay)
Full-screen celebration triggered upon goal completion.
- **Overlay Container:** `Key('celebration_overlay')`
- **Confetti Animation Widget:** `Key('celebration_confetti_widget')`
- **Goal Achieved Message Text:** `Key('celebration_title_text')`
- **Stats Summary Text:** `Key('celebration_stats_text')`
- **Dismiss/Close Button:** `Key('celebration_dismiss_button')`

---

## 3. Test Running Instructions & Environment Setup

### System Prerequisites Check
Before running E2E tests, verify that `puro` and `flutter` are available:
1. **Check Puro Version:**
   ```powershell
   puro --version
   ```
2. **Check Flutter Environment:**
   ```powershell
   puro flutter doctor
   ```
   *(Or if using global Flutter install: `flutter doctor`)*

### Environment Variables & Credentials
Create a `.env` file in the root directory and ensure the following keys are set:
```env
GEMINI_API_KEY=your_gemini_api_key_here
```

### Running E2E Tests on Target Devices
To execute the complete E2E integration test suite, run:

1. **Start your Android Emulator or connect a physical Android device.**
2. **Retrieve device list:**
   ```powershell
   puro flutter devices
   ```
3. **Execute E2E Integration Tests:**
   ```powershell
   # Running on the default/only connected Android device
   puro flutter test integration_test/app_test.dart

   # Running on a specific emulator/device
   puro flutter test -d <device_id> integration_test/app_test.dart
   ```
