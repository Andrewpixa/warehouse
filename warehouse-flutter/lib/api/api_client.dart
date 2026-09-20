import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

/// 首选：Nginx 反代 + HTTPS 域名（手机 4G/5G、正常办公网络下使用）
const String kApiBasePrimary = 'https://yaoheng.cloud/warehouse';

/// 兜底：直连 Spring Boot 8899 端口（HTTP 明文）。
/// 部分网络下 yaoheng.cloud:443 的 TLS 握手会被链路 reset（errno=54），
/// 此时自动降级到直连，请求结果与 token 完全一致（同一后端进程）。
const String kApiBaseFallback = 'http://123.57.134.57:8899/warehouse';

/// 对外暴露的当前地址（页面展示用）
String get kApiBase => ApiClient.instance.baseUrl;

const String kTokenKey = 'satoken';

class ApiException implements Exception {
  ApiException(this.message);
  final String message;

  @override
  String toString() => message;
}

class ApiClient {
  ApiClient._();
  static final ApiClient instance = ApiClient._();

  String? token;
  String _base = kApiBasePrimary;
  String get baseUrl => _base;

  Future<void> loadToken() async {
    final prefs = await SharedPreferences.getInstance();
    token = prefs.getString(kTokenKey);
  }

  Future<void> saveToken(String value) async {
    token = value;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(kTokenKey, value);
  }

  Future<void> clearToken() async {
    token = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(kTokenKey);
  }

  Map<String, String> _headers({bool form = false}) {
    final headers = <String, String>{
      if (form) 'Content-Type': 'application/x-www-form-urlencoded',
    };
    if (token != null && token!.isNotEmpty) {
      headers['satoken'] = token!;
    }
    return headers;
  }

  Uri _uri(String base, String path, [Map<String, String>? query]) {
    return Uri.parse('$base$path').replace(queryParameters: query);
  }

  String? _headerToken(http.Response res) {
    return res.headers['satoken'] ?? res.headers['Satoken'];
  }

  /// 统一发送：连接级故障（reset / 超时 / DNS）时自动切换到另一条线路重试一次，
  /// 并在本次运行期内粘住可用线路，避免每次请求都先失败一次。
  Future<http.Response> _send(
    Future<http.Response> Function(String base) doRequest,
  ) async {
    Object? firstError;
    for (var attempt = 0; attempt < 2; attempt++) {
      try {
        final res = await doRequest(_base).timeout(const Duration(seconds: 12));
        return res;
      } on TimeoutException catch (e) {
        firstError ??= e;
      } on http.ClientException catch (e) {
        // SocketException / connection reset 都包在 ClientException 里
        firstError ??= e;
      }
      _base = _base == kApiBasePrimary ? kApiBaseFallback : kApiBasePrimary;
    }
    throw ApiException('网络连接失败，请检查网络后重试');
  }

  Future<Map<String, dynamic>> get(String path,
      [Map<String, String>? query]) async {
    final res = await _send(
      (base) => http.get(_uri(base, path, query), headers: _headers()),
    );
    return _decode(res);
  }

  Future<Map<String, dynamic>> postForm(
    String path,
    Map<String, String> body,
  ) async {
    final res = await _send(
      (base) => http.post(
        _uri(base, path),
        headers: _headers(form: true),
        body: body,
      ),
    );
    final json = _decode(res);
    final headerToken = _headerToken(res);
    if (headerToken != null && headerToken.isNotEmpty) {
      await saveToken(headerToken);
    }
    return json;
  }

  Map<String, dynamic> _decode(http.Response res) {
    if (res.statusCode == 401) {
      throw ApiException('登录已过期，请重新登录');
    }
    if (res.statusCode < 200 || res.statusCode >= 300) {
      throw ApiException('请求失败 (${res.statusCode})');
    }
    final decoded = jsonDecode(utf8.decode(res.bodyBytes));
    if (decoded is! Map<String, dynamic>) {
      throw ApiException('响应格式错误');
    }
    final code = decoded['code'];
    if (code == -1) {
      throw ApiException((decoded['msg'] as String?) ?? '操作失败');
    }
    return decoded;
  }
}
