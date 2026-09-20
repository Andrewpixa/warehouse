import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../api/pharma_api.dart';
import '../ui/layout.dart';
import 'scan_page.dart';

class TraceQueryPage extends StatefulWidget {
  const TraceQueryPage({super.key});

  @override
  State<TraceQueryPage> createState() => _TraceQueryPageState();
}

class _TraceQueryPageState extends State<TraceQueryPage> {
  final _api = PharmaApi();
  final _inputCtrl = TextEditingController();

  bool _loading = false;
  String? _error;
  Map<String, dynamic>? _result;

  @override
  void dispose() {
    _inputCtrl.dispose();
    super.dispose();
  }

  Future<void> _scan() async {
    final code = await Navigator.of(context).push<String>(
      MaterialPageRoute(builder: (_) => const ScanPage()),
    );
    if (code == null || code.isEmpty) return;
    _inputCtrl.text = code;
    await _query();
  }

  Future<void> _query() async {
    final text = _inputCtrl.text.trim();
    if (text.isEmpty) {
      _toast('请扫描或输入追溯码、批号、入库/出库单号或发票号');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
      _result = null;
    });
    try {
      _result = await _api.lookupTrace(text);
    } catch (e) {
      _error = e.toString();
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _toast(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  String _s(dynamic v) => v == null ? '' : '$v';

  Map<String, dynamic>? _map(dynamic v) =>
      v is Map<String, dynamic> ? v : (v is Map ? Map<String, dynamic>.from(v) : null);

  @override
  Widget build(BuildContext context) {
    final wide = AppLayout.isWide(context);
    final queryBar = Padding(
      padding: AppLayout.pageInsets(context),
      child: Column(
        children: [
          if (wide)
            SizedBox(
              width: double.infinity,
              height: 56,
              child: FilledButton.icon(
                onPressed: _loading ? null : _scan,
                icon: const Icon(Icons.qr_code_scanner, size: 28),
                label: const Text('扫描追溯码 / 批号', style: TextStyle(fontSize: 16)),
              ),
            ),
          if (wide) const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _inputCtrl,
                  decoration: const InputDecoration(
                    labelText: '追溯码 / 批号 / 单号 / 发票',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.qr_code_scanner),
                  ),
                  onSubmitted: (_) => _query(),
                ),
              ),
              const SizedBox(width: 8),
              if (!wide)
                IconButton.filled(
                  onPressed: _loading ? null : _scan,
                  icon: const Icon(Icons.qr_code_scanner),
                  tooltip: '扫码',
                  style: IconButton.styleFrom(minimumSize: const Size(48, 48)),
                ),
              if (!wide) const SizedBox(width: 4),
              IconButton.outlined(
                onPressed: _loading ? null : _query,
                icon: const Icon(Icons.search),
                tooltip: '查询',
                style: IconButton.styleFrom(minimumSize: const Size(48, 48)),
              ),
            ],
          ),
          Row(
            children: [
              TextButton(
                onPressed: () async {
                  final data = await Clipboard.getData('text/plain');
                  if (data?.text != null && data!.text!.isNotEmpty) {
                    _inputCtrl.text = data.text!.trim();
                    _query();
                  } else {
                    _toast('剪贴板为空');
                  }
                },
                child: const Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.content_paste, size: 18),
                    SizedBox(width: 4),
                    Text('粘贴'),
                  ],
                ),
              ),
              const Expanded(
                child: Text(
                  '扫码可查该批货属于哪张入库单或出库单。',
                  style: TextStyle(color: Color(0xFF8A8F99), fontSize: 12),
                ),
              ),
            ],
          ),
        ],
      ),
    );

    final result = _loading
        ? const Center(child: CircularProgressIndicator())
        : _error != null
            ? _errorView()
            : _result == null
                ? _emptyView()
                : _resultView(_result!);

    if (wide) {
      return Scaffold(
        body: Row(
          children: [
            SizedBox(width: 380, child: queryBar),
            const VerticalDivider(width: 1),
            Expanded(child: result),
          ],
        ),
      );
    }
    return Scaffold(
      body: Column(
        children: [
          queryBar,
          Expanded(child: result),
        ],
      ),
    );
  }

  Widget _resultView(Map<String, dynamic> r) {
    final bill = _s(r['billType']);
    final inbound = bill.contains('入');
    final candidates = (r['candidates'] as List?) ?? const [];
    final traces = (r['traces'] as List?) ?? const [];
    final purchase = _map(r['purchase']);
    final outbound = _map(r['outbound']);
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        Card(
          color: inbound ? const Color(0xFFECFDF5) : const Color(0xFFEFF6FF),
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(
                      inbound ? Icons.move_to_inbox : Icons.outbox,
                      color: inbound ? const Color(0xFF15803D) : const Color(0xFF1D4ED8),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      bill.isEmpty ? '查询结果' : bill,
                      style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
                    ),
                    const Spacer(),
                    if (_s(r['matchTypeLabel']).isNotEmpty)
                      _tag(_s(r['matchTypeLabel']), const Color(0xFF6B7280)),
                  ],
                ),
                const SizedBox(height: 6),
                _metaRow('关键字', _s(r['keyword'])),
              ],
            ),
          ),
        ),
        if (purchase != null) ...[
          const SizedBox(height: 12),
          _orderCard(
            title: '入库单',
            color: const Color(0xFFECFDF5),
            icon: Icons.move_to_inbox,
            orderNo: _s(purchase['orderNo']),
            invoiceNo: _s(purchase['invoiceNo']),
            partyLabel: '供应商',
            party: _s(purchase['supplierName']),
            warehouse: _s(purchase['warehouseName']),
            status: _s(purchase['status']),
            date: _s(purchase['bizDate']),
            amount: _s(purchase['totalAmount']),
            items: purchase['items'] as List? ?? const [],
            inbound: true,
          ),
        ],
        if (outbound != null) ...[
          const SizedBox(height: 12),
          _orderCard(
            title: '出库单',
            color: const Color(0xFFEFF6FF),
            icon: Icons.outbox,
            orderNo: _s(outbound['orderNo']),
            invoiceNo: _s(outbound['invoiceNo']),
            partyLabel: '客户',
            party: _s(outbound['customerName']),
            warehouse: _s(outbound['warehouseName']),
            status: _s(outbound['status']),
            date: _s(outbound['bizDate']),
            amount: _s(outbound['totalAmount']),
            items: outbound['items'] as List? ?? const [],
            inbound: false,
          ),
        ],
        if (candidates.isNotEmpty) ...[
          const SizedBox(height: 12),
          const Text('该批号对应多张单据，点选查看', style: TextStyle(fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          for (final c in candidates)
            _candidateTile(c is Map ? Map<String, dynamic>.from(c) : const {}),
        ],
        if (traces.isNotEmpty) ...[
          const SizedBox(height: 12),
          Text('追溯码（${traces.length} 条）', style: const TextStyle(fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          for (final t in traces)
            _traceCard(t is Map ? Map<String, dynamic>.from(t) : const {}),
        ],
      ],
    );
  }

  Widget _candidateTile(Map<String, dynamic> c) {
    final bill = _s(c['billType']);
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Icon(bill.contains('入') ? Icons.move_to_inbox : Icons.outbox),
        title: Text(_s(c['drugName']).isEmpty ? _s(c['orderNo']) : _s(c['drugName'])),
        subtitle: Text('$bill  批号 ${_s(c['batchNo'])}  ${_s(c['orderNo'])}  ${_s(c['partyName'])}'),
        trailing: const Icon(Icons.chevron_right),
        onTap: () {
          final no = _s(c['orderNo']);
          if (no.isEmpty) return;
          _inputCtrl.text = no;
          _query();
        },
      ),
    );
  }

  Widget _orderCard({
    required String title,
    required Color color,
    required IconData icon,
    required String orderNo,
    required String invoiceNo,
    required String partyLabel,
    required String party,
    required String warehouse,
    required String status,
    required String date,
    required String amount,
    required List items,
    required bool inbound,
  }) {
    return Card(
      color: color,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, color: inbound ? const Color(0xFF15803D) : const Color(0xFF1D4ED8)),
                const SizedBox(width: 6),
                Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                const Spacer(),
                if (status.isNotEmpty) _tag(status, const Color(0xFF1F6FEB)),
              ],
            ),
            const SizedBox(height: 6),
            _metaRow('单号', orderNo),
            if (invoiceNo.isNotEmpty) _metaRow(inbound ? '供应商发票' : '销售发票', invoiceNo),
            if (party.isNotEmpty) _metaRow(partyLabel, party),
            if (warehouse.isNotEmpty) _metaRow('仓库', warehouse),
            if (date.isNotEmpty) _metaRow('业务日期', date),
            if (amount.isNotEmpty) _metaRow('金额', amount),
            if (items.isNotEmpty) ...[
              const SizedBox(height: 8),
              const Text('明细', style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
              for (final raw in items)
                Builder(builder: (_) {
                  final it = raw is Map ? Map<String, dynamic>.from(raw) : <String, dynamic>{};
                  final name = _s(it['drugName']).isNotEmpty ? _s(it['drugName']) : _s(it['genericName']);
                  final batch = _s(it['batchNo']);
                  final qty = _s(it['qty']);
                  return Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(
                      '${name.isEmpty ? '商品' : name}  ·  批号 ${batch.isEmpty ? '-' : batch}  ·  数量 $qty',
                      style: const TextStyle(fontSize: 13),
                    ),
                  );
                }),
            ],
          ],
        ),
      ),
    );
  }

  Widget _traceCard(Map<String, dynamic> t) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.qr_code_2, size: 18, color: Color(0xFF6B7280)),
                const SizedBox(width: 6),
                Expanded(
                  child: SelectableText(
                    _s(t['code']).isEmpty ? _s(t['spdid']) : _s(t['code']),
                    style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                  ),
                ),
                if (_s(t['packLevel']).isNotEmpty) _tag(_s(t['packLevel']), const Color(0xFF6B7280)),
              ],
            ),
            if (_s(t['drugName']).isNotEmpty) _metaRow('药品', _s(t['drugName'])),
            if (_s(t['batchNo']).isNotEmpty) _metaRow('批号', _s(t['batchNo'])),
            if (_s(t['bizType']).isNotEmpty) _metaRow('业务', _s(t['bizType'])),
            if (_s(t['invoiceNo']).isNotEmpty) _metaRow('发票', _s(t['invoiceNo'])),
            if (_s(t['orderNo']).isNotEmpty) _metaRow('单号', _s(t['orderNo'])),
          ],
        ),
      ),
    );
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
            width: 80,
            child: Text(label, style: const TextStyle(color: Color(0xFF8A8F99), fontSize: 12)),
          ),
          Expanded(child: SelectableText(value, style: const TextStyle(fontSize: 13))),
        ],
      ),
    );
  }

  Widget _emptyView() {
    return const Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.qr_code_scanner, size: 56, color: Color(0xFFC7CAD1)),
          SizedBox(height: 8),
          Text('扫描或输入后查询该批入库 / 出库'),
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
          Padding(padding: const EdgeInsets.symmetric(horizontal: 24), child: Text(_error ?? '')),
          const SizedBox(height: 12),
          OutlinedButton(onPressed: _query, child: const Text('重试')),
        ],
      ),
    );
  }
}
