import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../scan/barcode_image.dart';
import '../ui/layout.dart';

class ScanPage extends StatefulWidget {
  const ScanPage({super.key});

  @override
  State<ScanPage> createState() => _ScanPageState();
}

class _ScanPageState extends State<ScanPage> {
  final _controller = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
    formats: const [
      BarcodeFormat.qrCode,
      BarcodeFormat.dataMatrix,
      BarcodeFormat.code128,
      BarcodeFormat.code39,
      BarcodeFormat.ean13,
      BarcodeFormat.ean8,
    ],
  );
  bool _done = false;
  bool _picking = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _finish(String value) {
    if (_done || value.trim().isEmpty) return;
    _done = true;
    Navigator.of(context).pop(value.trim());
  }

  void _onDetect(BarcodeCapture capture) {
    if (_done || _picking) return;
    for (final b in capture.barcodes) {
      final v = b.rawValue?.trim() ?? '';
      if (v.isEmpty) continue;
      _finish(v);
      return;
    }
  }

  Future<void> _pickPhoto() async {
    if (_done || _picking) return;
    setState(() => _picking = true);
    await _controller.stop();
    if (!mounted) return;
    try {
      final code = await pickBarcodeFromPhoto(context);
      if (!mounted) return;
      if (code != null && code.isNotEmpty) {
        _finish(code);
        return;
      }
    } finally {
      if (mounted && !_done) {
        setState(() => _picking = false);
        await _controller.start();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final tablet = AppLayout.isTablet(context);
    final box = tablet ? 420.0 : 260.0;
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        title: const Text('扫描追溯码 / 批号'),
        actions: [
          IconButton(
            icon: const Icon(Icons.flash_on),
            onPressed: () => _controller.toggleTorch(),
          ),
        ],
      ),
      body: Stack(
        alignment: Alignment.center,
        children: [
          MobileScanner(controller: _controller, onDetect: _onDetect),
          IgnorePointer(
            child: Container(
              width: box,
              height: box,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.white70, width: 2),
                borderRadius: BorderRadius.circular(16),
              ),
            ),
          ),
          Align(
            alignment: Alignment.bottomCenter,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 28),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  FilledButton.icon(
                    onPressed: _picking ? null : _pickPhoto,
                    icon: const Icon(Icons.photo_outlined),
                    label: Text(_picking ? '正在识别照片…' : '从照片识别'),
                  ),
                  const SizedBox(height: 12),
                  Container(
                    width: tablet ? 520 : double.infinity,
                    padding: const EdgeInsets.fromLTRB(20, 16, 20, 16),
                    decoration: BoxDecoration(
                      color: const Color(0xCC000000),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Text(
                      '对准药盒上的追溯码、GS1 条码或批号，也可以从文件里选一张照片识别。',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.white70, height: 1.45, fontSize: 15),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
