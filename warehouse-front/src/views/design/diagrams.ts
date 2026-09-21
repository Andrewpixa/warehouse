export const architecture = `flowchart TB
  subgraph clients [客户端]
    Web[Vue3 Web]
    Mini[uni-app X 小程序]
    App[Flutter App]
    Desk[Electron 桌面]
  end
  subgraph access [接入层]
    Nginx[Nginx 静态站点与反代]
  end
  subgraph backend [应用层 Spring Boot]
    Ctrl[Controller]
    Svc[Service]
    Aop[SA-Token 权限与操作日志]
    Mp[MyBatis-Plus]
  end
  DB[(MySQL pharma_ims)]
  Web --> Nginx
  Mini --> Nginx
  App --> Nginx
  Desk --> Nginx
  Nginx -->|/warehouse| Ctrl
  Ctrl --> Svc
  Svc --> Aop
  Svc --> Mp
  Mp --> DB`

export const deploy = `flowchart LR
  Browser[浏览器或手机] --> Nginx
  Nginx -->|location /| Dist[前端 dist]
  Nginx -->|location /warehouse| Jar[Spring Boot :8899]
  Jar --> Mysql[(MySQL)]
  Ops[备份巡检脚本] -.-> Mysql
  Ops -.-> Jar`

export const usecaseSystem = `flowchart TB
  Admin[管理员]
  Staff[所有用户]
  Buyer[采购]
  Sales[销售]
  Keeper[仓管]
  subgraph sys [系统与权限]
    UC01[UC01 登录系统]
    UC02[UC02 退出登录]
    UC03[UC03 修改本人密码]
    UC04[UC04 维护个人信息]
    UC05[UC05 维护用户账号]
    UC06[UC06 分配角色权限]
    UC07[UC07 维护部门]
    UC08[UC08 发布公告]
    UC09[UC09 登录日志]
    UC10[UC10 操作日志]
  end
  subgraph master [基础档案]
    UC11[UC11 维护药品]
    UC12[UC12 维护客户]
    UC13[UC13 维护供应商]
    UC14[UC14 维护仓库]
  end
  Staff --> UC01
  Staff --> UC02
  Staff --> UC03
  Staff --> UC04
  Admin --> UC05
  Admin --> UC06
  Admin --> UC07
  Admin --> UC08
  Admin --> UC09
  Admin --> UC10
  Buyer --> UC11
  Sales --> UC12
  Buyer --> UC13
  Keeper --> UC14
  Admin --> UC11
  Admin --> UC12
  Admin --> UC13
  Admin --> UC14`

export const usecaseTrade = `flowchart TB
  Buyer[采购]
  Keeper[仓管]
  Sales[销售]
  Finance[财务]
  Hospital[医院收货]
  subgraph inbound [采购入库]
    UC15[UC15 开进货草稿]
    UC16[UC16 确认入库]
    UC17[UC17 删除进货草稿]
    UC18[UC18 查询进货发票]
  end
  subgraph outbound [销售出库]
    UC19[UC19 开出库草稿]
    UC20[UC20 确认出库]
    UC21[UC21 红冲出库]
    UC22[UC22 标记回款]
    UC23[UC23 医院确认收货]
    UC24[UC24 查询单位欠款]
  end
  Buyer --> UC15
  Buyer --> UC17
  Buyer --> UC18
  Keeper --> UC16
  Sales --> UC19
  Keeper --> UC20
  Sales --> UC21
  Finance --> UC22
  Sales --> UC22
  Hospital --> UC23
  Finance --> UC24
  Finance --> UC18`

export const usecaseStock = `flowchart TB
  Keeper[仓管]
  Finance[财务]
  Sales[销售]
  Admin[管理员]
  subgraph ware [仓储]
    UC25[UC25 查询批号库存]
    UC26[UC26 批号升益]
    UC27[UC27 库存盘点]
    UC28[UC28 跨仓调拨]
    UC29[UC29 查询分仓库存]
  end
  subgraph trace [追溯]
    UC30[UC30 发票或码查询]
    UC31[UC31 扫码采集纠错]
    UC32[UC32 大码解析]
  end
  subgraph close [结账与报表]
    UC33[UC33 日清检查]
    UC34[UC34 月结对账]
    UC35[UC35 进销统计报表]
    UC36[UC36 厂家流向]
  end
  Keeper --> UC25
  Keeper --> UC26
  Keeper --> UC27
  Keeper --> UC28
  Keeper --> UC29
  Keeper --> UC30
  Keeper --> UC31
  Keeper --> UC32
  Keeper --> UC33
  Finance --> UC25
  Finance --> UC34
  Finance --> UC35
  Sales --> UC30
  Sales --> UC36
  Admin --> UC33
  Admin --> UC34
  Admin --> UC35`

