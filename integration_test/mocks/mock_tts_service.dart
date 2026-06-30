class MockTtsService {
  String? lastSpokenText;

  Future<void> speak(String text) async {
    lastSpokenText = text;
  }
}
