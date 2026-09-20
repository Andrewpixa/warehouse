import 'package:flutter/material.dart';

import '../api/pharma_api.dart';
import '../ui/layout.dart';

class DrugPage extends StatefulWidget {
  const DrugPage({super.key});

  @override
  State<DrugPage> createState() => _DrugPageState();
}

class _DrugPageState extends State<DrugPage> {
  final _api = PharmaApi();
  final _scrollCtrl = ScrollController();
  final _genericCtrl = TextEditingController();
  final _tradeCtrl = TextEditingController();
  final _codeCtrl = TextEditingController();

  final List<Drug> _rows = [];
  int _page = 1;
  final int _limit = 20;
  num _total = 0;
  bool _loading = false;
  bool _loadingMore = false;
  String? _error;
  String _categoryFilter = ''; // '' / 'drug' / 'device'
  int? _statusFilter; // null / 1 / 0

  @override
  void initState() {
    super.initState();
    _scrollCtrl.addListener(_onScroll);
    _reload();
  }

  @override
  void dispose() {
    _scrollCtrl.dispose();
    _genericCtrl.dispose();
    _tradeCtrl.dispose();
    _codeCtrl.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_scrollCtrl.hasClients) return;
    if (_scrollCtrl.position.pixels >= _scrollCtrl.position.maxScrollExtent - 80 &&
        !_loadingMore &&
        !_loading &&
        _rows.length < _total) {
      _loadMore();
    }
  }

  Future<void> _reload() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    _page = 1;
    try {
      final res = await _api.loadAllDrug(
        page: _page,
        limit: _limit,
        genericName: _genericCtrl.text.trim(),
        tradeName: _tradeCtrl.text.trim(),
        code: _codeCtrl.text.trim(),
        status: _statusFilter,
        category: _categoryFilter.isEmpty ? null : _categoryFilter,
      );
      _rows
        ..clear()
        ..addAll(res.rows);
      _total = res.total;
      if (mounted) setState(() => _loading = false);
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString();
          _loading = false;
        });
      }
    }
  }

  Future<void> _loadMore() async {
    setState(() => _loadingMore = true);
    try {
      _page += 1;
      final res = await _api.loadAllDrug(
        page: _page,
        limit: _limit,
        genericName: _genericCtrl.text.trim(),
        tradeName: _tradeCtrl.text.trim(),
        code: _codeCtrl.text.trim(),
        status: _statusFilter,
        category: _categoryFilter.isEmpty ? null : _categoryFilter,
      );
      _rows.addAll(res.rows);
      _total = res.total;
      if (mounted) setState(() => _loadingMore = false);
    } catch (e) {
      _page -= 1;
      if (mounted) setState(() => _loadingMore = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          Padding(
            padding: AppLayout.pageInsets(context),
            child: Column(
              children: [
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _genericCtrl,
                        decoration: const InputDecoration(
                          isDense: true,
                          labelText: '通用名',
                          border: OutlineInputBorder(),
                        ),
                        onSubmitted: (_) => _reload(),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        controller: _tradeCtrl,
                        decoration: const InputDecoration(
                          isDense: true,
                          labelText: '商品名',
                          border: OutlineInputBorder(),
                        ),
                        onSubmitted: (_) => _reload(),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        controller: _codeCtrl,
                        decoration: const InputDecoration(
                          isDense: true,
                          labelText: '编码',
                          border: OutlineInputBorder(),
                        ),
                        onSubmitted: (_) => _reload(),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    _filterChip('全部', '', _categoryFilter),
                    const SizedBox(width: 6),
                    _filterChip('药品', '药品', _categoryFilter),
                    const SizedBox(width: 6),
                    _filterChip('器械', '器械', _categoryFilter),
                    const SizedBox(width: 12),
                    _statusChip('启用', 1),
                    const SizedBox(width: 6),
                    _statusChip('停用', 0),
                    const Spacer(),
                    IconButton.outlined(
                      onPressed: _loading ? null : _reload,
                      icon: const Icon(Icons.search),
                      tooltip: '查询',
                      style: IconButton.styleFrom(minimumSize: const Size(48, 48)),
                    ),
                  ],
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(
              children: [
                Text('共 $_total 条', style: const TextStyle(color: Color(0xFF8A8F99), fontSize: 12)),
                const Spacer(),
                Text('已加载 ${_rows.length} 条',
                    style: const TextStyle(color: Color(0xFF8A8F99), fontSize: 12)),
              ],
            ),
          ),
          Expanded(
            child: _loading && _rows.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? _errorView()
                    : _rows.isEmpty
                        ? _emptyView()
                        : AdaptiveCardGrid(
                            controller: _scrollCtrl,
                            itemCount: _rows.length + (_loadingMore ? 1 : 0),
                            rowHeight: AppLayout.isTablet(context) ? 188 : 210,
                            padding: AppLayout.pageInsets(context),
                            itemBuilder: (ctx, i) {
                              if (i >= _rows.length) {
                                return const Center(child: CircularProgressIndicator());
                              }
                              return _drugCard(_rows[i]);
                            },
                          ),
          ),
        ],
      ),
    );
  }

  Widget _filterChip(String label, String value, String current) {
    final selected = current == value;
    return FilterChip(
      label: Text(label),
      selected: selected,
      onSelected: (_) {
        setState(() => _categoryFilter = value);
        _reload();
      },
    );
  }

  Widget _statusChip(String label, int value) {
    final selected = _statusFilter == value;
    return FilterChip(
      label: Text(label),
      selected: selected,
      onSelected: (_) {
        setState(() => _statusFilter = selected ? null : value);
        _reload();
      },
    );
  }

  Widget _drugCard(Drug d) {
    final cold = d.isColdChain == 1;
    final enabled = d.status == 1;
    return Card(
      margin: AppLayout.columns(context) > 1 ? EdgeInsets.zero : const EdgeInsets.only(bottom: 10),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    d.genericName.isEmpty ? d.code : '${d.genericName} ${d.tradeName}'.trim(),
                    style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                  ),
                ),
                if (cold) _tag('冷链', const Color(0xFF1F6FEB)),
                const SizedBox(width: 4),
                _tag(
                  enabled ? '启用' : '停用',
                  enabled ? const Color(0xFF16A34A) : const Color(0xFFB91C1C),
                ),
                const SizedBox(width: 4),
                _tag(_categoryLabel(d), const Color(0xFF6B7280)),
              ],
            ),
            const SizedBox(height: 6),
            _metaRow('编码', d.code),
            _metaRow('规格', '${d.spec} ${d.unit}'.trim()),
            if (d.manufacturer.isNotEmpty) _metaRow('厂家', d.manufacturer),
            if (d.approvalNo.isNotEmpty) _metaRow('批准文号', d.approvalNo),
            if (d.barcode.isNotEmpty) _metaRow('条码', d.barcode),
            if (d.refPurchasePrice.isNotEmpty || d.refSalePrice.isNotEmpty)
              _metaRow('参考价',
                  '进 ${d.refPurchasePrice} / 销 ${d.refSalePrice}'.replaceAll(' / ', d.refPurchasePrice.isEmpty || d.refSalePrice.isEmpty ? '' : ' / ')),
            if (d.remark.isNotEmpty) _metaRow('备注', d.remark),
          ],
        ),
      ),
    );
  }

  String _categoryLabel(Drug d) {
    if (d.category == '器械' || d.category == 'device') return '器械';
    if (d.category == '药品' || d.category == 'drug') return '药品';
    final id = d.id.toInt();
    if (id >= 400001 && id <= 499999) return '器械';
    if (id >= 300001 && id <= 399999) return '药品';
    return '未分类';
  }

  Widget _tag(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Text(text, style: TextStyle(color: color, fontSize: 11)),
    );
  }

  Widget _metaRow(String label, String value) {
    if (value.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(top: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 64,
            child: Text(label, style: const TextStyle(color: Color(0xFF8A8F99), fontSize: 12)),
          ),
          Expanded(child: Text(value, style: const TextStyle(fontSize: 13))),
        ],
      ),
    );
  }

  Widget _emptyView() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.medication_outlined, size: 56, color: Color(0xFFC7CAD1)),
          const SizedBox(height: 8),
          const Text('暂无药品数据'),
          const SizedBox(height: 12),
          OutlinedButton(onPressed: _reload, child: const Text('刷新')),
        ],
      ),
    );
  }

  Widget _errorView() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 56, color: Color(0xFFB91C1C)),
          const SizedBox(height: 8),
          Text(_error ?? ''),
          const SizedBox(height: 12),
          OutlinedButton(onPressed: _reload, child: const Text('重试')),
        ],
      ),
    );
  }
}
