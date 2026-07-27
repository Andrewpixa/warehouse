-- =============================================================
-- 菜单结构调整迁移脚本（2026-07-26）
-- 变更内容：
--   1. 盘点管理(141)、序列号管理(188) 从「进货管理(3)」移入「仓储管理(194)」
--   2. 仓储管理(194) 顶级排序 9→5（紧跟零售管理之后，进销存业务流连续）
--   3. 会员中心(152) 5→6、人资中心(154) 6→9（让位给仓储管理）
--   4. 仓储管理内部重排：仓库管理(1) / 分仓库存(2) / 库存调拨(3) / 盘点管理(4) / 序列号管理(5)
--   5. 基础数据内部：商品分类(130) 改名「分类管理」并置顶(1)，商品管理(9) 顺位为 2
--      （命名风格统一带「管理」后缀；顺序匹配建档依赖：先分类后商品）
--   6. 种子对齐远程库标题：154「人员管理」、18「人员信息」（仅种子侧修改，远程库无需执行）
--   7. 顶级分组「基础数据」改名「基础资料」
-- 说明：仅改 pid/ordernum，菜单 id 不变，权限码与角色授权（sys_role_permission）不受影响。
-- 执行方式（Windows 下必须指定 utf8mb4）：
--   mysql --default-character-set=utf8mb4 -h <host> -u <user> -p warehouse < migration-20260726-menu.sql
-- =============================================================

USE warehouse;

-- 1. 盘点管理、序列号管理移入仓储管理（带原值守卫，幂等）
UPDATE sys_permission SET pid = 194, ordernum = 4 WHERE id = 141 AND pid = 3;
UPDATE sys_permission SET pid = 194, ordernum = 5 WHERE id = 188 AND pid = 3;

-- 2. 顶级分组排序：仓储管理提前到第 5 位
UPDATE sys_permission SET ordernum = 5 WHERE id = 194 AND ordernum = 9;
UPDATE sys_permission SET ordernum = 6 WHERE id = 152 AND ordernum = 5;
UPDATE sys_permission SET ordernum = 9 WHERE id = 154 AND ordernum = 6;

-- 3. 仓储管理内部顺序：分仓库存与库存调拨对调
UPDATE sys_permission SET ordernum = 2 WHERE id = 211 AND ordernum = 3;
UPDATE sys_permission SET ordernum = 3 WHERE id = 204 AND ordernum = 2;

-- 4. 基础数据：商品分类改名分类管理并置顶，商品管理顺位为 2
UPDATE sys_permission SET title = '分类管理', ordernum = 1 WHERE id = 130 AND title = '商品分类';
UPDATE sys_permission SET ordernum = 2 WHERE id = 9 AND ordernum = 1;

-- 5. 分组名「基础数据」改「基础资料」（业务用户更易理解）
UPDATE sys_permission SET title = '基础资料' WHERE id = 151 AND title = '基础数据';

-- 验证：应输出调整后的完整菜单树
SELECT p.id, p.pid, p.title, p.ordernum
FROM sys_permission p
WHERE p.type = 'menu' AND p.available = 1
ORDER BY p.pid, p.ordernum;
