import 'package:flutter/material.dart';

import 'api/api_client.dart';
import 'api/auth_api.dart';
import 'pages/intro_page.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const WarehouseApp());
}

class WarehouseApp extends StatelessWidget {
  const WarehouseApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '药企进销存',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFFC45C5C)),
        useMaterial3: true,
        visualDensity: VisualDensity.standard,
        navigationRailTheme: const NavigationRailThemeData(
          minWidth: 88,
          minExtendedWidth: 200,
        ),
      ),
      home: const _BootstrapPage(),
    );
  }
}

class _BootstrapPage extends StatefulWidget {
  const _BootstrapPage();

  @override
  State<_BootstrapPage> createState() => _BootstrapPageState();
}

class _BootstrapPageState extends State<_BootstrapPage> {
  @override
  void initState() {
    super.initState();
    _restore();
  }

  Future<void> _restore() async {
    final client = ApiClient.instance;
    await client.loadToken();
    AuthUser? user;
    if (client.token != null && client.token!.isNotEmpty) {
      try {
        user = await AuthApi().currentUser();
      } catch (_) {
        await client.clearToken();
      }
    }
    final Widget next = IntroPage(user: user);
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => next),
    );
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(child: CircularProgressIndicator()),
    );
  }
}
