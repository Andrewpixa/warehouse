import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../ui/layout.dart';

class OpsPage extends StatefulWidget {
  const OpsPage({
    super.key,
    required this.docType,
    required this.title,
    this.embedded = false,
  });

  final String docType;
  final String title;
  final bool embedded;

  @override
  State<OpsPage> createState() => _OpsPageState();
}

class _OpsCfg {
  const _OpsCfg({
    required this.action,
    required this.hint,
    required this.done,
  });
  final String action;
  final String hint;
  final Set<String> done;
}

const _opsCfg = <String, _OpsCfg>{
  'STOCKOUT': _OpsCfg(action: '标记已补货', hint: '缺货登记后，采购补货完成再点此推进。', done: {'已补货'}),
  'INBOUND_EX': _OpsCfg(action: '推进处理', hint: '到货异常：待处理 → 待复检 → 合格/拒收。', done: {'合格', '拒收'}),
  'RETURN_NOTICE': _OpsCfg(action: '推进（签/放/入）', hint: '销退通知：草稿 → 已签名 → 已释放 → 已入库。', done: {'已入库'}),
  'OFFSET': _OpsCfg(action: '确认冲账', hint: '勾兑银行到账与发票，财务确认后才改已回款。', done: {'已冲账'}),
  'ALLOCATE': _OpsCfg(action: '分货完成', hint: '分货完成后开票员才能开票。', done: {'已分货'}),
  'LOGISTICS': _OpsCfg(action: '运营同意', hint: '超件数预约、特殊送货需运营同意。', done: {'已同意'}),
  'CREDIT': _OpsCfg(
    action: '生效',
    hint: '客户开票额度。草稿点「生效」后，开票才按该额度校验；已生效无需再点。',
    done: {'已生效'},
  ),
  'QUOTA': _OpsCfg(action: '生效', hint: '库容统筹值草稿生效后，才能作为入库上限依据。', done: {'已生效'}),
};

class _OpsPageState extends State<OpsPage> {
  List<Map<String, dynamic>> _rows = [];
  String? _error;
  bool _loading = true;

  _OpsCfg get _cfg =>
      _opsCfg[widget.docType] ??
      const _OpsCfg(action: '推进', hint: '按流程把单据推到下一状态。', done: {});

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final json = await ApiClient.instance.get('/ops/loadAll', {
        'page': '1',
        'limit': '50',
        'docType': widget.docType,
      });
      final rows = (json['data'] as List?) ?? const [];
      _rows = rows.map((e) => Map<String, dynamic>.from(e as Map)).toList();
    } catch (e) {
      _error = e.toString();
    }
    if (mounted) setState(() => _loading = false);
  }

  Future<void> _confirm(num id) async {
    try {
      await ApiClient.instance.postForm('/ops/confirm', {'id': '$id'});
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('已${_cfg.action}')));
      await _load();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: widget.embedded ? null : AppBar(title: Text(widget.title)),
      body: Column(
        children: [
          Container(
            width: double.infinity,
            color: const Color(0xFFFFF7ED),
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
            child: Text(_cfg.hint, style: const TextStyle(fontSize: 14, color: Color(0xFF9A3412), height: 1.45)),
          ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? Center(child: Text(_error!))
                    : RefreshIndicator(
                        onRefresh: _load,
                        child: AdaptiveCardGrid(
                          itemCount: _rows.length,
                          rowHeight: 108,
                          controller: null,
                          itemBuilder: (_, i) {
                            final r = _rows[i];
                            final status = '${r['status'] ?? ''}';
                            final done = _cfg.done.contains(status);
                            return Card(
                              child: ListTile(
                                contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                                title: Text('${r['title'] ?? ''}'),
                                subtitle: Text('${r['docNo'] ?? ''}  $status'),
                                trailing: done
                                    ? Text(status, style: const TextStyle(color: Color(0xFF16A34A), fontSize: 13))
                                    : FilledButton.tonal(
                                        onPressed: () => _confirm(r['id'] as num),
                                        child: Text(_cfg.action),
                                      ),
                              ),
                            );
                          },
                        ),
                      ),
          ),
        ],
      ),
    );
  }
}
