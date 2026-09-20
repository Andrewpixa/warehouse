import 'package:flutter/material.dart';

/// 平板（最短边 ≥ 600）与宽屏（宽 ≥ 840，如 iPad 横屏）断点。
class AppLayout {
  static bool isTablet(BuildContext context) =>
      MediaQuery.sizeOf(context).shortestSide >= 600;

  static bool isWide(BuildContext context) =>
      MediaQuery.sizeOf(context).width >= 840;

  static int columns(BuildContext context, {int max = 3}) {
    final w = MediaQuery.sizeOf(context).width;
    if (w >= 1200) return max.clamp(1, 3);
    if (w >= 700) return 2.clamp(1, max);
    return 1;
  }

  static EdgeInsets pageInsets(BuildContext context) {
    final tablet = isTablet(context);
    return EdgeInsets.fromLTRB(tablet ? 24 : 12, tablet ? 16 : 12, tablet ? 24 : 12, 8);
  }
}

class AdaptiveCardGrid extends StatelessWidget {
  const AdaptiveCardGrid({
    super.key,
    required this.itemCount,
    required this.itemBuilder,
    this.controller,
    this.rowHeight = 200,
    this.padding,
  });

  final int itemCount;
  final IndexedWidgetBuilder itemBuilder;
  final ScrollController? controller;
  final double rowHeight;
  final EdgeInsets? padding;

  @override
  Widget build(BuildContext context) {
    final cols = AppLayout.columns(context);
    final pad = padding ?? const EdgeInsets.all(12);
    if (cols <= 1) {
      return ListView.builder(
        controller: controller,
        padding: pad,
        itemCount: itemCount,
        itemBuilder: itemBuilder,
      );
    }
    return GridView.builder(
      controller: controller,
      padding: pad,
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: cols,
        crossAxisSpacing: 12,
        mainAxisSpacing: 12,
        mainAxisExtent: rowHeight,
      ),
      itemCount: itemCount,
      itemBuilder: itemBuilder,
    );
  }
}
