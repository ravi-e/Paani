import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:android_alarm_manager_plus/android_alarm_manager_plus.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:paani/data/database_helper.dart';
import 'package:paani/services/tts_service.dart';

// Top-level callback for AndroidAlarmManager
@pragma('vm:entry-point')
void alarmCallback() async {
  WidgetsFlutterBinding.ensureInitialized();
  final db = DatabaseHelper.instance;
  final settings = await db.getSettings();

  final username = settings['username'] as String;
  final voiceEnabled = (settings['voice_enabled'] as int) == 1;

  // Speak TTS reminder
  if (voiceEnabled && username.isNotEmpty) {
    final firstName = username.split(' ').first;
    await TtsService.speak("Time to drink some water, $firstName!");
  }

  // Show insistent notification
  await NotificationService.showAlarmNotification(username);

  // Reschedule the next regular reminder automatically
  final nextTime = NotificationService.calculateNextAlarmTimeFromSettings(settings);
  await AndroidAlarmManager.oneShotAt(
    nextTime,
    NotificationService.alarmId,
    alarmCallback,
    alarmClock: true,
    allowWhileIdle: true,
    exact: true,
    wakeup: true,
  );
}

// Top-level callback for background notification actions
@pragma('vm:entry-point')
void notificationTapBackground(NotificationResponse response) async {
  WidgetsFlutterBinding.ensureInitialized();
  final db = DatabaseHelper.instance;

  // Cancel the alarm notification
  final notificationsPlugin = FlutterLocalNotificationsPlugin();
  await notificationsPlugin.cancel(NotificationService.notificationId);

  // Stop any active TTS
  await TtsService.stop();

  if (response.actionId == 'drank_action') {
    // Record drink in database
    final nowStr = DateTime.now().toUtc().toIso8601String();
    await db.insertLog(nowStr);

    // Read settings to reschedule the next regular alarm
    final settings = await db.getSettings();
    final nextTime = NotificationService.calculateNextAlarmTimeFromSettings(settings);

    // Cancel old alarms and schedule new one
    await AndroidAlarmManager.cancel(NotificationService.alarmId);
    await AndroidAlarmManager.oneShotAt(
      nextTime,
      NotificationService.alarmId,
      alarmCallback,
      alarmClock: true,
      allowWhileIdle: true,
      exact: true,
      wakeup: true,
    );
  } else if (response.actionId == 'snooze_action') {
    // Snooze for 5 minutes
    final snoozeMinutes = 5;
    final nextTime = DateTime.now().add(Duration(minutes: snoozeMinutes));

    // Cancel old alarms and schedule snooze alarm
    await AndroidAlarmManager.cancel(NotificationService.alarmId);
    await AndroidAlarmManager.oneShotAt(
      nextTime,
      NotificationService.alarmId,
      alarmCallback,
      alarmClock: true,
      allowWhileIdle: true,
      exact: true,
      wakeup: true,
    );
  }
}

class NotificationService {
  static const int alarmId = 101;
  static const int notificationId = 2002;
  static final FlutterLocalNotificationsPlugin _notificationsPlugin =
      FlutterLocalNotificationsPlugin();

  static Future<void> initialize() async {
    if (DatabaseHelper.instance.isTestMode) {
      return;
    }
    // Initialize Local Notifications
    const AndroidInitializationSettings initializationSettingsAndroid =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    const InitializationSettings initializationSettings = InitializationSettings(
      android: initializationSettingsAndroid,
    );

    await _notificationsPlugin.initialize(
      initializationSettings,
      onDidReceiveNotificationResponse: (NotificationResponse response) async {
        // Handle action if app in foreground
        notificationTapBackground(response);
      },
      onDidReceiveBackgroundNotificationResponse: notificationTapBackground,
    );

    // Initialize Alarm Manager
    await AndroidAlarmManager.initialize();
  }

