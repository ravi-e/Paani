import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:paani/main.dart';

void main() {
  group('Design System & Theme Verification Tests', () {
    
    testWidgets('Verify Onboarding Screen Design Metrics', (WidgetTester tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.pumpAndSettle();

      // 1. Check Onboarding Screen Title Typography
      final titleFinder = find.byKey(const Key('onboarding_title'));
      expect(titleFinder, findsOneWidget);
      final Text titleWidget = tester.widget<Text>(titleFinder);
      expect(titleWidget.style, isNotNull);
      
      final BuildContext context = tester.element(titleFinder);
      final TextStyle resolvedTitleStyle = Theme.of(context).textTheme.headlineMedium!;
      expect(resolvedTitleStyle.fontSize, equals(28.0));
      expect(resolvedTitleStyle.fontWeight, equals(FontWeight.w700));

      // 2. Check Onboarding Save Button Height (Should be >= 64dp)
      final saveButtonFinder = find.byKey(const Key('onboarding_save_button'));
      expect(saveButtonFinder, findsOneWidget);
      final Size saveButtonSize = tester.getSize(saveButtonFinder);
      debugPrint('Onboarding Save Button size: $saveButtonSize');
      expect(saveButtonSize.height, greaterThanOrEqualTo(64.0));

      // 3. Check Onboarding Emoji Touch Target Size (Should be >= 56x56dp)
      final emojiFinder = find.byKey(const Key('onboarding_emoji_item_smile'));
      expect(emojiFinder, findsOneWidget);
      final Size emojiSize = tester.getSize(emojiFinder);
      debugPrint('Emoji Item Smile size: $emojiSize');
      expect(emojiSize.width, greaterThanOrEqualTo(56.0), reason: 'Emoji item width is less than 56dp');
      expect(emojiSize.height, greaterThanOrEqualTo(56.0), reason: 'Emoji item height is less than 56dp');
    });

    testWidgets('Verify Dashboard Screen Design Metrics', (WidgetTester tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.pumpAndSettle();

      // Navigate to Dashboard
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();

      // 1. Verify Greeting Typography
      final greetingFinder = find.byKey(const Key('dashboard_greeting_text'));
      expect(greetingFinder, findsOneWidget);
      final BuildContext context = tester.element(greetingFinder);
      final TextStyle greetingStyle = Theme.of(context).textTheme.headlineMedium!;
      expect(greetingStyle.fontSize, equals(28.0));
      expect(greetingStyle.fontWeight, equals(FontWeight.w700));

      // 2. Verify Log Drink Button Height (Should be >= 64dp)
      final logButtonFinder = find.byKey(const Key('dashboard_log_drink_button'));
      expect(logButtonFinder, findsOneWidget);
      final Size logButtonSize = tester.getSize(logButtonFinder);
      debugPrint('Log Drink Button size: $logButtonSize');
      expect(logButtonSize.height, greaterThanOrEqualTo(64.0));

      // 3. Verify Snooze Button Height (Should be >= 64dp)
      final snoozeButtonFinder = find.byKey(const Key('dashboard_snooze_button'));
      expect(snoozeButtonFinder, findsOneWidget);
      final Size snoozeButtonSize = tester.getSize(snoozeButtonFinder);
      debugPrint('Snooze Button size: $snoozeButtonSize');
      expect(snoozeButtonSize.height, greaterThanOrEqualTo(64.0));

      // 4. Verify Nav Settings Touch Target Size (Should be >= 56x56dp)
      final settingsNavFinder = find.byKey(const Key('dashboard_nav_settings_button'));
      expect(settingsNavFinder, findsOneWidget);
      final Size settingsNavSize = tester.getSize(settingsNavFinder);
      debugPrint('Nav Settings Button size: $settingsNavSize');
      expect(settingsNavSize.width, greaterThanOrEqualTo(56.0));
      expect(settingsNavSize.height, greaterThanOrEqualTo(56.0));

      // 5. Verify Nav History Touch Target Size (Should be >= 56x56dp)
      final historyNavFinder = find.byKey(const Key('dashboard_nav_history_button'));
      expect(historyNavFinder, findsOneWidget);
      final Size historyNavSize = tester.getSize(historyNavFinder);
      debugPrint('Nav History Button size: $historyNavSize');
      expect(historyNavSize.width, greaterThanOrEqualTo(56.0));
      expect(historyNavSize.height, greaterThanOrEqualTo(56.0));
    });

    testWidgets('Verify Settings Screen Design Metrics', (WidgetTester tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.pumpAndSettle();

      // Navigate to Settings
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      // 1. Verify Target Stepper Buttons Size (Should be >= 56x56dp)
      final decreaseFinder = find.byKey(const Key('settings_target_decrease_button'));
      final increaseFinder = find.byKey(const Key('settings_target_increase_button'));
      expect(decreaseFinder, findsOneWidget);
      expect(increaseFinder, findsOneWidget);

      final Size decreaseSize = tester.getSize(decreaseFinder);
      final Size increaseSize = tester.getSize(increaseFinder);
      debugPrint('Settings Decrease Button size: $decreaseSize');
      debugPrint('Settings Increase Button size: $increaseSize');
      expect(decreaseSize.width, greaterThanOrEqualTo(56.0));
      expect(decreaseSize.height, greaterThanOrEqualTo(56.0));
      expect(increaseSize.width, greaterThanOrEqualTo(56.0));
      expect(increaseSize.height, greaterThanOrEqualTo(56.0));

      // 2. Verify Settings Save Button Height (Should be >= 64dp)
      final settingsSaveFinder = find.byKey(const Key('settings_save_button'));
      expect(settingsSaveFinder, findsOneWidget);
      final Size settingsSaveSize = tester.getSize(settingsSaveFinder);
      debugPrint('Settings Save Button size: $settingsSaveSize');
      expect(settingsSaveSize.height, greaterThanOrEqualTo(64.0));

      // 3. Verify Settings Clear Logs Button Height (Should be >= 64dp)
      final settingsClearFinder = find.byKey(const Key('settings_clear_logs_button'));
      expect(settingsClearFinder, findsOneWidget);
      final Size settingsClearSize = tester.getSize(settingsClearFinder);
      debugPrint('Settings Clear Button size: $settingsClearSize');
      expect(settingsClearSize.height, greaterThanOrEqualTo(64.0));
    });

    testWidgets('Verify Celebration Screen Design Metrics', (WidgetTester tester) async {
      await tester.pumpWidget(const PaaniApp());
      await tester.pumpAndSettle();

      // Navigate to Settings, change goal to 1, then go back to dashboard and log a drink to trigger celebration overlay
      await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Bob');
      await tester.tap(find.byKey(const Key('onboarding_save_button')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('dashboard_nav_settings_button')));
      await tester.pumpAndSettle();

      // Stepper decrease to 1
      for (int i = 0; i < 7; i++) {
        await tester.tap(find.byKey(const Key('settings_target_decrease_button')));
        await tester.pumpAndSettle();
      }
      expect(find.text('1'), findsOneWidget);

      await tester.ensureVisible(find.byKey(const Key('settings_save_button')));
      await tester.tap(find.byKey(const Key('settings_save_button')));
      await tester.pumpAndSettle();

      // Log drink
      await tester.ensureVisible(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.tap(find.byKey(const Key('dashboard_log_drink_button')));
      await tester.pumpAndSettle();

      // Celebration overlay should render
      expect(find.byKey(const Key('celebration_overlay')), findsOneWidget);

      // Verify Typography (Should match headlineMedium: 28pt)
      final celebrationTitleFinder = find.byKey(const Key('celebration_title_text'));
      expect(celebrationTitleFinder, findsOneWidget);
      final Text titleText = tester.widget<Text>(celebrationTitleFinder);
      expect(titleText.style?.fontSize, equals(28.0));
      expect(titleText.style?.fontWeight, equals(FontWeight.bold));

      // Verify Stats Typography (Should match bodyMedium: 18pt)
      final statsFinder = find.byKey(const Key('celebration_stats_text'));
      expect(statsFinder, findsOneWidget);
      final Text statsText = tester.widget<Text>(statsFinder);
      expect(statsText.style?.fontSize, equals(18.0));

      // Verify Dismiss Button Height (Should be >= 64dp)
      final dismissButtonFinder = find.byKey(const Key('celebration_dismiss_button'));
      expect(dismissButtonFinder, findsOneWidget);
      final Size dismissButtonSize = tester.getSize(dismissButtonFinder);
      debugPrint('Celebration Dismiss Button size: $dismissButtonSize');
      expect(dismissButtonSize.height, greaterThanOrEqualTo(64.0));
    });
  });
}
