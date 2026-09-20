import 'api_client.dart';

class Drug {
  Drug({
    required this.id,
    required this.code,
    required this.genericName,
    required this.tradeName,
    required this.spec,
    required this.unit,
    required this.manufacturer,
    required this.approvalNo,
    required this.barcode,
    required this.isColdChain,
    required this.rxType,
    required this.refPurchasePrice,
    required this.refSalePrice,
    required this.status,
    required this.category,
    required this.remark,
  });

  final num id;
  final String code;
  final String genericName;
  final String tradeName;
  final String spec;
  final String unit;
  final String manufacturer;
  final String approvalNo;
  final String barcode;
  final int isColdChain;
  final String rxType;
  final String refPurchasePrice;
  final String refSalePrice;
  final int status;
  final String category;
  final String remark;

  factory Drug.fromJson(Map<String, dynamic> j) => Drug(
        id: (j['id'] as num?) ?? 0,
        code: (j['code'] as String?) ?? '',
        genericName: (j['genericName'] as String?) ?? '',
        tradeName: (j['tradeName'] as String?) ?? '',
        spec: (j['spec'] as String?) ?? '',
        unit: (j['unit'] as String?) ?? '',
        manufacturer: (j['manufacturer'] as String?) ?? '',
        approvalNo: (j['approvalNo'] as String?) ?? '',
        barcode: (j['barcode'] as String?) ?? '',
        isColdChain: (j['isColdChain'] as num?)?.toInt() ?? 0,
        rxType: (j['rxType'] as String?) ?? '',
        refPurchasePrice: '${j['refPurchasePrice'] ?? ''}',
        refSalePrice: '${j['refSalePrice'] ?? ''}',
        status: (j['status'] as num?)?.toInt() ?? 1,
        category: (j['category'] as String?) ?? '',
        remark: (j['remark'] as String?) ?? '',
      );
}

class BatchStock {
  BatchStock({
    required this.id,
    required this.drugId,
    required this.warehouseId,
    required this.batchNo,
    required this.productionDate,
    required this.expireDate,
    required this.qty,
    required this.qualityStatus,
    required this.drugName,
    required this.drugSpec,
    required this.warehouseName,
  });

  final num id;
  final num drugId;
  final num warehouseId;
  final String batchNo;
  final String productionDate;
  final String expireDate;
  final String qty;
  final String qualityStatus;
  final String drugName;
  final String drugSpec;
  final String warehouseName;

  factory BatchStock.fromJson(Map<String, dynamic> j) => BatchStock(
        id: (j['id'] as num?) ?? 0,
        drugId: (j['drugId'] as num?) ?? 0,
        warehouseId: (j['warehouseId'] as num?) ?? 0,
        batchNo: (j['batchNo'] as String?) ?? '',
        productionDate: (j['productionDate'] as String?) ?? '',
        expireDate: (j['expireDate'] as String?) ?? '',
        qty: '${j['qty'] ?? ''}',
        qualityStatus: (j['qualityStatus'] as String?) ?? '',
        drugName: (j['drugName'] as String?) ?? '',
        drugSpec: (j['drugSpec'] as String?) ?? '',
        warehouseName: (j['warehouseName'] as String?) ?? '',
      );
}

class TraceCode {
  TraceCode({
    required this.id,
    required this.spdid,
    required this.code,
    required this.packLevel,
    required this.parentCode,
    required this.bizType,
    required this.status,
    required this.collectedAt,
    required this.remark,
    required this.invoiceNo,
    required this.orderNo,
    required this.drugName,
    required this.batchNo,
  });

  final num id;
  final String spdid;
  final String code;
  final String packLevel;
  final String parentCode;
  final String bizType;
  final String status;
  final String collectedAt;
  final String remark;
  final String invoiceNo;
  final String orderNo;
  final String drugName;
  final String batchNo;

  factory TraceCode.fromJson(Map<String, dynamic> j) => TraceCode(
        id: (j['id'] as num?) ?? 0,
        spdid: (j['spdid'] as String?) ?? '',
        code: (j['code'] as String?) ?? '',
        packLevel: (j['packLevel'] as String?) ?? '',
        parentCode: (j['parentCode'] as String?) ?? '',
        bizType: (j['bizType'] as String?) ?? '',
        status: (j['status'] as String?) ?? '',
        collectedAt: (j['collectedAt'] as String?) ?? '',
        remark: (j['remark'] as String?) ?? '',
        invoiceNo: (j['invoiceNo'] as String?) ?? '',
        orderNo: (j['orderNo'] as String?) ?? '',
        drugName: (j['drugName'] as String?) ?? '',
        batchNo: (j['batchNo'] as String?) ?? '',
      );
}

class SalesOrder {
  SalesOrder({
    required this.id,
    required this.orderNo,
    required this.invoiceNo,
    required this.orderType,
    required this.bizDate,
    required this.payType,
    required this.status,
    required this.totalAmount,
    required this.paidStatus,
    required this.paidAmount,
    required this.customerName,
    required this.warehouseName,
    required this.receiveStatus,
  });

