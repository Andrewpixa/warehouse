import 'package:flutter/material.dart';

import '../api/pharma_api.dart';
import '../ui/layout.dart';

class BatchStockPage extends StatefulWidget {
  const BatchStockPage({super.key});

  @override
  State<BatchStockPage> createState() => _BatchStockPageState();
}

class _BatchStockPageState extends State<BatchStockPage> {
  final _api = PharmaApi();
  final _scrollCtrl = ScrollController();
  final _batchCtrl = TextEditingController();
  final _drugCtrl = TextEditingController();

  final List<BatchStock> _rows = [];
  int _page = 1;
  final int _limit = 20;
  num _total = 0;
  bool _loading = false;
  bool _loadingMore = false;
  String? _error;
  String _expireFilter = 'all'; // all / near / expired

  @override
  void initState() {
    super.initState();
    _scrollCtrl.addListener(_onScroll);
    _reload();
  }

  @override
  void dispose() {
    _scrollCtrl.dispose();
    _batchCtrl.dispose();
    _drugCtrl.dispose();
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
      final res = await _api.loadAllBatchStock(
        page: _page,
        limit: _limit,
        expireFilter: _expireFilter,
        nearExpireDays: 90,
        batchNo: _batchCtrl.text.trim(),
        drugName: _drugCtrl.text.trim(),
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
      final res = await _api.loadAllBatchStock(
        page: _page,
        limit: _limit,
        expireFilter: _expireFilter,
        nearExpireDays: 90,
        batchNo: _batchCtrl.text.trim(),
        drugName: _drugCtrl.text.trim(),
      );
      _rows.addAll(res.rows);
      _total = res.total;
      if (mounted) setState(() => _loadingMore = false);
    } catch (_) {
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
                        controller: _drugCtrl,
                        decoration: const InputDecoration(
                          isDense: true,
                          labelText: '药品名称',
                          border: OutlineInputBorder(),
                        ),
                        onSubmitted: (_) => _reload(),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        controller: _batchCtrl,
                        decoration: const InputDecoration(
                          isDense: true,
                          labelText: '批号',
                          border: OutlineInputBorder(),
                        ),
                        onSubmitted: (_) => _reload(),
                      ),
                    ),
                    const SizedBox(width: 8),
                    IconButton.outlined(
                      onPressed: _loading ? null : _reload,
                      icon: const Icon(Icons.search),
                      tooltip: '查询',
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    _expireChip('全部', 'all'),
                    const SizedBox(width: 6),
                    _expireChip('近效期(90d)', 'near'),
                    const SizedBox(width: 6),
                    _expireChip('已过期', 'expired'),
                    const Spacer(),
                    Text('共 $_total 条',
                        style: const TextStyle(color: Color(0xFF8A8F99), fontSize: 12)),
                  ],
                ),
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
                            rowHeight: AppLayout.isTablet(context) ? 188 : 200,
                            padding: AppLayout.pageInsets(context),
                            itemBuilder: (ctx, i) {
                              if (i >= _rows.length) {
                                return const Center(child: CircularProgressIndicator());
                              }
                              return _stockCard(_rows[i]);
                            },
                          ),
          ),
        ],
      ),
    );
  }

  Widget _expireChip(String label, String value) {
    final selected = _expireFilter == value;
    return FilterChip(
      label: Text(label),
      selected: selected,
      onSelected: (_) {
        setState(() => _expireFilter = value);
        _reload();
      },
    );
  }

  Widget _stockCard(BatchStock s) {
    final status = _expireStatus(s.expireDate);
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
                    s.drugName.isEmpty ? '#${s.drugId}' : s.drugName,
                    style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                  ),
                ),
                _tag(
                  status.label,
                  status.color,
                ),
                if (s.qualityStatus.isNotEmpty) ...[
                  const SizedBox(width: 4),
                  _tag(s.qualityStatus, const Color(0xFF6B7280)),
                ],
              ],
            ),
            const SizedBox(height: 6),
            _metaRow('批号', s.batchNo),
            _metaRow('规格', s.drugSpec),
            _metaRow('库存', s.qty),
            _metaRow('生产日期', s.productionDate),
            _metaRow('有效期至', s.expireDate, highlight: status.highlight),
            if (s.warehouseName.isNotEmpty) _metaRow('仓库', s.warehouseName),
          ],
        ),
      ),
    );
  }

  ({String label, Color color, bool highlight}) _expireStatus(String expireDate) {
    if (expireDate.isEmpty) {
      return (label: '未知', color: const Color(0xFF6B7280), highlight: false);
    }
    try {
      final d = DateTime.parse(expireDate);
      final now = DateTime.now();
      final days = d.difference(now).inDays;
      if (days < 0) return (label: '已过期', color: const Color(0xFFB91C1C), highlight: true);
      if (days <= 90) return (label: '近效期', color: const Color(0xFFEA580C), highlight: true);
      return (label: '正常', color: const Color(0xFF16A34A), highlight: false);
    } catch (_) {
      return (label: '未知', color: const Color(0xFF6B7280), highlight: false);
    }
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

  Widget _metaRow(String label, String value, {bool highlight = false}) {
    if (value.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(top: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 72,
            child: Text(label, style: const TextStyle(color: Color(0xFF8A8F99), fontSize: 12)),
          ),
          Expanded(
            child: Text(
              value,
              style: TextStyle(
                fontSize: 13,
                color: highlight ? const Color(0xFFB91C1C) : null,
                fontWeight: highlight ? FontWeight.w600 : null,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _emptyView() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.inventory_2_outlined, size: 56, color: Color(0xFFC7CAD1)),
          const SizedBox(height: 8),
          const Text('暂无库存数据'),
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
