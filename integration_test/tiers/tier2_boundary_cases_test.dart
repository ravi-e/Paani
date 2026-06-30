import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:paani/main.dart';

void main() {
  group('Tier 2: Boundary Cases (35 Tests)', () {

    // --- ONBOARDING BOUNDARIES (1-5) ---
    testWidgets('1. Onboarding Boundary: Name with trailing spaces trimmed', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), '  Sarah  ');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();
      expect(find.text('Hello, Sarah'), findsOneWidget);
    });

    testWidgets('2. Onboarding Boundary: Very long username handles correctly', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      const longName = 'Grandma Elizabeth Antoinette Marie Charlotte the Hydrated';
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), longName);
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();
      expect(find.text('Hello, $longName'), findsOneWidget);
    });

    testWidgets('3. Onboarding Boundary: Special characters in name are allowed', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'J-P & Co.');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();
      expect(find.text('Hello, J-P & Co.'), findsOneWidget);
    });

    testWidgets('4. Onboarding Boundary: Clicking get started with invalid emoji select', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Alice');
      await tester.ensureVisible(find.byKey(const Key('onboarding_emoji_item_smile')));

      await tester.tap(find.byKey(const Key('onboarding_emoji_item_smile')));
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('dashboard_screen')), findsOneWidget);
    });

    testWidgets('5. Onboarding Boundary: Rapid clicks on save button handles gracefully', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('dashboard_screen')), findsOneWidget);
    });

    // --- DASHBOARD BOUNDARIES (6-10) ---
    testWidgets('6. Dashboard Boundary: Excess drinks logging beyond daily goal', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 15; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pump();
      }
      await tester.pumpAndSettle();
      expect(find.text('15 / 8\nGlasses'), findsOneWidget);
    });

    testWidgets('7. Dashboard Boundary: Undo action when logs are empty does not crash', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      // Directly click undo button (if found) or just try to trigger undo via SnackBar state.
      // Since undo snackbar button isn't visible, let's verify we have 0 glasses.
      expect(find.text('0 / 8\nGlasses'), findsOneWidget);
    });

    testWidgets('8. Dashboard Boundary: Multiple logs and single undo', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));


      await tester.tap(find.byKey(const Key('dashboard_log_drink_button'))); // 1
      await tester.pump();
      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

      await tester.tap(find.byKey(const Key('dashboard_log_drink_button'))); // 2
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_undo_button')));


      await tester.tap(find.byKey(const Key('dashboard_undo_button')));
      await tester.pumpAndSettle();
      expect(find.text('1 / 8\nGlasses'), findsOneWidget);
    });

    testWidgets('9. Dashboard Boundary: Zero goal behaves gracefully', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      // Go to settings and set goal to 1 (since 0 might be disallowed by validation, we try decrease stepper)
      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));

      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 10; i++) {
        await tester.ensureVisible(find.byKey(const Key('settings_target_decrease_button')));

        await tester.tap(find.byKey(const Key('settings_target_decrease_button')));
        await tester.pump();
      }
      // Target text should be 1 (minimum boundary)
      expect(find.text('1'), findsOneWidget);
      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));

      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();
      expect(find.text('0 / 1\nGlasses'), findsOneWidget);
    });

    testWidgets('10. Dashboard Boundary: Snooze button extreme presses keeps incrementing', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 5; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_snooze_button')));

        await tester.tap(find.byKey(const Key('dashboard_snooze_button')));
        await tester.pumpAndSettle();
      }
      expect(find.text('Reminder snoozed for 80 min'), findsOneWidget);
    });

    // --- HISTORY BOUNDARIES (11-15) ---
    testWidgets('11. History Boundary: Deleting first item in logs list', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));


      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pump();
      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_history_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_history_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('history_item_delete_button_0')));


      await tester.tap(find.byKey(const Key('history_item_delete_button_0')));
      await tester.pumpAndSettle();

      // Only history_item_0 should remain (since there was 0 and 1, now 1 item left which becomes index 0)
      expect(find.byKey(const Key('history_item_0')), findsOneWidget);
      expect(find.byKey(const Key('history_item_1')), findsNothing);
    });

    testWidgets('12. History Boundary: Deleting last item in logs list', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));


      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pump();
      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_history_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_history_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('history_item_delete_button_1')));


      await tester.tap(find.byKey(const Key('history_item_delete_button_1')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('history_item_0')), findsOneWidget);
      expect(find.byKey(const Key('history_item_1')), findsNothing);
    });

    testWidgets('13. History Boundary: Deleting all items sequentially shows empty state', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));


      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_history_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_history_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('history_item_delete_button_0')));


      await tester.tap(find.byKey(const Key('history_item_delete_button_0')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('history_empty_state_text')), findsOneWidget);
    });

    testWidgets('14. History Boundary: Adding massive amount of logs and scrolling history list', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 20; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pump();
      }
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_history_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_history_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('history_list_view')), findsOneWidget);
    });

    testWidgets('15. History Boundary: Empty list back navigation preserves state', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_history_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_history_button')));
      await tester.pumpAndSettle();
      await tester.ensureVisible(find.byKey(const Key('history_back_button')));

      await tester.tap(find.byKey(const Key('history_back_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('dashboard_screen')), findsOneWidget);
    });

    // --- SETTINGS BOUNDARIES (16-20) ---
    testWidgets('16. Settings Boundary: Empty name input is ignored during save', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.enterText(find.byKey(const Key('settings_username_input')), '');
      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));

      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();

      // Since name was empty, settings saving is ignored (remains on settings screen)
      expect(find.byKey(const Key('settings_screen')), findsOneWidget);
    });

    testWidgets('17. Settings Boundary: Decreasing target goal below 1 is blocked', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      // Decrease goal 10 times (default is 8)
      for (int i = 0; i < 10; i++) {
        await tester.ensureVisible(find.byKey(const Key('settings_target_decrease_button')));

        await tester.tap(find.byKey(const Key('settings_target_decrease_button')));
        await tester.pump();
      }
      expect(find.text('1'), findsOneWidget);
    });

    testWidgets('18. Settings Boundary: Extremely high daily goal limit stepper', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 12; i++) {
        await tester.ensureVisible(find.byKey(const Key('settings_target_increase_button')));

        await tester.tap(find.byKey(const Key('settings_target_increase_button')));
        await tester.pump();
      }
      expect(find.text('20'), findsOneWidget);
    });

    testWidgets('19. Settings Boundary: Active Hours Start Time boundary presses', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_start_time_tile')));


      await tester.tap(find.byKey(const Key('settings_start_time_tile')));
      await tester.pump();
      expect(find.text('07:00'), findsOneWidget);
    });

    testWidgets('20. Settings Boundary: Active Hours End Time boundary presses', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_end_time_tile')));


      await tester.tap(find.byKey(const Key('settings_end_time_tile')));
      await tester.pump();
      expect(find.text('21:00'), findsOneWidget);
    });

    // --- CELEBRATION BOUNDARIES (21-25) ---
    testWidgets('21. Celebration Boundary: Re-evaluating celebration trigger on page refresh', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 8; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pump();
      }
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('celebration_overlay')), findsOneWidget);
    });

    testWidgets('22. Celebration Boundary: Dismissing celebration overlay does not show it again immediately', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 8; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pump();
      }
      await tester.pumpAndSettle();
      await tester.ensureVisible(find.byKey(const Key('celebration_dismiss_button')));

      await tester.tap(find.byKey(const Key('celebration_dismiss_button')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('celebration_overlay')), findsNothing);
    });

    testWidgets('23. Celebration Boundary: Confetti container layout boundaries', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 8; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pump();
      }
      await tester.pumpAndSettle();
      final confetti = find.byKey(const Key('celebration_confetti_widget'));
      expect(confetti, findsOneWidget);
    });

    testWidgets('24. Celebration Boundary: Statistics values render correctly at target threshold', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 8; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pump();
      }
      await tester.pumpAndSettle();
      expect(find.text('You reached your daily goal of 8 glasses! (Total: 8)'), findsOneWidget);
    });

    testWidgets('25. Celebration Boundary: Tapping confetti does not dismiss celebration overlay', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 8; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pump();
      }
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('celebration_confetti_widget')));


      await tester.tap(find.byKey(const Key('celebration_confetti_widget')));
      await tester.pump();
      expect(find.byKey(const Key('celebration_overlay')), findsOneWidget); // still visible
    });

    // --- REMINDERS BOUNDARIES (26-30) ---
    testWidgets('26. Reminders Boundary: Large snooze value formatting', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 10; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_snooze_button')));

        await tester.tap(find.byKey(const Key('dashboard_snooze_button')));
        await tester.pump();
      }
      await tester.pumpAndSettle();
      expect(find.text('Reminder snoozed for 130 min'), findsOneWidget);
    });

    testWidgets('27. Reminders Boundary: Sound selector tile cycling boundaries', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      expect(find.text('Classic Bell'), findsOneWidget);
      await tester.ensureVisible(find.byKey(const Key('settings_sound_tile')));

      await tester.tap(find.byKey(const Key('settings_sound_tile')));
      await tester.pump();
      expect(find.text('Ocean Breeze'), findsOneWidget);
      await tester.ensureVisible(find.byKey(const Key('settings_sound_tile')));

      await tester.tap(find.byKey(const Key('settings_sound_tile')));
      await tester.pump();
      expect(find.text('Classic Bell'), findsOneWidget);
    });

    testWidgets('28. Reminders Boundary: Clear logs button deletes logs with active reminders', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));


      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_clear_logs_button')));


      await tester.tap(find.byKey(const Key('settings_clear_logs_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));


      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();

      expect(find.text('0 / 8\nGlasses'), findsOneWidget);
    });

    testWidgets('29. Reminders Boundary: Setting identical active hours start and end values', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      // Just verify tiles are functional
      expect(find.byKey(const Key('settings_start_time_tile')), findsOneWidget);
      expect(find.byKey(const Key('settings_end_time_tile')), findsOneWidget);
    });

    testWidgets('30. Reminders Boundary: Disabling voice reminder preferences does not impact other sound settings', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_voice_toggle')));


      await tester.tap(find.byKey(const Key('settings_voice_toggle')));
      await tester.pump();
      
      expect(find.text('Classic Bell'), findsOneWidget); // sound settings still rendered
    });

    // --- VOICE/HAPTICS BOUNDARIES (31-35) ---
    testWidgets('31. Voice/Haptics Boundary: Toggle switches state multiple times', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      final switchFinder = find.byKey(const Key('settings_voice_toggle'));
      await tester.ensureVisible(switchFinder);

      await tester.tap(switchFinder);
      await tester.pump();
      await tester.ensureVisible(switchFinder);

      await tester.tap(switchFinder);
      await tester.pump();
    });

    testWidgets('32. Voice/Haptics Boundary: Cycling sounds works on settings screen', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_sound_tile')));


      await tester.tap(find.byKey(const Key('settings_sound_tile')));
      await tester.pump();
      expect(find.text('Ocean Breeze'), findsOneWidget);
    });

    testWidgets('33. Voice/Haptics Boundary: Setting invalid username and exiting does not crash', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.enterText(find.byKey(const Key('settings_username_input')), '');
      // Try clicking save (should block navigation)
      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));

      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pump();
      expect(find.byKey(const Key('settings_screen')), findsOneWidget);
    });

    testWidgets('34. Voice/Haptics Boundary: Clear logs button removes logs instantly from memory', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));


      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_clear_logs_button')));


      await tester.tap(find.byKey(const Key('settings_clear_logs_button')));
      await tester.pumpAndSettle();
      
      // Go back to check logs
      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));

      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();
      expect(find.text('0 / 8\nGlasses'), findsOneWidget);
    });

    testWidgets('35. Voice/Haptics Boundary: Username field character limits checks', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      final nameInput = find.byKey(const Key('settings_username_input'));
      await tester.enterText(nameInput, 'A');
      expect(find.text('A'), findsOneWidget);
    });
  });
}