export const erOverview = `erDiagram
  岗位 ||--o{ 员工 : "1对多"
  员工 ||--o{ 仓库 : "负责人"
  供应商 ||--o{ 进货单 : "供货"
  客户 ||--o{ 出库单 : "购进"
  仓库 ||--o{ 进货单 : "入库仓"
  仓库 ||--o{ 出库单 : "出库仓"
  仓库 ||--o{ 批号库存 : "存放"
  仓库 ||--o{ 升益单 : "盘点仓"
  药品 ||--o{ 进货单明细 : "进货行"
  药品 ||--o{ 出库单明细 : "出库行"
  药品 ||--o{ 批号库存 : "库存行"
  药品 ||--o{ 升益单明细 : "升益行"
  员工 ||--o{ 进货单 : "业务员验收保管"
  员工 ||--o{ 出库单 : "业务员复核"
  进货单 ||--|{ 进货单明细 : "主从"
  出库单 ||--|{ 出库单明细 : "主从"
  升益单 ||--|{ 升益单明细 : "主从"
  出库单明细 ||--o{ 追溯码 : "SPDID"
  追溯码 ||--o{ 包装关联 : "上级码"
  包装关联 }o--|| 追溯码 : "下级码"`

export const erMaster = `erDiagram
  岗位 ||--o{ 员工 : "任职"
  员工 ||--o{ 仓库 : "负责人"
  岗位 {
    bigint 编号 PK
    varchar 名称
    tinyint 状态
  }
  员工 {
    bigint 编号 PK
    varchar 工号 UK
    varchar 姓名
    bigint 岗位编号 FK
    varchar 手机
    tinyint 状态
  }
  供应商 {
    bigint 编号 PK
    varchar 供应商编码 UK
    varchar 供应商名称
    varchar 许可证号
    tinyint 状态
  }
  客户 {
    bigint 编号 PK
    varchar 客户编码 UK
    varchar 客户名称
    varchar 客户类型
    tinyint 状态
  }
  药品 {
    bigint 编号 PK
    varchar 药品编码 UK
    varchar 通用名
    varchar 批准文号
    tinyint 是否冷链
    tinyint 状态
  }
  仓库 {
    bigint 编号 PK
    varchar 仓库编码 UK
    varchar 仓库名称
    varchar 仓库类型
    bigint 负责人编号 FK
    tinyint 状态
  }`

export const erIn = `erDiagram
  供应商 ||--o{ 进货单 : "供货"
  仓库 ||--o{ 进货单 : "入库仓"
  进货单 ||--|{ 进货单明细 : "主从"
  药品 ||--o{ 进货单明细 : "进货行"
  进货单 {
    bigint 编号 PK
    varchar 单据号 UK
    bigint 供应商编号 FK
    bigint 仓库编号 FK
    date 业务日期
    varchar 单据状态
    decimal 总金额
  }
  进货单明细 {
    bigint 编号 PK
    bigint 进货单编号 FK
    bigint 药品编号 FK
    varchar 批号
    date 有效期至
    decimal 入库数量
    decimal 进价
    varchar SPDID UK
  }`

export const erOut = `erDiagram
  客户 ||--o{ 出库单 : "购进"
  仓库 ||--o{ 出库单 : "出库仓"
  出库单 ||--|{ 出库单明细 : "主从"
  药品 ||--o{ 出库单明细 : "出库行"
  出库单 {
    bigint 编号 PK
    varchar 单据号 UK
    varchar 发票号 UK
    varchar 单据类型
    varchar 原蓝字发票号
    bigint 客户编号 FK
    varchar 单据状态
    varchar 回款状态
    varchar 收货状态
  }
  出库单明细 {
    bigint 编号 PK
    bigint 出库单编号 FK
    bigint 药品编号 FK
    varchar 批号
    decimal 出库数量
    varchar SPDID UK
  }`

