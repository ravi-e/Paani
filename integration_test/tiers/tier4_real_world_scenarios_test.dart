import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:paani/main.dart';

void main() {
  group('Tier 4: Real-World Scenarios (5 Tests)', () {

    testWidgets('1. Scenario: Complete Senior Onboarding and First Drink Logging Flow', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      
      // Step 1: Onboarding
      expect(find.byKey(const Key('onboarding_screen')), findsOneWidget);
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Grandpa Arthur');
      await tester.ensureVisible(find.byKey(const Key('onboarding_emoji_item_water')));

      await tester.tap(find.byKey(const Key('onboarding_emoji_item_water')));
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      // Step 2: Dashboard Verification
      expect(find.byKey(const Key('dashboard_screen')), findsOneWidget);
      expect(find.text('Hello, Grandpa Arthur'), findsOneWidget);
      expect(find.text('0 / 8\nGlasses'), findsOneWidget);

      // Step 3: Log Drink
      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();
      expect(find.text('1 / 8\nGlasses'), findsOneWidget);

      // Step 4: History Inspection
      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_history_button')));

      await tester.tap(find.byKey(const Key('dashboard_nav_history_button')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('history_screen')), findsOneWidget);
      expect(find.byKey(const Key('history_item_0')), findsOneWidget);

      // Step 5: Back to Dashboard
      await tester.ensureVisible(find.byKey(const Key('history_back_button')));

      await tester.tap(find.byKey(const Key('history_back_button')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('dashboard_screen')), findsOneWidget);
    });

    testWidgets('2. Scenario: Settings Adjustments & Target Calibration Flow', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      
      // Onboard first
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Arthur');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      // Navigate to Settings
      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));

      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('settings_screen')), findsOneWidget);

      // Adjust Settings
      await tester.ensureVisible(find.byKey(const Key('settings_target_decrease_button')));

      await tester.tap(find.byKey(const Key('settings_target_decrease_button'))); // 8 -> 7
      await tester.ensureVisible(find.byKey(const Key('settings_start_time_tile')));

      await tester.tap(find.byKey(const Key('settings_start_time_tile'))); // 08:00 -> 07:00
      await tester.ensureVisible(find.byKey(const Key('settings_end_time_tile')));

      await tester.tap(find.byKey(const Key('settings_end_time_tile'))); // 20:00 -> 21:00
      await tester.ensureVisible(find.byKey(const Key('settings_voice_toggle')));

      await tester.tap(find.byKey(const Key('settings_voice_toggle'))); // Toggle voice
      await tester.ensureVisible(find.byKey(const Key('settings_sound_tile')));

      await tester.tap(find.byKey(const Key('settings_sound_tile'))); // Toggle sound

      // Save
      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));

      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();

      // Verify Dashboard reflects changes
      expect(find.byKey(const Key('dashboard_screen')), findsOneWidget);
      expect(find.text('0 / 7\nGlasses'), findsOneWidget);
    });

    testWidgets('3. Scenario: Hydration Goal Celebration, Dismissal, and Log Cleansing', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      
      // Onboard
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Arthur');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      // Log 8 times to reach target
      for (int i = 0; i < 8; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pump();
      }
      await tester.pumpAndSettle();

      // Celebration Overlay shows up
      expect(find.byKey(const Key('celebration_overlay')), findsOneWidget);
      await tester.ensureVisible(find.byKey(const Key('celebration_dismiss_button')));

      await tester.tap(find.byKey(const Key('celebration_dismiss_button')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('celebration_overlay')), findsNothing);

      // Cleanse logs via Settings
      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));

      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();
      await tester.ensureVisible(find.byKey(const Key('settings_clear_logs_button')));

      await tester.tap(find.byKey(const Key('settings_clear_logs_button')));
      await tester.pumpAndSettle();
      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));

      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();

      // Verify dashboard is 0
      expect(find.text('0 / 8\nGlasses'), findsOneWidget);

      // Verify history is empty
      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_history_button')));

      await tester.tap(find.byKey(const Key('dashboard_nav_history_button')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('history_empty_state_text')), findsOneWidget);
    });

    testWidgets('4. Scenario: Correction of Erroneous Entries via Snackbars and History Screen', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      
      // Onboard
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Arthur');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      // Log and Undo via snackbar
      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();
      expect(find.text('1 / 8\nGlasses'), findsOneWidget);

      await tester.ensureVisible(find.byKey(const Key('dashboard_undo_button')));


      await tester.tap(find.byKey(const Key('dashboard_undo_button')));
      await tester.pumpAndSettle();
      expect(find.text('0 / 8\nGlasses'), findsOneWidget);

      // Log 2 drinks, delete 1 from history
      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pump();
      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();
      expect(find.text('2 / 8\nGlasses'), findsOneWidget);

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_history_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_history_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('history_item_delete_button_0')));


      await tester.tap(find.byKey(const Key('history_item_delete_button_0')));
      await tester.pumpAndSettle();

      // Return to Dashboard and check progress
      await tester.ensureVisible(find.byKey(const Key('history_back_button')));

      await tester.tap(find.byKey(const Key('history_back_button')));
      await tester.pumpAndSettle();
      expect(find.text('1 / 8\nGlasses'), findsOneWidget);
    });

    testWidgets('5. Scenario: Full Day Simulation: Active Hours, Goal Re-calibration, and Reminders Snooze', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      
      // Onboard
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Grandpa Arthur');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      // Calibrate active hours
      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));

      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();
      await tester.ensureVisible(find.byKey(const Key('settings_start_time_tile')));

      await tester.tap(find.byKey(const Key('settings_start_time_tile'))); // 07:00
      await tester.ensureVisible(find.byKey(const Key('settings_end_time_tile')));

      await tester.tap(find.byKey(const Key('settings_end_time_tile'))); // 21:00
      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));

      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();

      // Snooze reminder twice
      await tester.ensureVisible(find.byKey(const Key('dashboard_snooze_button')));

      await tester.tap(find.byKey(const Key('dashboard_snooze_button')));
      await tester.pump();
      await tester.ensureVisible(find.byKey(const Key('dashboard_snooze_button')));

      await tester.tap(find.byKey(const Key('dashboard_snooze_button')));
      await tester.pumpAndSettle();
      expect(find.text('Reminder snoozed for 50 min'), findsOneWidget);

      // Log multiple drinks to reach goal
      for (int i = 0; i < 8; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pump();
      }
      await tester.pumpAndSettle();

      // Dismiss celebration and verify final state
      expect(find.byKey(const Key('celebration_overlay')), findsOneWidget);
      await tester.ensureVisible(find.byKey(const Key('celebration_dismiss_button')));

      await tester.tap(find.byKey(const Key('celebration_dismiss_button')));
      await tester.pumpAndSettle();
      expect(find.text('8 / 8\nGlasses'), findsOneWidget);
    });
  });
}
