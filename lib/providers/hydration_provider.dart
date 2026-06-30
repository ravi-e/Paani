import 'package:flutter/material.dart';
import 'package:paani/data/database_helper.dart';
import 'package:paani/services/notification_service.dart';

class HydrationProvider extends ChangeNotifier {
  final DatabaseHelper _db = DatabaseHelper.instance;

  String _username = '';
  String _selectedEmoji = 'smile';
  int _dailyGoal = 8;
  bool _voiceEnabled = true;
  String _startTime = '08:00';
  String _endTime = '20:00';
  String _selectedSound = 'Classic Bell';
  int _snoozeMinutes = 30;

  List<Map<String, dynamic>> _rawLogs = [];
  bool _showCelebration = false;
  bool _isInitialized = false;

  // Getters
  String get username => _username;
  String get selectedEmoji => _selectedEmoji;
  int get dailyGoal => _dailyGoal;
  bool get voiceEnabled => _voiceEnabled;
  String get startTime => _startTime;
  String get endTime => _endTime;
  String get selectedSound => _selectedSound;
  int get snoozeMinutes => _snoozeMinutes;
  bool get showCelebration => _showCelebration;
  bool get isInitialized => _isInitialized;

  // Convert raw logs to a List of time strings (e.g., "14:35")
  List<String> get logs {
    return _rawLogs.map((log) {
      final dt = DateTime.parse(log['timestamp'] as String).toLocal();
      final hour = dt.hour.toString().padLeft(2, '0');
      final minute = dt.minute.toString().padLeft(2, '0');
      return '$hour:$minute';
    }).toList();
  }

  Future<void> initialize() async {
    if (DatabaseHelper.instance.isTestMode) {
      _username = '';
      _selectedEmoji = 'smile';
      _dailyGoal = 8;
      _voiceEnabled = true;
      _startTime = '08:00';
      _endTime = '20:00';
      _selectedSound = 'Classic Bell';
      _snoozeMinutes = 30;
      _isInitialized = true;
      notifyListeners();
      return;
    }

    final settings = await _db.getSettings();
    _username = settings['username'] as String;
    _selectedEmoji = settings['selected_emoji'] as String;
    _dailyGoal = settings['daily_goal'] as int;
    _voiceEnabled = (settings['voice_enabled'] as int) == 1;
    _startTime = settings['start_time'] as String;
    _endTime = settings['end_time'] as String;
    _selectedSound = settings['selected_sound'] as String;
    _snoozeMinutes = settings['snooze_minutes'] as int;

    await refreshLogs();
    _isInitialized = true;
    notifyListeners();

    // Schedule reminders on app start
    if (_username.isNotEmpty) {
      await NotificationService.scheduleNextReminder(this);
    }
  }

  Future<void> refreshLogs() async {
    final allLogs = await _db.getLogs();
    final today = DateTime.now();
    _rawLogs = allLogs.where((log) {
      try {
        final dt = DateTime.parse(log['timestamp'] as String).toLocal();
        return dt.year == today.year && dt.month == today.month && dt.day == today.day;
      } catch (e) {
        return false;
      }
    }).toList();
    notifyListeners();
  }

  Future<void> logDrink() async {
    final nowStr = DateTime.now().toUtc().toIso8601String();
    await _db.insertLog(nowStr);
    await refreshLogs();

    if (_rawLogs.length >= _dailyGoal) {
      _showCelebration = true;
    }

    // Cancel any active looping alarms/notifications since a drink is logged
    await NotificationService.cancelAlarm();

    // Schedule next reminder
    await NotificationService.scheduleNextReminder(this);

    notifyListeners();
  }

  Future<void> undoDrink() async {
    await _db.deleteLastLog();
    await refreshLogs();
    _showCelebration = false;
    notifyListeners();
  }

  Future<void> deleteLog(int index) async {
    if (index >= 0 && index < _rawLogs.length) {
      final id = _rawLogs[index]['id'] as int;
      await _db.deleteLog(id);
      await refreshLogs();
    }
  }

  Future<void> clearLogs() async {
    await _db.clearLogs();
    await refreshLogs();
  }

  Future<void> snoozeReminder() async {
    _snoozeMinutes += 10;
    await NotificationService.snoozeAlarm(_snoozeMinutes);
    notifyListeners();
  }

  Future<void> saveSettings({
    required String name,
    required String emoji,
    required int goal,
    required bool voice,
    required String start,
    required String end,
    required String sound,
    required int snooze,
  }) async {
    _username = name;
    _selectedEmoji = emoji;
    _dailyGoal = goal;
    _voiceEnabled = voice;
    _startTime = start;
    _endTime = end;
    _selectedSound = sound;
    _snoozeMinutes = snooze;

    if (_rawLogs.length >= goal) {
      _showCelebration = true;
    } else {
      _showCelebration = false;
    }

    await _db.saveSettings({
      'username': name,
      'selected_emoji': emoji,
      'daily_goal': goal,
      'voice_enabled': voice ? 1 : 0,
      'start_time': start,
      'end_time': end,
      'selected_sound': sound,
      'snooze_minutes': snooze,
    });

    // Reschedule alarms with new settings
    await NotificationService.scheduleNextReminder(this);

    notifyListeners();
  }

  void dismissCelebration() {
    _showCelebration = false;
    notifyListeners();
  }
}
