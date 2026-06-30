import 'dart:async';
import 'dart:io';
import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';

class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._init();
  static Database? _database;

  // In-memory fallback for widget tests to bypass native sqflite plugin
  bool get _isTestMode => Platform.environment.containsKey('FLUTTER_TEST') || isTestModeOverride;
  static bool isTestModeOverride = false;
  bool get isTestMode => _isTestMode;
  
  Map<String, dynamic> _testSettings = {
    'id': 1,
    'username': '',
    'selected_emoji': 'smile',
    'daily_goal': 8,
    'voice_enabled': 1,
    'start_time': '08:00',
    'end_time': '20:00',
    'selected_sound': 'Classic Bell',
    'snooze_minutes': 30,
  };

  final List<Map<String, dynamic>> _testLogs = [];
  int _testLogIdCounter = 1;

  DatabaseHelper._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('paani_hydration.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);
    return await openDatabase(
      path,
      version: 1,
      onCreate: _createDB,
    );
  }

  Future _createDB(Database db, int version) async {
    await db.execute('''
      CREATE TABLE reminder_settings (
        id INTEGER PRIMARY KEY,
        username TEXT NOT NULL,
        selected_emoji TEXT NOT NULL,
        daily_goal INTEGER NOT NULL,
        voice_enabled INTEGER NOT NULL,
        start_time TEXT NOT NULL,
        end_time TEXT NOT NULL,
        selected_sound TEXT NOT NULL,
        snooze_minutes INTEGER NOT NULL
      )
    ''');

    await db.execute('''
      CREATE TABLE drink_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp TEXT NOT NULL
      )
    ''');

    await db.insert('reminder_settings', {
      'id': 1,
      'username': '',
      'selected_emoji': 'smile',
      'daily_goal': 8,
      'voice_enabled': 1,
      'start_time': '08:00',
      'end_time': '20:00',
      'selected_sound': 'Classic Bell',
      'snooze_minutes': 30,
    });
  }

  // --- Settings Methods ---
  Future<Map<String, dynamic>> getSettings() async {
    if (_isTestMode) {
      return _testSettings;
    }
    final db = await instance.database;
    final maps = await db.query(
      'reminder_settings',
      where: 'id = ?',
      whereArgs: [1],
    );
    if (maps.isNotEmpty) {
      return maps.first;
    } else {
      return _testSettings;
    }
  }

  Future<int> saveSettings(Map<String, dynamic> settings) async {
    if (_isTestMode) {
      // Merge new settings with existing test settings
      _testSettings = {
        ..._testSettings,
        ...settings,
      };
      return 1;
    }
    final db = await instance.database;
    return await db.update(
      'reminder_settings',
      settings,
      where: 'id = ?',
      whereArgs: [1],
    );
  }

  // --- Drink Logs Methods ---
  Future<int> insertLog(String timestamp) async {
    if (_isTestMode) {
      final id = _testLogIdCounter++;
      _testLogs.add({
        'id': id,
        'timestamp': timestamp,
      });
      return id;
    }
    final db = await instance.database;
    return await db.insert('drink_logs', {
      'timestamp': timestamp,
    });
  }

  Future<List<Map<String, dynamic>>> getLogs() async {
    if (_isTestMode) {
      return List.from(_testLogs);
    }
    final db = await instance.database;
    return await db.query('drink_logs', orderBy: 'id ASC');
  }

  Future<int> deleteLastLog() async {
    if (_isTestMode) {
      if (_testLogs.isNotEmpty) {
        _testLogs.removeLast();
        return 1;
      }
      return 0;
    }
    final db = await instance.database;
    final logs = await db.query('drink_logs', orderBy: 'id DESC', limit: 1);
    if (logs.isNotEmpty) {
      final id = logs.first['id'] as int;
      return await db.delete(
        'drink_logs',
        where: 'id = ?',
        whereArgs: [id],
      );
    }
    return 0;
  }

  Future<int> deleteLog(int id) async {
    if (_isTestMode) {
      final beforeLength = _testLogs.length;
      _testLogs.removeWhere((log) => log['id'] == id);
      return beforeLength - _testLogs.length;
    }
    final db = await instance.database;
    return await db.delete(
      'drink_logs',
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Future<int> clearLogs() async {
    if (_isTestMode) {
      final count = _testLogs.length;
      _testLogs.clear();
      return count;
    }
    final db = await instance.database;
    return await db.delete('drink_logs');
  }

  Future<void> close() async {
    if (_isTestMode) return;
    final db = await instance.database;
    db.close();
  }

  Future<void> resetDatabase() async {
    _testSettings = {
      'id': 1,
      'username': '',
      'selected_emoji': 'smile',
      'daily_goal': 8,
      'voice_enabled': 1,
      'start_time': '08:00',
      'end_time': '20:00',
      'selected_sound': 'Classic Bell',
      'snooze_minutes': 30,
    };
    _testLogs.clear();
    _testLogIdCounter = 1;

    final db = await database;
    await db.delete('drink_logs');
    await db.update(
      'reminder_settings',
      {
        'username': '',
        'selected_emoji': 'smile',
        'daily_goal': 8,
        'voice_enabled': 1,
        'start_time': '08:00',
        'end_time': '20:00',
        'selected_sound': 'Classic Bell',
        'snooze_minutes': 30,
      },
      where: 'id = ?',
      whereArgs: [1],
    );
  }
}
