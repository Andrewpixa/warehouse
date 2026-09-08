# 药企进销存：ER 图、UML 与数据库选型

> 依据当前库 `pharma_ims`（B/S 已接入）整理。  
> 登录权限仍使用 `sys_*` 表；本文业务模型只覆盖药企进销存核心表。

---

## 1. 业务总览

```text
档案层
  岗位 / 员工 / 供应商 / 客户 / 药品 / 仓库

单据层
  进货单 + 进货明细  → 确认后增加批号库存
  出库单 + 出库明细  → 确认后扣减批号库存
                       （主表含发票号，明细含 SPDID）

库存与追溯
  批号库存（药品 + 仓库 + 批号 + 质量状态）
  追溯码（按明细 SPDID 挂多条码）
```

核心查询链：

```text
发票号(sales_orders.invoice_no)
  → 出库明细(sales_order_items.spdid)
  → 追溯码(trace_codes.code)
```

---

## 2. ER 图（概念 / 逻辑）

可直接粘贴到支持 Mermaid 的 Markdown / Typora / GitHub / VS Code 预览。  
下图实体名、字段名为中文（便于课设展示）；物理表名仍见第 4 节。

```mermaid
erDiagram
  岗位 ||--o{ 员工 : "1对多"
  员工 ||--o{ 仓库 : "负责人"
  供应商 ||--o{ 进货单 : "供货"
  客户 ||--o{ 出库单 : "购进"
  仓库 ||--o{ 进货单 : "入库仓"
  仓库 ||--o{ 出库单 : "出库仓"
  仓库 ||--o{ 批号库存 : "存放"
  药品 ||--o{ 进货单明细 : "进货行"
  药品 ||--o{ 出库单明细 : "出库行"
  药品 ||--o{ 批号库存 : "库存行"
  员工 ||--o{ 进货单 : "业务员/验收/保管/制单"
  员工 ||--o{ 出库单 : "业务员/复核/制单"
  进货单 ||--|{ 进货单明细 : "主从"
  出库单 ||--|{ 出库单明细 : "主从"
  出库单明细 ||--o{ 追溯码 : "SPDID逻辑关联"

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
    tinyint 状态
  }
  供应商 {
    bigint 编号 PK
    varchar 供应商编码 UK
    varchar 供应商名称
    tinyint 首营是否通过
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
  }
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
    decimal 入库数量
    varchar SPDID UK
  }
  出库单 {
    bigint 编号 PK
    varchar 单据号 UK
    varchar 发票号 UK
    bigint 客户编号 FK
    bigint 仓库编号 FK
    date 业务日期
    varchar 单据状态
    decimal 总金额
  }
  出库单明细 {
    bigint 编号 PK
    bigint 出库单编号 FK
    bigint 药品编号 FK
    varchar 批号
    decimal 出库数量
    varchar SPDID UK
  }
  批号库存 {
    bigint 编号 PK
    bigint 药品编号 FK
    bigint 仓库编号 FK
    varchar 批号
    decimal 库存数量
    varchar 质量状态
  }
  追溯码 {
    bigint 编号 PK
    varchar SPDID
    varchar 码值 UK
    varchar 包装层级
    varchar 业务类型
    varchar 状态
  }
```

### 2.1 关系说明（课设可直接写进文档）

| 关系 | 基数 | 说明 |
|---|---|---|
| 岗位 → 员工 | 1:N | 一个岗位多名员工 |
| 供应商 → 进货单 | 1:N | 一张进货单对应一个供应商 |
| 客户 → 出库单 | 1:N | 一张出库单对应一个客户 |
| 进货单 → 进货明细 | 1:N | 主从表 |
| 出库单 → 出库明细 | 1:N | 主从表；明细含 SPDID |
| 药品/仓库 → 批号库存 | N:M（通过库存表） | 唯一键：`drug_id + warehouse_id + batch_no + quality_status` |
| 出库明细 → 追溯码 | 1:N（逻辑） | 通过 `spdid` 关联；库中未建物理外键，便于入库侧也可挂码 |

### 2.2 库存唯一性（重要）

`batch_stocks` 唯一约束：

```text
uk_batch_stocks (drug_id, warehouse_id, batch_no, quality_status)
```

含义：**一物一批一库一质量状态 = 一条库存**。

---

## 3. UML

### 3.1 类图（领域模型，对应后端实体）

