import 'api_client.dart';

class Captcha {
  Captcha({required this.captchaId, required this.image});
  final String captchaId;
  final String image;
}

class AuthUser {
  AuthUser({required this.name, required this.loginname});
  final String name;
  final String loginname;
}

class AuthApi {
  final _client = ApiClient.instance;

  Future<Captcha> fetchCaptcha() async {
    final json = await _client.get('/login/getCaptchaBase64');
    return Captcha(
      captchaId: (json['captchaId'] as String?) ?? '',
      image: (json['image'] as String?) ?? '',
    );
  }

  Future<void> login({
    required String loginname,
    required String password,
    required String code,
    required String captchaId,
  }) async {
    final json = await _client.postForm('/login/login', {
      'loginname': loginname,
      'pwd': password,
      'code': code,
      'captchaId': captchaId,
      'platform': 'app',
    });
    final token = json['token'] as String?;
    if (token != null && token.isNotEmpty) {
      await _client.saveToken(token);
    }
    if (_client.token == null || _client.token!.isEmpty) {
      throw ApiException('登录成功但未拿到令牌');
    }
  }

  Future<AuthUser> currentUser() async {
    final json = await _client.get('/login/currentUser');
    final user = json['user'];
    if (user is! Map<String, dynamic>) {
      throw ApiException('未登录');
    }
    return AuthUser(
      name: (user['name'] as String?) ?? '',
      loginname: (user['loginname'] as String?) ?? '',
    );
  }

  Future<void> logout() async {
    try {
      await _client.get('/login/logout');
    } finally {
      await _client.clearToken();
    }
  }
}