  static Future<void> scheduleNextReminder(dynamic settingsProvider) async {
    if (DatabaseHelper.instance.isTestMode) {
      return;
    }
    // Cancel any current alarm
    await AndroidAlarmManager.cancel(alarmId);

    // Get settings map or values
    final settings = await DatabaseHelper.instance.getSettings();
    if (settings['username'].toString().isEmpty) return;

    final nextTime = calculateNextAlarmTimeFromSettings(settings);

    await AndroidAlarmManager.oneShotAt(
      nextTime,
      alarmId,
      alarmCallback,
      alarmClock: true,
      allowWhileIdle: true,
      exact: true,
      wakeup: true,
    );
  }

  static Future<void> snoozeAlarm(int minutes) async {
    if (DatabaseHelper.instance.isTestMode) {
      return;
    }
    await AndroidAlarmManager.cancel(alarmId);
    final nextTime = DateTime.now().add(Duration(minutes: minutes));

    await AndroidAlarmManager.oneShotAt(
      nextTime,
      alarmId,
      alarmCallback,
      alarmClock: true,
      allowWhileIdle: true,
      exact: true,
      wakeup: true,
    );
  }

  static Future<void> cancelAlarm() async {
    if (DatabaseHelper.instance.isTestMode) {
      return;
    }
    await AndroidAlarmManager.cancel(alarmId);
    await _notificationsPlugin.cancel(notificationId);
    await TtsService.stop();
  }

  static Future<void> showAlarmNotification(String username) async {
    final firstName = username.split(' ').first;
    final title = username.isNotEmpty ? "Drink water, $firstName! 💧" : "Drink water! 💧";

    final androidDetails = AndroidNotificationDetails(
      'water_reminders_channel',
      'Water Reminders',
      channelDescription: 'Hydration reminders and looping alarms for seniors',
      importance: Importance.max,
      priority: Priority.high,
      ongoing: true,
      autoCancel: false,
      // 4 represents FLAG_INSISTENT, which loops the sound until dismissed
      additionalFlags: Int32List.fromList([4]),
      actions: const [
        AndroidNotificationAction(
          'drank_action',
          '✅ DRANK WATER NOW',
          showsUserInterface: false,
        ),
        AndroidNotificationAction(
          'snooze_action',
          '⏲️ SNOOZE 5 MINS',
          showsUserInterface: false,
        ),
      ],
    );

    final details = NotificationDetails(android: androidDetails);

    await _notificationsPlugin.show(
      notificationId,
      title,
      "It's time to drink a glass of water.",
      details,
    );
  }

  static DateTime calculateNextAlarmTimeFromSettings(Map<String, dynamic> settings) {
    final startStr = settings['start_time'] as String? ?? '08:00';
    final endStr = settings['end_time'] as String? ?? '20:00';

    final startParts = startStr.split(':');
    final endParts = endStr.split(':');

    final startHour = int.parse(startParts[0]);
    final startMin = int.parse(startParts[1]);

    final endHour = int.parse(endParts[0]);
    final endMin = int.parse(endParts[1]);

    final now = DateTime.now();

    final startTimeToday = DateTime(now.year, now.month, now.day, startHour, startMin);
    final endTimeToday = DateTime(now.year, now.month, now.day, endHour, endMin);

    DateTime nextTime;

    if (now.isAfter(endTimeToday)) {
      // Wrap to tomorrow's start time
      nextTime = startTimeToday.add(const Duration(days: 1));
    } else if (now.isBefore(startTimeToday)) {
      // Schedule for today's start time
      nextTime = startTimeToday;
    } else {
      // Within active hours. Add 60 minutes interval
      nextTime = now.add(const Duration(minutes: 60));
      if (nextTime.isAfter(endTimeToday)) {
        // If it goes past end time, wrap to tomorrow
        nextTime = startTimeToday.add(const Duration(days: 1));
      }
    }

    return nextTime;
  }
}
