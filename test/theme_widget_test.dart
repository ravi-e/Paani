import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:paani/core/theme.dart';
import 'package:paani/main.dart';

void main() {
  testWidgets('Theme Button Heights and Touch Target Sizes', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const PaaniApp());

    // We start on the onboarding screen.
    // Verify Onboarding save button height is >= 64dp
    final saveButtonFinder = find.byKey(const Key('onboarding_save_button'));
    expect(saveButtonFinder, findsOneWidget);
    final Size saveButtonSize = tester.getSize(saveButtonFinder);
    print('Onboarding save button size: $saveButtonSize');
    expect(saveButtonSize.height, greaterThanOrEqualTo(64.0));

    // Verify Onboarding emoji items touch target size is >= 56x56dp
    final smileEmojiFinder = find.byKey(const Key('onboarding_emoji_item_smile'));
    expect(smileEmojiFinder, findsOneWidget);
    final Size smileEmojiSize = tester.getSize(smileEmojiFinder);
    print('Smile emoji item size: $smileEmojiSize');
    expect(smileEmojiSize.width, greaterThanOrEqualTo(56.0));
    expect(smileEmojiSize.height, greaterThanOrEqualTo(56.0));

    // On the onboarding screen, enter name
    await tester.enterText(find.byKey(const Key('onboarding_name_input')), 'Test User');
    await tester.pumpAndSettle();
    
    // Tap the save button to go to dashboard
    await tester.tap(find.byKey(const Key('onboarding_save_button')));
    await tester.pumpAndSettle();

    // Now we should be on the dashboard screen
    // Verify Dashboard Log Drink button height is >= 64dp
    final logDrinkButtonFinder = find.byKey(const Key('dashboard_log_drink_button'));
    expect(logDrinkButtonFinder, findsOneWidget);
    final Size logDrinkButtonSize = tester.getSize(logDrinkButtonFinder);
    print('Dashboard log drink button size: $logDrinkButtonSize');
    expect(logDrinkButtonSize.height, greaterThanOrEqualTo(64.0));

    // Verify Snooze button height is >= 64dp
    final snoozeButtonFinder = find.byKey(const Key('dashboard_snooze_button'));
    expect(snoozeButtonFinder, findsOneWidget);
    final Size snoozeButtonSize = tester.getSize(snoozeButtonFinder);
    print('Dashboard snooze button size: $snoozeButtonSize');
    expect(snoozeButtonSize.height, greaterThanOrEqualTo(64.0));

    // Verify dashboard nav buttons (settings, history) are >= 56x56dp
    final navSettingsFinder = find.byKey(const Key('dashboard_nav_settings_button'));
    expect(navSettingsFinder, findsOneWidget);
    final Size navSettingsSize = tester.getSize(navSettingsFinder);
    print('Dashboard nav settings button size: $navSettingsSize');
    expect(navSettingsSize.width, greaterThanOrEqualTo(56.0));
    expect(navSettingsSize.height, greaterThanOrEqualTo(56.0));

    // Navigate to settings
    await tester.tap(navSettingsFinder);
    await tester.pumpAndSettle();

    // Now we are on settings screen
    // Verify decrease and increase target buttons are >= 56x56dp
    final decreaseBtnFinder = find.byKey(const Key('settings_target_decrease_button'));
    expect(decreaseBtnFinder, findsOneWidget);
    final Size decreaseBtnSize = tester.getSize(decreaseBtnFinder);
    print('Settings decrease button size: $decreaseBtnSize');
    expect(decreaseBtnSize.width, greaterThanOrEqualTo(56.0));
    expect(decreaseBtnSize.height, greaterThanOrEqualTo(56.0));

    final increaseBtnFinder = find.byKey(const Key('settings_target_increase_button'));
    expect(increaseBtnFinder, findsOneWidget);
    final Size increaseBtnSize = tester.getSize(increaseBtnFinder);
    print('Settings increase button size: $increaseBtnSize');
    expect(increaseBtnSize.width, greaterThanOrEqualTo(56.0));
    expect(increaseBtnSize.height, greaterThanOrEqualTo(56.0));

    // Verify save settings button is >= 64dp
    final saveSettingsFinder = find.byKey(const Key('settings_save_button'));
    expect(saveSettingsFinder, findsOneWidget);
    final Size saveSettingsSize = tester.getSize(saveSettingsFinder);
    print('Settings save button size: $saveSettingsSize');
    expect(saveSettingsSize.height, greaterThanOrEqualTo(64.0));
  });

  testWidgets('Typography Style Resolution', (WidgetTester tester) async {
    final theme = PaaniTheme.lightTheme;
    final textTheme = theme.textTheme;

    // Verify Atkinson Hyperlegible Next is utilized (loaded as AtkinsonHyperlegible or similar font family)
    expect(textTheme.headlineLarge?.fontFamily, contains('AtkinsonHyperlegible'));
    expect(textTheme.headlineMedium?.fontFamily, contains('AtkinsonHyperlegible'));
    expect(textTheme.bodyLarge?.fontFamily, contains('AtkinsonHyperlegible'));
    expect(textTheme.bodyMedium?.fontFamily, contains('AtkinsonHyperlegible'));
    expect(textTheme.labelLarge?.fontFamily, contains('AtkinsonHyperlegible'));
    expect(textTheme.labelMedium?.fontFamily, contains('AtkinsonHyperlegible'));

    // Check specific style values
    expect(textTheme.headlineLarge?.fontSize, 32.0);
    expect(textTheme.headlineLarge?.fontWeight, FontWeight.w800);

    expect(textTheme.headlineMedium?.fontSize, 28.0);
    expect(textTheme.headlineMedium?.fontWeight, FontWeight.w700);

    expect(textTheme.bodyLarge?.fontSize, 22.0);
    expect(textTheme.bodyLarge?.fontWeight, FontWeight.w400);

    expect(textTheme.bodyMedium?.fontSize, 18.0);
    expect(textTheme.bodyMedium?.fontWeight, FontWeight.w400);

    expect(textTheme.labelLarge?.fontSize, 20.0);
    expect(textTheme.labelLarge?.fontWeight, FontWeight.w700);

    expect(textTheme.labelMedium?.fontSize, 16.0);
    expect(textTheme.labelMedium?.fontWeight, FontWeight.w700);
  });
}
