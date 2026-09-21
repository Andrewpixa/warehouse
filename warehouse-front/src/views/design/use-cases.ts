export type UseCase = {
  id: string
  name: string
  actor: string
  module: string
  summary: string
}

export const useCases: UseCase[] = [
  { id: 'UC01', name: '登录系统', actor: '所有用户', module: '系统', summary: '输入账号、密码与验证码，服务端校验后建立会话。' },
  { id: 'UC02', name: '退出登录', actor: '所有用户', module: '系统', summary: '注销会话并回到介绍页或登录页。' },
  { id: 'UC03', name: '修改本人密码', actor: '所有用户', module: '系统', summary: '本人改密，禁止改他人密码。' },
  { id: 'UC04', name: '维护个人信息', actor: '所有用户', module: '系统', summary: '查看和修改本人资料，敏感字段由服务端过滤。' },
  { id: 'UC05', name: '维护用户账号', actor: '管理员', module: '权限', summary: '增删改用户、分配角色、停用账号。' },
  { id: 'UC06', name: '分配角色与菜单权限', actor: '管理员', module: '权限', summary: '配置角色菜单与按钮权限，接口按 view/create/update/delete 校验。' },
  { id: 'UC07', name: '维护部门', actor: '管理员', module: '权限', summary: '维护树形部门，作为人员归属。' },
  { id: 'UC08', name: '发布系统公告', actor: '管理员', module: '系统', summary: '发布、修改、下架公告，工作台展示。' },
  { id: 'UC09', name: '查看登录日志', actor: '管理员', module: '系统', summary: '按时间、账号查询登录审计。' },
  { id: 'UC10', name: '查看操作日志', actor: '管理员', module: '系统', summary: '按模块、操作人、时间查询业务操作痕迹。' },
  { id: 'UC11', name: '维护药品档案', actor: '采购 / 管理员', module: '档案', summary: '维护品种编码、规格、批准文号、条码、冷链与处方分类。' },
  { id: 'UC12', name: '维护客户档案', actor: '销售 / 管理员', module: '档案', summary: '维护医院、药店、诊所客户与证照。' },
  { id: 'UC13', name: '维护供应商档案', actor: '采购 / 管理员', module: '档案', summary: '维护供应商、许可证、首营状态。' },
  { id: 'UC14', name: '维护仓库档案', actor: '仓管 / 管理员', module: '档案', summary: '维护合格仓、待验仓、退货仓及负责人。' },
  { id: 'UC15', name: '开进货草稿', actor: '采购', module: '进货', summary: '按供应商、仓库、批号、效期开进货单，不改库存。' },
  { id: 'UC16', name: '确认入库', actor: '仓管', module: '进货', summary: '验收合格后确认，按入库数量增加批号库存。' },
  { id: 'UC17', name: '删除进货草稿', actor: '采购', module: '进货', summary: '未确认的进货单可删除，已确认不可改账。' },
  { id: 'UC18', name: '查询进货与供应商发票', actor: '采购 / 财务', module: '进货', summary: '按单号、供应商、日期查询进货与发票。' },
  { id: 'UC19', name: '开出库草稿', actor: '销售', module: '出库', summary: '指定客户、仓库、批号与发票号，生成 SPDID，不扣库存。' },
  { id: 'UC20', name: '确认出库', actor: '仓管', module: '出库', summary: '确认后按批号扣库存，库存不足拒绝。' },
  { id: 'UC21', name: '红冲出库', actor: '销售', module: '出库', summary: '按原蓝字发票另开红冲单，确认后加回批号库存。' },
  { id: 'UC22', name: '标记回款', actor: '财务 / 销售', module: '出库', summary: '按发票累计已回金额，形成未回、部分、已回三态。' },
  { id: 'UC23', name: '医院确认收货', actor: '医院收货', module: '出库', summary: '待收货单签收、部分签收或拒收，重复提交拒绝。' },
  { id: 'UC24', name: '查询单位欠款', actor: '财务 / 销售', module: '出库', summary: '按客户查看未回款与部分回款出库单。' },
  { id: 'UC25', name: '查询批号库存', actor: '仓管 / 财务', module: '仓储', summary: '按药品、仓库、批号、质量状态查数量与效期。' },
  { id: 'UC26', name: '批号升益', actor: '仓管', module: '仓储', summary: '按账面与实盘差额调整批号库存。' },
  { id: 'UC27', name: '库存盘点', actor: '仓管', module: '仓储', summary: '建盘点单、录入实盘、按差异量调整库存。' },
  { id: 'UC28', name: '跨仓调拨', actor: '仓管', module: '仓储', summary: '发出、收货、取消，在途取消回补源仓。' },
  { id: 'UC29', name: '查询分仓库存', actor: '仓管', module: '仓储', summary: '按商品乘仓库查看分仓库存与预警。' },
  { id: 'UC30', name: '按发票或追溯码查询', actor: '仓管 / 销售', module: '追溯', summary: '发票号看整单码与流向，SPDID 或码值看单品。' },
  { id: 'UC31', name: '扫码采集或纠错', actor: '仓管', module: '追溯', summary: '摄像头或扫码枪录入、替换异常码。' },
  { id: 'UC32', name: '大码解析包装树', actor: '仓管', module: '追溯', summary: '大包装解析为中包、小盒并写入关联。' },
  { id: 'UC33', name: '日清检查', actor: '仓管 / 管理员', module: '结账', summary: '按日核对进货、出库笔数与金额并确认日清。' },
  { id: 'UC34', name: '月结对账', actor: '财务 / 管理员', module: '结账', summary: '按月汇总销售、采购、未回款并确认月结。' },
  { id: 'UC35', name: '查看进销统计报表', actor: '财务 / 管理员', module: '报表', summary: '进货统计、出货统计、金额/商品/利润分析。' },
  { id: 'UC36', name: '查看厂家流向', actor: '销售 / 管理员', module: '报表', summary: '按客户、品种、发票查看已确认出库流向。' }
]