```mermaid
classDiagram
  class Position {
    +Long id
    +String name
    +Integer status
  }
  class Employee {
    +Long id
    +String empNo
    +String name
    +Long positionId
    +Integer status
  }
  class Supplier {
    +Long id
    +String code
    +String name
    +Integer firstCampOk
  }
  class Customer {
    +Long id
    +String code
    +String name
    +String customerType
  }
  class Drug {
    +Long id
    +String code
    +String genericName
    +String approvalNo
    +Integer isColdChain
  }
  class Warehouse {
    +Long id
    +String code
    +String name
    +String whType
    +Long managerId
  }
  class PurchaseOrder {
    +Long id
    +String orderNo
    +Long supplierId
    +Long warehouseId
    +LocalDate bizDate
    +String status
    +BigDecimal totalAmount
    +List~PurchaseOrderItem~ items
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
    +String orderNo
    +String invoiceNo
    +Long customerId
    +Long warehouseId
    +LocalDate bizDate
    +String status
    +BigDecimal totalAmount
    +List~SalesOrderItem~ items
    +saveDraft()
    +confirm()
  }
  class SalesOrderItem {
    +Long id
    +Long orderId
    +Long drugId
    +String batchNo
    +BigDecimal qty
    +String spdid
  }
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
  class TraceCode {
    +Long id
    +String spdid
    +String code
    +String packLevel
    +String bizType
    +String status
  }

  Position "1" --> "0..*" Employee
  Employee "1" --> "0..*" Warehouse : manager
  Supplier "1" --> "0..*" PurchaseOrder
  Customer "1" --> "0..*" SalesOrder
  Warehouse "1" --> "0..*" PurchaseOrder
  Warehouse "1" --> "0..*" SalesOrder
  Warehouse "1" --> "0..*" BatchStock
  Drug "1" --> "0..*" PurchaseOrderItem
  Drug "1" --> "0..*" SalesOrderItem
  Drug "1" --> "0..*" BatchStock
  PurchaseOrder "1" *-- "1..*" PurchaseOrderItem
  SalesOrder "1" *-- "1..*" SalesOrderItem
  SalesOrderItem "1" --> "0..*" TraceCode : spdid
```

### 3.2 用例图（课设演示范围）

```mermaid
flowchart LR
  Admin[管理员/仓管]
  Admin --> UC1[维护档案<br/>药品/客户/供应商/仓库]
  Admin --> UC2[开进货单并确认入库]
  Admin --> UC3[开出库单并确认出库]
  Admin --> UC4[查询批号库存]
  Admin --> UC5[按发票号查追溯码]
  Admin --> UC6[按 SPDID 采集追溯码]
```

### 3.3 时序图：进货确认 → 批号库存增加

```mermaid
sequenceDiagram
  actor User as 用户
  participant UI as 前端进货单页
  participant API as PurchaseOrderController
  participant Svc as PurchaseOrderService
  participant Stock as BatchStockService
  participant DB as MySQL(pharma_ims)

  User->>UI: 保存草稿 / 确认入库
  UI->>API: POST /purchase/savePurchase
  API->>Svc: saveDraft(order+items)
  Svc->>DB: 写 purchase_orders / items
  UI->>API: POST /purchase/confirmPurchase?id=
  API->>Svc: confirm(id)
  loop 每条明细
    Svc->>Stock: increase(drug,warehouse,batch,qty)
    Stock->>DB: upsert batch_stocks
  end
  Svc->>DB: status = 已确认
  API-->>UI: 确认成功
```

### 3.4 时序图：出库确认 + 发票号查追溯码

```mermaid
sequenceDiagram
  actor User as 用户
  participant UI as 前端出库/追溯页
  participant Out as SalesOutboundController
  participant Trace as TraceCodeController
  participant Svc as SalesOrderService
  participant Stock as BatchStockService
  participant DB as MySQL

  User->>UI: 保存出库草稿(含发票号)
  UI->>Out: POST /outbound/saveOutbound
  Out->>Svc: saveDraft(生成 SPDID)
  Svc->>DB: 写 sales_orders / items

  User->>UI: 确认出库
  UI->>Out: POST /outbound/confirmOutbound
  Out->>Svc: confirm(id)
  loop 每条明细
    Svc->>Stock: decrease(...)
    Stock->>DB: 扣减 batch_stocks
  end

  User->>UI: 输入发票号查询
  UI->>Out: GET /outbound/loadByInvoice
  Out->>DB: 查单 + 明细 + 追溯码
  Out-->>UI: 订单/明细/已采码

  User->>UI: 采集追溯码
  UI->>Trace: POST /trace/addCodes
  Trace->>DB: insert trace_codes(spdid, code)
```

