// Basic smoke test: app entry should build without throwing.
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:warehouse_flutter/main.dart';

void main() {
  testWidgets('App boots to bootstrap page', (WidgetTester tester) async {
    await tester.pumpWidget(const WarehouseApp());
    // First frame: bootstrap page shows a progress indicator
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}