export type CatalogItem = {
  id: string
  name: string
  entry: string
}

export const queries: CatalogItem[] = [
  { id: 'Q01', name: '用户列表查询', entry: '人资 / 用户' },
  { id: 'Q02', name: '角色权限查询', entry: '人资 / 角色' },
  { id: 'Q03', name: '登录日志查询', entry: '系统 / 登录日志' },
  { id: 'Q04', name: '操作日志查询', entry: '系统 / 操作日志' },
  { id: 'Q05', name: '药品档案查询', entry: '档案 / 药品' },
  { id: 'Q06', name: '客户档案查询', entry: '档案 / 客户' },
  { id: 'Q07', name: '供应商档案查询', entry: '档案 / 供应商' },
  { id: 'Q08', name: '仓库档案查询', entry: '档案 / 仓库' },
  { id: 'Q09', name: '进货单查询', entry: '进货单' },
  { id: 'Q10', name: '供应商发票查询', entry: '进货单（发票号）' },
  { id: 'Q11', name: '出库单查询', entry: '销售出库单' },
  { id: 'Q12', name: '待收货查询', entry: '医院收货确认' },
  { id: 'Q13', name: '单位欠款查询', entry: '单位欠款' },
  { id: 'Q14', name: '批号库存查询', entry: '批号库存' },
  { id: 'Q15', name: '近效期预警查询', entry: '批号库存（效期）' },
  { id: 'Q16', name: '分仓库存查询', entry: '分仓库存' },
  { id: 'Q17', name: '盘点单查询', entry: '盘点管理' },
  { id: 'Q18', name: '调拨单查询', entry: '跨仓调拨' },
  { id: 'Q19', name: '追溯码/发票号查询', entry: '追溯码查询' },
  { id: 'Q20', name: '大码包装树查询', entry: '大码解析' },
  { id: 'Q21', name: '日清检查清单', entry: '日清检查' },
  { id: 'Q22', name: '月结对账单', entry: '月结对账' },
  { id: 'Q23', name: '厂家流向查询', entry: '厂家流向' },
  { id: 'Q24', name: '缺货补货查询', entry: '缺货补货' },
  { id: 'Q25', name: '到货异常查询', entry: '到货异常' },
  { id: 'Q26', name: '销退通知查询', entry: '销退通知' },
  { id: 'Q27', name: '银行到账查询', entry: '银行到账' },
  { id: 'Q28', name: '出库打印包查询', entry: '出库打印包' }
]

export const reports: CatalogItem[] = [
  { id: 'R01', name: '进销金额分析', entry: '进销金额分析' },
  { id: 'R02', name: '进销商品分析', entry: '进销商品分析' },
  { id: 'R03', name: '利润分析', entry: '利润分析' },
  { id: 'R04', name: '进货统计', entry: '进货统计' },
  { id: 'R05', name: '出货统计', entry: '出货统计' },
  { id: 'R06', name: '绩效排名', entry: '业绩排名' },
  { id: 'R07', name: '店员提成', entry: '店员提成' },
  { id: 'R08', name: '日清检查表', entry: '日清检查' },
  { id: 'R09', name: '月结对账表', entry: '月结对账' },
  { id: 'R10', name: '厂家流向报表', entry: '厂家流向' },
  { id: 'R11', name: '单位欠款报表', entry: '单位欠款' },
  { id: 'R12', name: '近效期预警报表', entry: '批号库存' },
  { id: 'R13', name: '分仓库存报表', entry: '分仓库存' },
  { id: 'R14', name: '批号库存报表', entry: '批号库存' },
  { id: 'R15', name: '首页经营看板', entry: '首页' },
  { id: 'R16', name: '收货确认汇总', entry: '医院收货确认' },
  { id: 'R17', name: '红冲出库汇总', entry: '销售出库单（红冲）' },
  { id: 'R18', name: '银行到账汇总', entry: '银行到账' },
  { id: 'R19', name: '缺货补货汇总', entry: '缺货补货' },
  { id: 'R20', name: '信誉额/统筹值一览', entry: '信誉额、统筹值' }
]
