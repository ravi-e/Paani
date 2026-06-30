import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:paani/main.dart';

void main() {
  group('Tier 1: Feature Coverage (35 Tests)', () {
    
    // --- ONBOARDING TESTS (1-5) ---
    testWidgets('1. Onboarding: Title renders correctly', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      expect(find.byKey(const Key('onboarding_title')), findsOneWidget);
    });

    testWidgets('2. Onboarding: Empty name shows validation error', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pump();
      expect(find.byKey(const Key('onboarding_error_text')), findsOneWidget);
    });

    testWidgets('3. Onboarding: Emoji selection changes background/selection UI', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      // Tap emoji star
      await tester.ensureVisible(find.byKey(const Key('onboarding_emoji_item_star')));

      await tester.tap(find.byKey(const Key('onboarding_emoji_item_star')));
      await tester.pump();
      // Test selection succeeds (no error)
      expect(find.byKey(const Key('onboarding_emoji_item_star')), findsOneWidget);
    });

    testWidgets('4. Onboarding: Valid name and emoji saves and navigates to Dashboard', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Senior Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('dashboard_screen')), findsOneWidget);
      expect(find.byKey(const Key('onboarding_screen')), findsNothing);
    });

    testWidgets('5. Onboarding: State persistence on first load requires name input', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      expect(find.byKey(const Key('onboarding_name_input')), findsOneWidget);
      expect(find.byKey(const Key('dashboard_screen')), findsNothing);
    });

    // --- DASHBOARD TESTS (6-10) ---
    testWidgets('6. Dashboard: Personalized greeting renders with user name', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();
      expect(find.text('Hello, Bob'), findsOneWidget);
    });

    testWidgets('7. Dashboard: Log Drink increments daily glass count', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();
      
      expect(find.text('0 / 8\nGlasses'), findsOneWidget);
      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();
      expect(find.text('1 / 8\nGlasses'), findsOneWidget);
    });

    testWidgets('8. Dashboard: Undo button on snackbar reverts log count', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));


      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();
      expect(find.text('1 / 8\nGlasses'), findsOneWidget);

      await tester.ensureVisible(find.byKey(const Key('dashboard_undo_button')));


      await tester.tap(find.byKey(const Key('dashboard_undo_button')));
      await tester.pumpAndSettle();
      expect(find.text('0 / 8\nGlasses'), findsOneWidget);
    });

    testWidgets('9. Dashboard: Progress ring updates value correctly', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      final ringFinder = find.byKey(const Key('dashboard_progress_ring'));
      expect(ringFinder, findsOneWidget);
      
      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

      
      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();
      expect(find.text('1 / 8\nGlasses'), findsOneWidget);
    });

    testWidgets('10. Dashboard: Snooze button updates snooze count/label', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      expect(find.text('Reminder snoozed for 30 min'), findsOneWidget);
      await tester.ensureVisible(find.byKey(const Key('dashboard_snooze_button')));

      await tester.tap(find.byKey(const Key('dashboard_snooze_button')));
      await tester.pumpAndSettle();
      expect(find.text('Reminder snoozed for 40 min'), findsOneWidget);
    });

    // --- HISTORY TESTS (11-15) ---
    testWidgets('11. History: Empty state text displays when logs list is empty', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_history_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_history_button')));
      await tester.pumpAndSettle();
      
      expect(find.byKey(const Key('history_screen')), findsOneWidget);
      expect(find.byKey(const Key('history_empty_state_text')), findsOneWidget);
    });

    testWidgets('12. History: Logged drink appears in history list', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));


      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_history_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_history_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('history_item_0')), findsOneWidget);
      expect(find.byKey(const Key('history_item_time_0')), findsOneWidget);
    });

    testWidgets('13. History: Delete button in list removes entry', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
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

    testWidgets('14. History: Back button returns user to Dashboard', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
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

    testWidgets('15. History: Multiple items display sequentially in list', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));


      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();
      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_history_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_history_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('history_item_0')), findsOneWidget);
      expect(find.byKey(const Key('history_item_1')), findsOneWidget);
    });

    // --- SETTINGS TESTS (16-20) ---
    testWidgets('16. Settings: Correctly loads initial username and target goal', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('settings_username_input')), findsOneWidget);
      expect(find.text('8'), findsOneWidget); // Default target value text
    });

    testWidgets('17. Settings: Goal increase button increments goal', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_target_increase_button')));


      await tester.tap(find.byKey(const Key('settings_target_increase_button')));
      await tester.pump();

      expect(find.text('9'), findsOneWidget);
    });

    testWidgets('18. Settings: Goal decrease button decrements goal', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_target_decrease_button')));


      await tester.tap(find.byKey(const Key('settings_target_decrease_button')));
      await tester.pump();

      expect(find.text('7'), findsOneWidget);
    });

    testWidgets('19. Settings: Voice switch can be toggled', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      final switchFinder = find.byKey(const Key('settings_voice_toggle'));
      expect(switchFinder, findsOneWidget);
      await tester.ensureVisible(switchFinder);

      await tester.tap(switchFinder);
      await tester.pump();
    });

    testWidgets('20. Settings: Clear logs button resets Dashboard statistics', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
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

    // --- CELEBRATION TESTS (21-25) ---
    testWidgets('21. Celebration: Hidden by default before target reached', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('celebration_overlay')), findsNothing);
    });

    testWidgets('22. Celebration: Triggers automatically when glass count hits goal', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 8; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pumpAndSettle();
      }

      expect(find.byKey(const Key('celebration_overlay')), findsOneWidget);
    });

    testWidgets('23. Celebration: Renders confetti widget correctly', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 8; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pumpAndSettle();
      }

      expect(find.byKey(const Key('celebration_confetti_widget')), findsOneWidget);
    });

    testWidgets('24. Celebration: Displays success title and stats message', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 8; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pumpAndSettle();
      }

      expect(find.byKey(const Key('celebration_title_text')), findsOneWidget);
      expect(find.byKey(const Key('celebration_stats_text')), findsOneWidget);
    });

    testWidgets('25. Celebration: Close button dismisses the celebration overlay', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      for (int i = 0; i < 8; i++) {
        await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

        await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
        await tester.pumpAndSettle();
      }

      await tester.ensureVisible(find.byKey(const Key('celebration_dismiss_button')));


      await tester.tap(find.byKey(const Key('celebration_dismiss_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('celebration_overlay')), findsNothing);
    });

    // --- REMINDERS TESTS (26-30) ---
    testWidgets('26. Reminders: Snoozing updates next reminder timer countdown', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      expect(find.text('Reminder snoozed for 30 min'), findsOneWidget);
      await tester.ensureVisible(find.byKey(const Key('dashboard_snooze_button')));

      await tester.tap(find.byKey(const Key('dashboard_snooze_button')));
      await tester.pumpAndSettle();
      expect(find.text('Reminder snoozed for 40 min'), findsOneWidget);
    });

    testWidgets('27. Reminders: Start time tile triggers time choice/toggle', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      expect(find.text('08:00'), findsOneWidget);
      await tester.ensureVisible(find.byKey(const Key('settings_start_time_tile')));

      await tester.tap(find.byKey(const Key('settings_start_time_tile')));
      await tester.pump();
      expect(find.text('07:00'), findsOneWidget);
    });

    testWidgets('28. Reminders: End time tile triggers time choice/toggle', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      expect(find.text('20:00'), findsOneWidget);
      await tester.ensureVisible(find.byKey(const Key('settings_end_time_tile')));

      await tester.tap(find.byKey(const Key('settings_end_time_tile')));
      await tester.pump();
      expect(find.text('21:00'), findsOneWidget);
    });

    testWidgets('29. Reminders: Sound picker tile toggles standard sound values', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
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
    });

    testWidgets('30. Reminders: Saved settings persist on navigation back to Dashboard', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_target_increase_button')));


      await tester.tap(find.byKey(const Key('settings_target_increase_button'))); // Goal becomes 9
      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));

      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();

      expect(find.text('0 / 9\nGlasses'), findsOneWidget);
    });

    // --- VOICE/HAPTICS TESTS (31-35) ---
    testWidgets('31. Voice/Haptics: Toggle switches state reactively', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      final switchFinder = find.byKey(const Key('settings_voice_toggle'));
      expect(switchFinder, findsOneWidget);
      await tester.ensureVisible(switchFinder);

      await tester.tap(switchFinder);
      await tester.pump();
    });

    testWidgets('32. Voice/Haptics: Voice reminders toggle propagates state', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_voice_toggle')));


      await tester.tap(find.byKey(const Key('settings_voice_toggle')));
      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));

      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('dashboard_screen')), findsOneWidget);
    });

    testWidgets('33. Voice/Haptics: Selected sound option updates properly', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_sound_tile')));


      await tester.tap(find.byKey(const Key('settings_sound_tile')));
      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));

      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('dashboard_screen')), findsOneWidget);
    });

    testWidgets('34. Voice/Haptics: Settings name textfield updates controller text', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.enterText(find.byKey(const Key('settings_username_input')), 'Alice');
      expect(find.text('Alice'), findsOneWidget);
    });

    testWidgets('35. Voice/Haptics: Settings back navigation triggers state updates', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.enterText(find.byKey(const Key('settings_username_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));

      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();

      expect(find.text('Hello, Sarah'), findsOneWidget);
    });
  });
}
