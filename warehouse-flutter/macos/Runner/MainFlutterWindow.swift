import Cocoa
import FlutterMacOS
import Vision

class MainFlutterWindow: NSWindow {
  override func awakeFromNib() {
    let flutterViewController = FlutterViewController()
    let windowFrame = self.frame
    self.contentViewController = flutterViewController
    self.setFrame(windowFrame, display: true)

    let channel = FlutterMethodChannel(
      name: "warehouse/barcode",
      binaryMessenger: flutterViewController.engine.binaryMessenger
    )
    channel.setMethodCallHandler { call, result in
      guard call.method == "analyzeImage" else {
        result(FlutterMethodNotImplemented)
        return
      }
      let path = (call.arguments as? [String: Any])?["path"] as? String ?? ""
      Self.detectBarcodes(path: path, result: result)
    }

    RegisterGeneratedPlugins(registry: flutterViewController)

    super.awakeFromNib()
  }

  private static func detectBarcodes(path: String, result: @escaping FlutterResult) {
    guard !path.isEmpty,
          let image = NSImage(contentsOfFile: path),
          let tiff = image.tiffRepresentation,
          let bitmap = NSBitmapImageRep(data: tiff),
          let cg = bitmap.cgImage else {
      result([])
      return
    }
    let request = VNDetectBarcodesRequest { request, _ in
      let values = (request.results as? [VNBarcodeObservation])?
        .compactMap { $0.payloadStringValue?.trimmingCharacters(in: .whitespacesAndNewlines) }
        .filter { !$0.isEmpty } ?? []
      var seen = Set<String>()
      let unique = values.filter { seen.insert($0).inserted }
      DispatchQueue.main.async { result(unique) }
    }
    request.symbologies = [.qr, .dataMatrix, .code128, .code39, .ean13, .ean8, .pdf417]
    let handler = VNImageRequestHandler(cgImage: cg, options: [:])
    DispatchQueue.global(qos: .userInitiated).async {
      do {
        try handler.perform([request])
      } catch {
        DispatchQueue.main.async {
          result(FlutterError(code: "scan", message: error.localizedDescription, details: nil))
        }
      }
    }
  }
}
