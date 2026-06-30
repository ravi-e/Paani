class MockHapticService {
  int vibrateCount = 0;

  Future<void> triggerVibration() async {
    vibrateCount++;
  }
}