  final num id;
  final String orderNo;
  final String invoiceNo;
  final String orderType;
  final String bizDate;
  final String payType;
  final String status;
  final String totalAmount;
  final String paidStatus;
  final String paidAmount;
  final String customerName;
  final String warehouseName;
  final String receiveStatus;

  factory SalesOrder.fromJson(Map<String, dynamic> j) => SalesOrder(
        id: (j['id'] as num?) ?? 0,
        orderNo: (j['orderNo'] as String?) ?? '',
        invoiceNo: (j['invoiceNo'] as String?) ?? '',
        orderType: (j['orderType'] as String?) ?? '',
        bizDate: (j['bizDate'] as String?) ?? '',
        payType: (j['payType'] as String?) ?? '',
        status: (j['status'] as String?) ?? '',
        totalAmount: '${j['totalAmount'] ?? ''}',
        paidStatus: (j['paidStatus'] as String?) ?? '',
        paidAmount: '${j['paidAmount'] ?? ''}',
        customerName: (j['customerName'] as String?) ?? '',
        warehouseName: (j['warehouseName'] as String?) ?? '',
        receiveStatus: (j['receiveStatus'] as String?) ?? '',
      );
}

class PageResult<T> {
  PageResult({required this.total, required this.rows});

  final num total;
  final List<T> rows;
}

class PharmaApi {
  final _client = ApiClient.instance;

  /// 药品档案分页
  Future<PageResult<Drug>> loadAllDrug({
    int page = 1,
    int limit = 20,
    String? genericName,
    String? tradeName,
    String? code,
    String? approvalNo,
    int? status,
    String? category,
  }) async {
    final q = <String, String>{
      'page': '$page',
      'limit': '$limit',
      if (genericName != null && genericName.isNotEmpty) 'genericName': genericName,
      if (tradeName != null && tradeName.isNotEmpty) 'tradeName': tradeName,
      if (code != null && code.isNotEmpty) 'code': code,
      if (approvalNo != null && approvalNo.isNotEmpty) 'approvalNo': approvalNo,
      if (status != null) 'status': '$status',
      if (category != null && category.isNotEmpty) 'category': category,
    };
    final json = await _client.get('/drug/loadAllDrug', q);
    final rows = (json['data'] as List?) ?? const [];
    return PageResult(
      total: (json['count'] as num?) ?? 0,
      rows: rows.map((e) => Drug.fromJson(e as Map<String, dynamic>)).toList(),
    );
  }

  /// 批号库存分页
  Future<PageResult<BatchStock>> loadAllBatchStock({
    int page = 1,
    int limit = 20,
    String? expireFilter,
    int? nearExpireDays,
    String? batchNo,
    String? drugName,
  }) async {
    final q = <String, String>{
      'page': '$page',
      'limit': '$limit',
      if (expireFilter != null && expireFilter.isNotEmpty) 'expireFilter': expireFilter,
      if (nearExpireDays != null) 'nearExpireDays': '$nearExpireDays',
      if (batchNo != null && batchNo.isNotEmpty) 'batchNo': batchNo,
      if (drugName != null && drugName.isNotEmpty) 'drugName': drugName,
    };
    final json = await _client.get('/batchStock/loadAllBatchStock', q);
    final rows = (json['data'] as List?) ?? const [];
    return PageResult(
      total: (json['count'] as num?) ?? 0,
      rows: rows.map((e) => BatchStock.fromJson(e as Map<String, dynamic>)).toList(),
    );
  }

  /// 统一查询：追溯码 / 批号 / 入出库单 / 发票
  Future<Map<String, dynamic>> lookupTrace(String keyword) async {
    final json = await _client.get('/trace/lookup', {'keyword': keyword});
    final data = json['data'];
    if (data is Map<String, dynamic>) return data;
    throw ApiException((json['msg'] as String?) ?? '未找到');
  }
  Future<({SalesOrder? order, List<TraceCode> traces})> loadInvoice(String invoiceNo) async {
    final json = await _client.get('/trace/loadInvoice', {'invoiceNo': invoiceNo});
    final orderJson = json['data'];
    final tracesJson = json['traces'] as List? ?? const [];
    return (
      order: orderJson is Map<String, dynamic> ? SalesOrder.fromJson(orderJson) : null,
      traces: tracesJson.map((e) => TraceCode.fromJson(e as Map<String, dynamic>)).toList(),
    );
  }

  /// 按 SPDID 查追溯码
  Future<List<TraceCode>> loadBySpdid(String spdid) async {
    final json = await _client.get('/trace/loadBySpdid', {'spdid': spdid});
    final rows = (json['data'] as List?) ?? const [];
    return rows.map((e) => TraceCode.fromJson(e as Map<String, dynamic>)).toList();
  }
}
