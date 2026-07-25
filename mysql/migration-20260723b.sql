-- 2026-07-23 业务模块操作级权限码补全迁移
-- 背景：SaTokenConfigure 已把 inport/sales/retail/report/category/serialNumber/operationLog
--   从模块级 module:* 通配校验细化为操作级权限码（view/create/update/delete/return），
--   但 sys_permission 中这些模块没有任何权限码种子，非超管用户永远无法被授予，业务功能整体 403。
-- 本脚本：
--   1. 补齐各业务模块操作级权限码(id 168-193)及"序列号管理"菜单(188)
--   2. 补齐角色菜单授权缺口（销售员缺 商品销售/销售订单/退加货记录/等级规则 菜单授权）
--   3. 按角色职责授予新权限码，并补上销售员/采购员开单必需的 goods:view、category:view
-- 全部幂等（按 id/percode 与 rid/pid 防重），可重复执行。

-- 1. 新增权限码与序列号管理菜单（id 或 percode 已存在则跳过）
INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, `open`, ordernum, available)
SELECT v.id, v.pid, v.type, v.title, v.percode, v.icon, v.href, v.target, v.`open`, v.ordernum, v.available FROM (
    SELECT 168 AS id, 158 AS pid, 'permission' AS type, '进货查询' AS title, 'inport:view' AS percode, NULL AS icon, NULL AS href, NULL AS target, 0 AS `open`, 1 AS ordernum, 1 AS available
    UNION ALL SELECT 169, 158, 'permission', '进货开单', 'inport:create', NULL, NULL, NULL, 0, 2, 1
    UNION ALL SELECT 170, 158, 'permission', '进货修改', 'inport:update', NULL, NULL, NULL, 0, 3, 1
    UNION ALL SELECT 171, 158, 'permission', '进货删除', 'inport:delete', NULL, NULL, NULL, 0, 4, 1
    UNION ALL SELECT 172, 158, 'permission', '进货退货', 'inport:return', NULL, NULL, NULL, 0, 5, 1
    UNION ALL SELECT 173, 155, 'permission', '销售查询', 'sales:view', NULL, NULL, NULL, 0, 1, 1
    UNION ALL SELECT 174, 155, 'permission', '销售开单', 'sales:create', NULL, NULL, NULL, 0, 2, 1
    UNION ALL SELECT 175, 155, 'permission', '销售修改', 'sales:update', NULL, NULL, NULL, 0, 3, 1
    UNION ALL SELECT 176, 155, 'permission', '销售删除', 'sales:delete', NULL, NULL, NULL, 0, 4, 1
    UNION ALL SELECT 177, 155, 'permission', '销售退货', 'sales:return', NULL, NULL, NULL, 0, 5, 1
    UNION ALL SELECT 178, 134, 'permission', '零售查询', 'retail:view', NULL, NULL, NULL, 0, 1, 1
    UNION ALL SELECT 179, 134, 'permission', '零售开单', 'retail:create', NULL, NULL, NULL, 0, 2, 1
    UNION ALL SELECT 180, 134, 'permission', '零售修改', 'retail:update', NULL, NULL, NULL, 0, 3, 1
    UNION ALL SELECT 181, 134, 'permission', '零售删除', 'retail:delete', NULL, NULL, NULL, 0, 4, 1
    UNION ALL SELECT 182, 134, 'permission', '零售退货', 'retail:return', NULL, NULL, NULL, 0, 5, 1
    UNION ALL SELECT 183, 125, 'permission', '报表查看', 'report:view', NULL, NULL, NULL, 0, 1, 1
    UNION ALL SELECT 184, 130, 'permission', '分类查询', 'category:view', NULL, NULL, NULL, 0, 1, 1
    UNION ALL SELECT 185, 130, 'permission', '分类添加', 'category:create', NULL, NULL, NULL, 0, 2, 1
    UNION ALL SELECT 186, 130, 'permission', '分类修改', 'category:update', NULL, NULL, NULL, 0, 3, 1
    UNION ALL SELECT 187, 130, 'permission', '分类删除', 'category:delete', NULL, NULL, NULL, 0, 4, 1
    UNION ALL SELECT 188, 3, 'menu', '序列号管理', NULL, 'Tickets', '/business/serial-number', NULL, 0, 13, 1
    UNION ALL SELECT 189, 188, 'permission', '序列号查询', 'serialNumber:view', NULL, NULL, NULL, 0, 1, 1
    UNION ALL SELECT 190, 188, 'permission', '序列号添加', 'serialNumber:create', NULL, NULL, NULL, 0, 2, 1
    UNION ALL SELECT 191, 188, 'permission', '序列号修改', 'serialNumber:update', NULL, NULL, NULL, 0, 3, 1
    UNION ALL SELECT 192, 188, 'permission', '序列号删除', 'serialNumber:delete', NULL, NULL, NULL, 0, 4, 1
    UNION ALL SELECT 193, 132, 'permission', '操作日志查询', 'operationLog:view', NULL, NULL, NULL, 0, 1, 1
) v
LEFT JOIN sys_permission p ON p.id = v.id OR (v.percode IS NOT NULL AND p.percode = v.percode)
WHERE p.id IS NULL;

