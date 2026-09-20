import 'dart:convert';

import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/auth_api.dart';
import 'home_page.dart';
import 'intro_page.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _auth = AuthApi();
  final _nameCtrl = TextEditingController();
  final _pwdCtrl = TextEditingController();
  final _codeCtrl = TextEditingController();
  String _captchaId = '';
  String _captchaImage = '';
  bool _loading = false;
  bool _obscure = true;

  @override
  void initState() {
    super.initState();
    _refreshCaptcha();
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _pwdCtrl.dispose();
    _codeCtrl.dispose();
    super.dispose();
  }

  Future<void> _refreshCaptcha() async {
    try {
      final captcha = await _auth.fetchCaptcha();
      if (!mounted) return;
      setState(() {
        _captchaId = captcha.captchaId;
        _captchaImage = captcha.image;
        _codeCtrl.clear();
      });
    } catch (e) {
      if (!mounted) return;
      _toast(e.toString());
    }
  }

  Future<void> _submit() async {
    final name = _nameCtrl.text.trim();
    final pwd = _pwdCtrl.text;
    final code = _codeCtrl.text.trim();
    if (name.isEmpty || pwd.isEmpty || code.isEmpty) {
      _toast('请填写用户名、密码和验证码');
      return;
    }
    setState(() => _loading = true);
    try {
      await _auth.login(
        loginname: name,
        password: pwd,
        code: code,
        captchaId: _captchaId,
      );
      final user = await _auth.currentUser();
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => HomePage(user: user)),
      );
    } catch (e) {
      if (!mounted) return;
      _toast(e.toString());
      await _refreshCaptcha();
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _toast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  ImageProvider? _captchaProvider() {
    if (_captchaImage.isEmpty) return null;
    final raw = _captchaImage.contains(',')
        ? _captchaImage.split(',').last
        : _captchaImage;
    try {
      return MemoryImage(base64Decode(raw));
    } catch (_) {
      return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final captcha = _captchaProvider();
    return Scaffold(
      backgroundColor: const Color(0xFFF5F6F8),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 480),
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    '药企进销存',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '员工端 · $kApiBase',
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 12, color: Color(0xFF8A8F99)),
                  ),
                  const SizedBox(height: 32),
                  TextField(
                    controller: _nameCtrl,
                    decoration: const InputDecoration(
                      labelText: '用户名',
                      border: OutlineInputBorder(),
                    ),
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _pwdCtrl,
                    obscureText: _obscure,
                    decoration: InputDecoration(
                      labelText: '密码',
                      border: const OutlineInputBorder(),
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscure ? Icons.visibility_off : Icons.visibility,
                        ),
                        onPressed: () => setState(() => _obscure = !_obscure),
                      ),
                    ),
                    textInputAction: TextInputAction.next,
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _codeCtrl,
                          decoration: const InputDecoration(
                            labelText: '验证码',
                            border: OutlineInputBorder(),
                          ),
                          onSubmitted: (_) => _submit(),
                        ),
                      ),
                      const SizedBox(width: 12),
                      InkWell(
                        onTap: _refreshCaptcha,
                        child: Container(
                          width: 116,
                          height: 48,
                          alignment: Alignment.center,
                          decoration: BoxDecoration(
                            color: Colors.white,
                            border: Border.all(color: const Color(0xFFD9DDE3)),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: captcha == null
                              ? const Text('点击刷新')
                              : Image(image: captcha, fit: BoxFit.fill),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: _loading ? null : _submit,
                    style: FilledButton.styleFrom(
                      minimumSize: const Size.fromHeight(48),
                    ),
                    child: Text(_loading ? '登录中...' : '登录'),
                  ),
                  TextButton(
                    onPressed: () {
                      Navigator.of(context).pushReplacement(
                        MaterialPageRoute(
                          builder: (_) => const IntroPage(playBoot: false),
                        ),
                      );
                    },
                    child: const Text('返回产品介绍'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
