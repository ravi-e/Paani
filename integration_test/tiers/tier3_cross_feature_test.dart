import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:paani/main.dart';

void main() {
  group('Tier 3: Cross Feature (7 Tests)', () {

    testWidgets('1. Cross-Feature: Onboarding name propagates correctly to Dashboard greeting', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Grandma Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('dashboard_screen')), findsOneWidget);
      expect(find.text('Hello, Grandma Sarah'), findsOneWidget);
    });

    testWidgets('2. Cross-Feature: Logging drink on Dashboard adds entry instantly in History', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Grandma Sarah');
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
    });

    testWidgets('3. Cross-Feature: Setting daily target goal updates Dashboard progress denominator', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('settings_target_increase_button')));


      await tester.tap(find.byKey(const Key('settings_target_increase_button'))); // 8 -> 9
      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));

      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();

      expect(find.text('0 / 9\nGlasses'), findsOneWidget);
    });

    testWidgets('4. Cross-Feature: Deleting logged item in History reverts Dashboard log count', (tester) async {
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

      await tester.ensureVisible(find.byKey(const Key('history_back_button')));


      await tester.tap(find.byKey(const Key('history_back_button')));
      await tester.pumpAndSettle();

      expect(find.text('0 / 8\nGlasses'), findsOneWidget);
    });

    testWidgets('5. Cross-Feature: Celebration triggers upon goal completion and goes away after Undo', (tester) async {
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

      await tester.ensureVisible(find.byKey(const Key('celebration_dismiss_button')));


      await tester.tap(find.byKey(const Key('celebration_dismiss_button')));
      await tester.pumpAndSettle();

      // Click undo snackbar (if still active)
      await tester.ensureVisible(find.byKey(const Key('dashboard_undo_button')));

      await tester.tap(find.byKey(const Key('dashboard_undo_button')));
      await tester.pumpAndSettle();

      expect(find.text('7 / 8\nGlasses'), findsOneWidget);
    });

    testWidgets('6. Cross-Feature: Lowering goal in settings below logged count triggers celebration', (tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Sarah');
      await tester.ensureVisible(find.byKey(const Key('onboarding_save_button')));

      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));


      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));

      await tester.tap(find.byKey(const Key('dashboard_log_drink_button'))); // 2 logged
      await tester.pumpAndSettle();

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_settings_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      // Lower goal to 2
      for (int i = 0; i < 6; i++) {
        await tester.ensureVisible(find.byKey(const Key('settings_target_decrease_button')));

        await tester.tap(find.byKey(const Key('settings_target_decrease_button')));
        await tester.pump();
      }
      expect(find.text('2'), findsOneWidget);

      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));


      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();

      // Goal is 2, logged count is 2 -> should trigger celebration immediately
      expect(find.byKey(const Key('celebration_overlay')), findsOneWidget);
    });

    testWidgets('7. Cross-Feature: Clearing logs in settings completely empties History screen', (tester) async {
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

      await tester.ensureVisible(find.byKey(const Key('dashboard_nav_history_button')));


      await tester.tap(find.byKey(const Key('dashboard_nav_history_button')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('history_empty_state_text')), findsOneWidget);
    });
  });
}
