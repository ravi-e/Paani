class MockDatabaseHelper {
  final List<Map<String, dynamic>> _mockDb = [];

  Future<void> insertDrink(String timestamp) async {
    _mockDb.add({'id': _mockDb.length, 'timestamp': timestamp});
  }

  Future<List<Map<String, dynamic>>> getDrinks() async {
    return List.from(_mockDb);
  }

  Future<void> deleteDrink(int id) async {
    _mockDb.removeWhere((item) => item['id'] == id);
  }

  Future<void> clearAll() async {
    _mockDb.clear();
  }
}