-- 2. 菜单授权缺口补齐 + 3. 新权限码按角色授权（rid/pid 防重）
--    角色: 1超级管理员 11仓库管理员 12采购员 13销售员 14财务人员
INSERT INTO sys_role_permission (rid, pid)
SELECT v.rid, v.pid FROM (
    -- 超级管理员：新菜单(155/156/157 销售页, 161 等级规则, 166/167 零售订单与退回, 188 序列号) + 全部新权限码
    SELECT 1 AS rid, 155 AS pid
    UNION ALL SELECT 1, 156 UNION ALL SELECT 1, 157 UNION ALL SELECT 1, 161
    UNION ALL SELECT 1, 166 UNION ALL SELECT 1, 167 UNION ALL SELECT 1, 188
    UNION ALL SELECT 1, 162 UNION ALL SELECT 1, 163 UNION ALL SELECT 1, 164 UNION ALL SELECT 1, 165
    UNION ALL SELECT 1, 168 UNION ALL SELECT 1, 169 UNION ALL SELECT 1, 170 UNION ALL SELECT 1, 171 UNION ALL SELECT 1, 172
    UNION ALL SELECT 1, 173 UNION ALL SELECT 1, 174 UNION ALL SELECT 1, 175 UNION ALL SELECT 1, 176 UNION ALL SELECT 1, 177
    UNION ALL SELECT 1, 178 UNION ALL SELECT 1, 179 UNION ALL SELECT 1, 180 UNION ALL SELECT 1, 181 UNION ALL SELECT 1, 182
    UNION ALL SELECT 1, 183
    UNION ALL SELECT 1, 184 UNION ALL SELECT 1, 185 UNION ALL SELECT 1, 186 UNION ALL SELECT 1, 187
    UNION ALL SELECT 1, 189 UNION ALL SELECT 1, 190 UNION ALL SELECT 1, 191 UNION ALL SELECT 1, 192 UNION ALL SELECT 1, 193
    -- 仓库管理员：进货全部操作 + 分类全部操作 + 序列号菜单及全部操作
    UNION ALL SELECT 11, 168 UNION ALL SELECT 11, 169 UNION ALL SELECT 11, 170 UNION ALL SELECT 11, 171 UNION ALL SELECT 11, 172
    UNION ALL SELECT 11, 184 UNION ALL SELECT 11, 185 UNION ALL SELECT 11, 186 UNION ALL SELECT 11, 187
    UNION ALL SELECT 11, 188 UNION ALL SELECT 11, 189 UNION ALL SELECT 11, 190 UNION ALL SELECT 11, 191 UNION ALL SELECT 11, 192
    -- 采购员：商品查询(开单必需) + 分类查询 + 进货查询/开单/退货
    UNION ALL SELECT 12, 91
    UNION ALL SELECT 12, 184
    UNION ALL SELECT 12, 168 UNION ALL SELECT 12, 169 UNION ALL SELECT 12, 172
    -- 销售员：销售页菜单 + 等级规则菜单 + 零售订单/退回菜单 + 销售/零售全部操作 + 开单辅助权限
    UNION ALL SELECT 13, 155 UNION ALL SELECT 13, 156 UNION ALL SELECT 13, 157
    UNION ALL SELECT 13, 161 UNION ALL SELECT 13, 166 UNION ALL SELECT 13, 167
    UNION ALL SELECT 13, 162 UNION ALL SELECT 13, 163 UNION ALL SELECT 13, 164 UNION ALL SELECT 13, 165
    UNION ALL SELECT 13, 173 UNION ALL SELECT 13, 174 UNION ALL SELECT 13, 175 UNION ALL SELECT 13, 176 UNION ALL SELECT 13, 177
    UNION ALL SELECT 13, 178 UNION ALL SELECT 13, 179 UNION ALL SELECT 13, 180 UNION ALL SELECT 13, 181 UNION ALL SELECT 13, 182
    UNION ALL SELECT 13, 91 UNION ALL SELECT 13, 184 UNION ALL SELECT 13, 189
    -- 财务人员：报表查看（只读）
    UNION ALL SELECT 14, 183
) v
LEFT JOIN sys_role_permission rp ON rp.rid = v.rid AND rp.pid = v.pid
WHERE rp.rid IS NULL;
