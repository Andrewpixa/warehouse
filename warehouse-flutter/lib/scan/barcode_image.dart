import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

const _channel = MethodChannel('warehouse/barcode');

const _formats = [
  BarcodeFormat.qrCode,
  BarcodeFormat.dataMatrix,
  BarcodeFormat.code128,
  BarcodeFormat.code39,
  BarcodeFormat.ean13,
  BarcodeFormat.ean8,
];

/// 从文件选择一张照片，识别其中的追溯码 / 条码。取消时返回 null。
Future<String?> pickBarcodeFromPhoto(BuildContext context) async {
  final picked = await FilePicker.platform.pickFiles(
    type: FileType.image,
    allowMultiple: false,
    dialogTitle: '选择含追溯码或条码的照片',
  );
  final path = picked?.files.single.path;
  if (path == null || path.isEmpty) return null;
  if (!context.mounted) return null;

  final codes = await barcodesInImage(path);
  if (!context.mounted) return null;
  if (codes.isEmpty) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('这张照片里没有识别到追溯码或条码')),
    );
    return null;
  }
  if (codes.length == 1) return codes.first;
  return showDialog<String>(
    context: context,
    builder: (ctx) => SimpleDialog(
      title: const Text('识别到多个码，请选择'),
      children: [
        for (final code in codes)
          SimpleDialogOption(
            onPressed: () => Navigator.pop(ctx, code),
            child: Text(code),
          ),
      ],
    ),
  );
}

Future<List<String>> barcodesInImage(String path) async {
  if (Platform.isMacOS) {
    final raw = await _channel.invokeMethod<List<dynamic>>('analyzeImage', {'path': path});
    return _unique(raw?.map((e) => '$e'));
  }
  final controller = MobileScannerController(formats: _formats);
  try {
    final capture = await controller.analyzeImage(path);
    return _unique(capture?.barcodes.map((b) => b.rawValue));
  } finally {
    await controller.dispose();
  }
}

List<String> _unique(Iterable<String?>? values) {
  final seen = <String>{};
  final out = <String>[];
  for (final raw in values ?? const <String?>[]) {
    final text = raw?.trim() ?? '';
    if (text.isEmpty || !seen.add(text)) continue;
    out.add(text);
  }
  return out;
}