export const erStock = `erDiagram
  药品 ||--o{ 批号库存 : "库存行"
  仓库 ||--o{ 批号库存 : "存放"
  仓库 ||--o{ 升益单 : "盘点仓"
  升益单 ||--|{ 升益单明细 : "主从"
  药品 ||--o{ 升益单明细 : "升益行"
  批号库存 {
    bigint 编号 PK
    bigint 药品编号 FK
    bigint 仓库编号 FK
    varchar 批号
    date 有效期至
    decimal 库存数量
    varchar 质量状态
  }
  升益单 {
    bigint 编号 PK
    varchar 单据号 UK
    bigint 仓库编号 FK
    varchar 单据状态
  }
  升益单明细 {
    bigint 编号 PK
    bigint 升益单编号 FK
    bigint 药品编号 FK
    decimal 账面数量
    decimal 实盘数量
  }`

export const erTrace = `erDiagram
  出库单明细 ||--o{ 追溯码 : "SPDID"
  进货单明细 ||--o{ 追溯码 : "SPDID"
  追溯码 ||--o{ 包装关联 : "上级码"
  包装关联 }o--|| 追溯码 : "下级码"
  追溯码 {
    bigint 编号 PK
    varchar SPDID
    varchar 码值 UK
    varchar 包装层级
    varchar 业务类型
    varchar 状态
  }
  包装关联 {
    bigint 编号 PK
    varchar 上级码
    varchar 下级码 UK
  }`

export const erClose = `erDiagram
  日清记录 {
    bigint 编号 PK
    date 业务日期 UK
    varchar 状态
    int 进货单数
    decimal 进货金额
    int 出库单数
    decimal 出库金额
  }
  月结记录 {
    bigint 编号 PK
    char 结账月份 UK
    varchar 状态
    decimal 销售金额
    decimal 未回款金额
    decimal 采购金额
  }`

export const classMaster = `classDiagram
  class Supplier {
    +Long id
    +String code
    +String name
    +String licenseNo
    +Integer status
  }
  class Customer {
    +Long id
    +String code
    +String name
    +String customerType
    +Integer status
  }
  class Drug {
    +Long id
    +String code
    +String genericName
    +String approvalNo
    +Integer isColdChain
    +Integer status
  }
  class Warehouse {
    +Long id
    +String code
    +String name
    +String whType
    +Long managerId
  }
  class Employee {
    +Long id
    +String empNo
    +String name
    +Long positionId
  }
  class Position {
    +Long id
    +String name
    +Integer status
  }
  Position "1" --> "0..*" Employee
  Employee "1" --> "0..*" Warehouse : manager`

export const classOrder = `classDiagram
  class PurchaseOrder {
    +Long id
    +String orderNo
    +Long supplierId
    +Long warehouseId
    +String status
    +saveDraft()
    +confirm()
  }
  class PurchaseOrderItem {
    +Long id
    +Long orderId
    +Long drugId
    +String batchNo
    +BigDecimal stockInQty
    +String spdid
  }
  class SalesOrder {
    +Long id
    +String invoiceNo
    +String orderType
    +String paidStatus
    +String receiveStatus
    +saveDraft()
    +confirm()
    +markPaid()
    +confirmReceipt()
    +saveReversal()
  }
  class SalesOrderItem {
    +Long id
    +Long orderId
    +Long drugId
    +String batchNo
    +BigDecimal qty
    +String spdid
  }
  PurchaseOrder "1" *-- "1..*" PurchaseOrderItem
  SalesOrder "1" *-- "1..*" SalesOrderItem`

export const classStock = `classDiagram
  class BatchStock {
    +Long id
    +Long drugId
    +Long warehouseId
    +String batchNo
    +BigDecimal qty
    +String qualityStatus
    +increase()
    +decrease()
  }
  class SurplusOrder {
    +Long id
    +String orderNo
    +String status
    +saveDraft()
    +confirm()
  }
  class SurplusOrderItem {
    +Long id
    +Long orderId
    +BigDecimal bookQty
    +BigDecimal actualQty
  }
  class TraceCode {
    +Long id
    +String spdid
    +String code
    +String packLevel
    +String status
  }
  class DailyCloseRecord {
    +LocalDate bizDate
    +String status
  }
  class MonthlyCloseRecord {
    +String yearMonth
    +String status
  }
  SurplusOrder "1" *-- "1..*" SurplusOrderItem
  SalesOrderItem "1" --> "0..*" TraceCode : spdid`

export const seqPurchase = `sequenceDiagram
  actor User as 仓管
  participant UI as 前端进货单
  participant API as PurchaseOrderController
  participant Svc as PurchaseOrderService
  participant Stock as BatchStockService
  participant DB as MySQL
  User->>UI: 保存草稿
  UI->>API: POST /purchase/savePurchase
  API->>Svc: saveDraft
  Svc->>DB: 写进货单与明细
  User->>UI: 确认入库
  UI->>API: POST /purchase/confirmPurchase
  API->>Svc: confirm
  loop 每条明细
    Svc->>Stock: increase 批号库存
    Stock->>DB: upsert batch_stocks
  end
  Svc->>DB: status 已确认
  API-->>UI: 成功`