### 3.5 状态图：单据状态

```mermaid
stateDiagram-v2
  [*] --> 草稿 : 新建/保存
  草稿 --> 已确认 : 确认过账\n(改库存)
  草稿 --> [*] : 删除草稿
  已确认 --> [*]
  note right of 已确认
    当前实现：已确认后不可再改。
    「已过账」预留给以后财务对接。
  end note
```

---

## 4. 表清单（与 Access / 课设文档对齐）

| 中文名 | MySQL 表 | 层级 |
|---|---|---|
| 岗位 | `positions` | 档案 |
| 员工 | `employees` | 档案 |
| 供应商 | `suppliers` | 档案 |
| 客户 | `customers` | 档案 |
| 药品 | `drugs` | 档案 |
| 仓库 | `warehouses` | 档案 |
| 进货单 | `purchase_orders` | 单据 |
| 进货单明细 | `purchase_order_items` | 单据 |
| 出库单 | `sales_orders` | 单据 |
| 出库单明细 | `sales_order_items` | 单据 |
| 批号库存 | `batch_stocks` | 库存 |
| 追溯码 | `trace_codes` | 追溯 |

另：B/S 登录菜单权限使用 `sys_user / sys_role / sys_permission ...`，与业务库同库但模型独立。

---

## 5. 后面转 SQL Server 还是继续 MySQL？

### 结论（针对你这个项目）

**继续用 MySQL 作为主库；不要为了“看起来更企业”而现在转 SQL Server。**

只有下面情况才值得转 / 并行 SQL Server：

1. 老师/课设评分**明确要求** SQL Server  
2. 实习单位正式环境就是 SQL Server，你要做**对接演示**  
3. Access 只能用学校机房的 SQL Server ODBC，而连不上 MySQL  

除此之外，**MySQL 更合适**。

### 对比（按你的真实约束）

| 维度 | MySQL（推荐） | SQL Server |
|---|---|---|
| 与当前 Spring Boot 项目 | 已接通 `pharma_ims`，零迁移成本 | 要改驱动、方言、部分 SQL、部署 |
| Mac 本机开发 | 顺畅 | 本机装/连更麻烦，常靠远程/Docker |
| Access ODBC | 可行（需装 MySQL ODBC） | 学校 Windows 环境往往更现成 |
| 课设 811/812 加分 | 够用；B/S + 追溯码更有辨识度 | 若老师强调“微软技术栈”才更有分 |
| 药企实习常见度 | 常见 | 也很常见（尤其老系统/集团） |
| 迁移难度 | — | 表结构可迁，应用层要回归测试 |

### 建议策略

```text
现在～交课设
  主库 = MySQL（你已经在跑）
  Access = 同结构原型 / 少量窗体演示（或 ODBC 链 MySQL）

以后如果老师/实习强制 SQL Server
  1) 先冻结表结构（本文 ER）
  2) 用同名字段迁到 SQL Server
  3) 只改 application.yml 数据源 + 少量 SQL 方言
  4) 业务代码尽量不动（MyBatis-Plus 迁移成本可控）
```

### 一句话答辩口径

> 业务模型按药企进销存设计，数据库选型以可落地为准：当前 B/S 与开发环境统一使用 MySQL；若部署环境要求 SQL Server，表结构可平滑迁移，应用层通过更换数据源适配，不推翻领域模型。

---

## 6. 画图工具建议（交作业用）

1. **Typora / VS Code Mermaid 预览**：直接渲染本文图  
2. **[mermaid.live](https://mermaid.live)**：导出 PNG/SVG 贴进 Word/PPT  
3. **ProcessOn / draw.io**：把 ER 实体手工重画一版（部分老师更认“传统 ER 符号”）  
4. Access：关系图窗口可再画一份中文表名版，和本文对照

---

## 7. 修订记录

| 日期 | 说明 |
|---|---|
| 2026-09-03 | 初版：对齐 `pharma_ims` 现网表结构与 B/S 已实现链路 |
| 2026-09-08 | ER 图实体名、字段名改为中文展示；物理表名仍用英文 |
