import 'package:flutter_tts/flutter_tts.dart';
import 'package:paani/data/database_helper.dart';

class TtsService {
  static final FlutterTts _tts = FlutterTts();

  static Future<void> speak(String text) async {
    if (DatabaseHelper.instance.isTestMode) {
      return;
    }
    try {
      await _tts.setLanguage("en-US");
      await _tts.setPitch(1.0);
      await _tts.setSpeechRate(0.5); // Slightly slower for elderly users
      await _tts.speak(text);
    } catch (e) {
      print("TTS Error: $e");
    }
  }

  static Future<void> stop() async {
    if (DatabaseHelper.instance.isTestMode) {
      return;
    }
    try {
      await _tts.stop();
    } catch (e) {
      print("TTS Stop Error: $e");
    }
  }
}