export const seqOutbound = `sequenceDiagram
  actor User as 仓管或销售
  participant UI as 前端出库或追溯页
  participant Out as SalesOutboundController
  participant Trace as TraceCodeController
  participant Svc as SalesOrderService
  participant Stock as BatchStockService
  participant DB as MySQL
  User->>UI: 保存出库草稿含发票号
  UI->>Out: POST /outbound/saveOutbound
  Out->>Svc: saveDraft 生成 SPDID
  Svc->>DB: 写出库单与明细
  User->>UI: 确认出库
  UI->>Out: POST /outbound/confirmOutbound
  loop 每条明细
    Svc->>Stock: decrease
    Stock->>DB: 扣减 batch_stocks
  end
  User->>UI: 扫码或输入发票号
  UI->>Trace: POST /trace/addCodes
  Trace->>DB: insert trace_codes`

export const seqReversal = `sequenceDiagram
  actor User as 销售
  participant UI as 前端红冲
  participant Out as SalesOutboundController
  participant Svc as SalesOrderService
  participant Stock as BatchStockService
  participant DB as MySQL
  User->>UI: 按原发票开红冲单
  UI->>Out: POST /outbound/saveReversal
  Svc->>DB: 新出库单 order_type 红冲
  User->>UI: 确认红冲
  UI->>Out: POST /outbound/confirmOutbound
  loop 每条明细
    Svc->>Stock: increase 加回批号库存
  end
  Note over Svc: 不修改原蓝字出库单`

export const seqReceipt = `sequenceDiagram
  actor H as 医院
  participant UI as 收货页
  participant Out as SalesOutboundController
  participant DB as MySQL
  H->>UI: 打开待收货
  UI->>Out: GET /outbound/loadPendingReceipt
  Out-->>UI: 待收货列表
  H->>UI: 签收或部分签收
  UI->>Out: POST /outbound/confirmReceipt
  Out->>DB: 回写 receive_status
  Note over Out: 重复提交拒绝`

export const seqLogin = `sequenceDiagram
  actor U as 用户
  participant UI as 登录页
  participant API as LoginController
  participant DB as MySQL
  U->>UI: 打开登录
  UI->>API: GET 验证码
  API-->>UI: 验证码图
  U->>UI: 账号密码验证码
  UI->>API: POST /login/login
  API->>DB: 校验用户与一次性验证码
  alt 通过
    API-->>UI: 建立会话
    UI->>API: GET /login/currentUser
    API-->>UI: 用户菜单权限
  else 失败
    API-->>UI: 错误并刷新验证码
  end`

export const stateOrder = `stateDiagram-v2
  [*] --> 草稿 : 新建或保存
  草稿 --> 已确认 : 确认过账改库存
  草稿 --> [*] : 删除草稿
  已确认 --> [*]
  note right of 已确认
    已确认后不可再改账。
    红冲另开新单。
  end note`

export const statePaid = `stateDiagram-v2
  [*] --> 未回款 : 出库确认默认
  未回款 --> 部分回款 : 回款未满额
  未回款 --> 已回款 : 回款满额
  部分回款 --> 已回款 : 补齐
  部分回款 --> 部分回款 : 再次部分回款`

export const stateReceipt = `stateDiagram-v2
  [*] --> 待收货 : 正常出库已确认
  待收货 --> 已签收 : 实收等于出库
  待收货 --> 部分签收 : 实收不足
  待收货 --> 拒收 : 整单拒收`

export const stateTrace = `stateDiagram-v2
  [*] --> 正常 : 采集或解析写入
  正常 --> 作废 : 错码作废`

export const techSelect = `flowchart TB
  subgraph chosen [已选定]
    Java[Java 21]
    Boot[Spring Boot 3]
    Vue[Vue 3 + TS]
    Mysql[MySQL 8]
    Token[SA-Token]
    Uni[uni-app X]
  end
  subgraph reason [原因]
    R1[课程与开源底座一致]
    R2[前后端分离便于答辩]
    R3[本机与演示环境好装]
  end
  Java --> R1
  Boot --> R1
  Vue --> R2
  Token --> R2
  Mysql --> R3
  Uni --> R2`
