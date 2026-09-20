import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/auth_api.dart';
import '../pages/ops_page.dart';
import '../ui/layout.dart';
import 'batch_stock_page.dart';
import 'drug_page.dart';
import 'intro_page.dart';
import 'login_page.dart';
import 'trace_query_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key, required this.user});

  final AuthUser user;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _index = 0;

  late final List<Widget> _pages = [
    const DrugPage(),
    const BatchStockPage(),
    const TraceQueryPage(),
    const _OpsHubPage(),
    _ProfilePage(user: widget.user, onLogout: _logout),
  ];

  Future<void> _logout() async {
    await AuthApi().logout();
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => const LoginPage()),
    );
  }

  static const _destinations = [
    NavigationDestination(
      icon: Icon(Icons.medication_outlined),
      selectedIcon: Icon(Icons.medication),
      label: '药品',
    ),
    NavigationDestination(
      icon: Icon(Icons.inventory_2_outlined),
      selectedIcon: Icon(Icons.inventory_2),
      label: '批号库存',
    ),
    NavigationDestination(
      icon: Icon(Icons.qr_code_scanner_outlined),
      selectedIcon: Icon(Icons.qr_code_scanner),
      label: '追溯查询',
    ),
    NavigationDestination(
      icon: Icon(Icons.account_tree_outlined),
      selectedIcon: Icon(Icons.account_tree),
      label: '运作',
    ),
    NavigationDestination(
      icon: Icon(Icons.person_outline),
      selectedIcon: Icon(Icons.person),
      label: '我的',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    final wide = AppLayout.isWide(context);
    final content = SafeArea(
      bottom: !wide,
      child: IndexedStack(index: _index, children: _pages),
    );
    if (wide) {
      return Scaffold(
        body: Row(
          children: [
            NavigationRail(
              selectedIndex: _index,
              extended: MediaQuery.sizeOf(context).width >= 1100,
              onDestinationSelected: (i) => setState(() => _index = i),
              labelType: MediaQuery.sizeOf(context).width >= 1100
                  ? NavigationRailLabelType.none
                  : NavigationRailLabelType.all,
              destinations: [
                for (final d in _destinations)
                  NavigationRailDestination(
                    icon: d.icon,
                    selectedIcon: d.selectedIcon,
                    label: Text(d.label),
                  ),
              ],
            ),
            const VerticalDivider(width: 1),
            Expanded(child: content),
          ],
        ),
      );
    }
    return Scaffold(
      body: content,
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: _destinations,
      ),
    );
  }
}

class _OpsHubPage extends StatefulWidget {
  const _OpsHubPage();

  @override
  State<_OpsHubPage> createState() => _OpsHubPageState();
}

class _OpsHubPageState extends State<_OpsHubPage> {
  static const items = [
    ('STOCKOUT', '缺货补货', Icons.inventory_outlined),
    ('INBOUND_EX', '到货异常', Icons.report_gmailerrorred_outlined),
    ('RETURN_NOTICE', '销退通知', Icons.assignment_return_outlined),
    ('OFFSET', '收款冲账', Icons.account_balance_outlined),
    ('ALLOCATE', '分货', Icons.call_split_outlined),
    ('LOGISTICS', '物流联系单', Icons.local_shipping_outlined),
    ('CREDIT', '信誉额', Icons.verified_outlined),
    ('QUOTA', '统筹值', Icons.warehouse_outlined),
  ];

  int _selected = 0;

  @override
  Widget build(BuildContext context) {
    final tablet = AppLayout.isTablet(context);
    return Scaffold(
      appBar: AppBar(title: const Text('公司运作')),
      body: tablet
          ? Row(
              children: [
                SizedBox(
                  width: 280,
                  child: ListView(
                    children: [
                      for (var i = 0; i < items.length; i++)
                        ListTile(
                          selected: _selected == i,
                          leading: Icon(items[i].$3),
                          title: Text(items[i].$2),
                          onTap: () => setState(() => _selected = i),
                        ),
                    ],
                  ),
                ),
                const VerticalDivider(width: 1),
                Expanded(
                  child: OpsPage(
                    key: ValueKey(items[_selected].$1),
                    docType: items[_selected].$1,
                    title: items[_selected].$2,
                    embedded: true,
                  ),
                ),
              ],
            )
          : GridView.count(
              padding: AppLayout.pageInsets(context),
              crossAxisCount: 2,
              childAspectRatio: 2.4,
              crossAxisSpacing: 12,
              mainAxisSpacing: 12,
              children: [
                for (final e in items)
                  Card(
                    child: InkWell(
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => OpsPage(docType: e.$1, title: e.$2),
                        ),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Row(
                          children: [
                            Icon(e.$3, color: const Color(0xFFC45C5C), size: 28),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Text(e.$2, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                            ),
                            const Icon(Icons.chevron_right),
                          ],
                        ),
                      ),
                    ),
                  ),
              ],
            ),
    );
  }
}

class _ProfilePage extends StatelessWidget {
  const _ProfilePage({required this.user, required this.onLogout});

  final AuthUser user;
  final Future<void> Function() onLogout;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('我的')),
      body: Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 720),
          child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 28,
                    backgroundColor: const Color(0xFFC45C5C),
                    child: const Icon(Icons.person, color: Colors.white),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          user.name.isEmpty ? user.loginname : user.name,
                          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '账号 ${user.loginname}',
                          style: const TextStyle(color: Color(0xFF6B7280)),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          ListTile(
            leading: const Icon(Icons.cloud_outlined),
            title: const Text('接口地址'),
            subtitle: Text(kApiBase, style: const TextStyle(fontSize: 12)),
          ),
          ListTile(
            leading: const Icon(Icons.info_outline),
            title: const Text('应用版本'),
            subtitle: const Text('warehouse_flutter 1.0.0+1 · Flutter 跨平台'),
          ),
          ListTile(
            leading: const Icon(Icons.phone_iphone_outlined),
            title: const Text('平台'),
            subtitle: const Text('iOS / Android / macOS / Web 多端原生运行'),
          ),
          ListTile(
            leading: const Icon(Icons.auto_awesome_outlined),
            title: const Text('产品介绍'),
            subtitle: const Text('查看系统能力与追溯链路'),
            onTap: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => IntroPage(user: user, playBoot: false),
                ),
              );
            },
          ),
          const Divider(),
          ListTile(
            leading: const Icon(Icons.logout, color: Color(0xFFB91C1C)),
            title: const Text('退出登录', style: TextStyle(color: Color(0xFFB91C1C))),
            onTap: onLogout,
          ),
        ],
          ),
        ),
      ),
    );
  }
}
