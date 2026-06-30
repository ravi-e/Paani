import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:paani/data/database_helper.dart';
import 'tiers/tier1_feature_coverage_test.dart' as tier1;
import 'tiers/tier2_boundary_cases_test.dart' as tier2;
import 'tiers/tier3_cross_feature_test.dart' as tier3;
import 'tiers/tier4_real_world_scenarios_test.dart' as tier4;

void main() {
  final IntegrationTestWidgetsFlutterBinding binding =
      IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  
  binding.framePolicy = LiveTestWidgetsFlutterBindingFramePolicy.fullyLive;

  DatabaseHelper.isTestModeOverride = true;

  group('Paani E2E Suite', () {
    setUp(() async {
      await DatabaseHelper.instance.resetDatabase();
      try {
        final view = binding.platformDispatcher.views.first;
        view.physicalSize = const Size(1080, 2400);
        view.devicePixelRatio = 1.0;
      } catch (e) {
        // Fallback or ignore if not supported
      }
    });

    tier1.main();
    tier2.main();
    tier3.main();
    tier4.main();
  });
}
