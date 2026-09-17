package com.sunlee.sys.schedule;

import com.sunlee.sys.common.AppFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 组织与演示人员：销售/仓储只挂细分组；每组一位经理（超管）；头像来自仓库 faces。
 */
@Slf4j
@Order(20)
@Component
public class DemoOrgSeed implements CommandLineRunner {

    private static final String PWD = "532ac00e86893901af5f0be6b704dbc7";
    private static final String SALT = "04A93C74C8294AA09A8B974FD1F4ECBB";

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        try {
            jdbc.update("UPDATE sys_permission SET available = 0 WHERE id = 14 OR pid = 14 OR title = '部门管理' OR href LIKE '%DeptManager%' OR href = '/system/dept'");
            seedDepts();
            copyFaces();
            jdbc.update("UPDATE sys_user SET deptid = 22 WHERE loginname = 'purchase'");
            jdbc.update("UPDATE sys_user SET deptid = 23 WHERE loginname = 'finance'");
            jdbc.update("UPDATE sys_user SET deptid = 21, type = 0, remark = '总经理' WHERE loginname = 'admin'");
            jdbc.update("UPDATE sys_user SET deptid = 5 WHERE loginname = 'sales'");
            jdbc.update("UPDATE sys_user SET deptid = 25 WHERE loginname = 'warehouse'");
            jdbc.update("UPDATE sys_user SET deptid = 5 WHERE deptid = 2");
            seedPeople();
            bindManagers();
            seedDeptDeskMenus();
            log.info("demo org/users seeded");
        } catch (Exception e) {
            log.warn("demo org seed skipped: {}", e.getMessage());
        }
    }

    private void seedDepts() {
        jdbc.update("UPDATE sys_dept SET pid = 2, name = '销售一部', dept_code = 'XSB-1', dept_type = '销售', available = 1, remark = NULL, ordernum = 1 WHERE id = 5");
        jdbc.update("UPDATE sys_dept SET pid = 2, name = '销售二部', dept_code = 'XSB-2', dept_type = '销售', available = 1, remark = NULL, ordernum = 2 WHERE id = 6");
        insertDept(5, 2, "销售一部", "XSB-1", "销售", 1, null);
        insertDept(6, 2, "销售二部", "XSB-2", "销售", 2, null);
        insertDept(21, 1, "总经办", "ZJB", "总经办", 2, null);
        insertDept(22, 1, "采购部", "CGB", "采购", 6, null);
        insertDept(23, 1, "财务部", "CWB", "财务", 7, null);
        insertDept(24, 1, "信息部", "XXB", "信息", 8, null);
        insertDept(27, 24, "IT组", "XXB-IT", "信息", 1, null);
        insertDept(28, 24, "物流组", "XXB-WL", "物流", 2, null);
        insertDept(25, 3, "常温仓组", "CCB-CW", "仓储", 1, "收货验收,出库复核");
        insertDept(26, 3, "冷链仓组", "CCB-LL", "仓储", 2, "收货验收,养护检查,出库复核");
        jdbc.update("UPDATE sys_dept SET name = '药衡医药', dept_code = 'ROOT', dept_type = '公司', available = 1, open = 1, pid = 0 WHERE id = 1");
        jdbc.update("UPDATE sys_user SET deptid = 23 WHERE deptid = 7");
        jdbc.update("DELETE FROM sys_dept WHERE id = 7 AND (dept_code IS NULL OR dept_code = '')");
        jdbc.update("UPDATE sys_dept SET available = 0, remark = '历史节点已停用' WHERE id IN (8,9,10,18) AND (dept_code IS NULL OR dept_code = '')");
    }

    private void seedDeptDeskMenus() {
        insertMenu(250, 1, "部门作业", null, "OfficeBuilding", 20);
        insertMenu(251, 250, "采购作业", "/business/dept-desk?type=采购", "ShoppingCart", 1);
        insertMenu(252, 250, "销售作业", "/business/dept-desk?type=销售", "Sell", 2);
        insertMenu(253, 250, "仓储作业", "/business/dept-desk?type=仓储", "Box", 3);
        insertMenu(254, 250, "质量作业", "/business/dept-desk?type=质量", "Stamp", 4);
        insertMenu(255, 250, "财务作业", "/business/dept-desk?type=财务", "Wallet", 5);
        insertMenu(256, 250, "信息作业", "/business/dept-desk?type=信息", "Monitor", 6);
        insertMenu(259, 250, "模拟开票", "/business/invoice", "Ticket", 7);
        insertMenu(261, 250, "物流作业", "/business/dept-desk?type=物流", "Van", 8);
        insertPerm(257, 254, "质量查看", "quality:view", 1);
        insertPerm(258, 254, "质量判定", "quality:confirm", 2);
        grantMenus(1, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 261);
        grantMenus(12, 250, 251);
        grantMenus(13, 250, 252);
        grantMenus(11, 250, 253);
        grantMenus(15, 250, 254, 257, 258);
        grantMenus(14, 250, 255);
        grantMenus(16, 250, 256, 259, 261);
    }

    private void insertMenu(int id, int pid, String title, String href, String icon, int order) {
        Integer byId = jdbc.queryForObject("SELECT COUNT(*) FROM sys_permission WHERE id = ?", Integer.class, id);
        if (byId != null && byId > 0) {
            jdbc.update("UPDATE sys_permission SET available = 1, pid = ?, title = ?, icon = ?, href = ? WHERE id = ?",
                    pid, title, icon, href, id);
            return;
        }
        if (href != null && !href.isEmpty()) {
            Integer byHref = jdbc.queryForObject("SELECT COUNT(*) FROM sys_permission WHERE href = ?", Integer.class, href);
            if (byHref != null && byHref > 0) {
                jdbc.update("UPDATE sys_permission SET available = 1, pid = ?, title = ?, icon = ? WHERE href = ?",
                        pid, title, icon, href);
                return;
            }
        }
        jdbc.update("""
                INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
                VALUES (?,?, 'menu', ?, NULL, ?, ?, '', 0, ?, 1)
                """, id, pid, title, icon, href, order);
    }

    private void insertPerm(int id, int pid, String title, String percode, int order) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sys_permission WHERE id = ? OR percode = ?", Integer.class, id, percode);
        if (n != null && n > 0) {
            return;
        }
        jdbc.update("""
                INSERT INTO sys_permission (id, pid, type, title, percode, icon, href, target, open, ordernum, available)
                VALUES (?,?, 'permission', ?, ?, NULL, NULL, NULL, 0, ?, 1)
                """, id, pid, title, percode, order);
    }

    private void grantMenus(int roleId, int... pids) {
        for (int pid : pids) {
            Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_permission WHERE rid = ? AND pid = ?", Integer.class, roleId, pid);
            if (n != null && n == 0) {
                jdbc.update("INSERT INTO sys_role_permission (rid, pid) VALUES (?, ?)", roleId, pid);
            }
        }
    }

    private void insertDept(int id, int pid, String name, String code, String type, int order, String gsp) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sys_dept WHERE id = ? OR dept_code = ?", Integer.class, id, code);
        if (n != null && n > 0) {
            return;
        }
        jdbc.update("""
                INSERT INTO sys_dept (id, pid, name, dept_code, dept_type, open, remark, address, available, ordernum, createtime, gsp_roles)
                VALUES (?,?,?,?,?,0,NULL,NULL,1,?,NOW(),?)
                """, id, pid, name, code, type, order, gsp);
    }

    private void seedPeople() {
        upsert("陈衡", "chenheng", 21, "总经理", 1, 1, 0, 1, "avatar/male_01.jpg");
        upsert("周助理", "zhouzl", 21, "行政助理", 2, 14, 1, 0, "avatar/female_01.jpg");
        upsert("刘销经", "liuxj", 5, "销售一部经理", 1, 1, 0, 1, "avatar/male_02.jpg");
        upsert("李销一", "lix1", 5, "销售代表", 2, 13, 1, 1, "avatar/male_03.jpg");
        upsert("王销一", "wangx1", 5, "销售代表", 3, 13, 1, 0, "avatar/female_02.jpg");
        upsert("赵销二", "zhaox2", 6, "销售二部经理", 1, 1, 0, 1, "avatar/male_04.jpg");
        upsert("钱销二", "qianx2", 6, "销售代表", 2, 13, 1, 0, "avatar/female_03.jpg");
        upsert("孙仓管", "suncg", 25, "常温仓组经理", 1, 1, 0, 1, "avatar/male_05.jpg");
        upsert("吴收货", "wush", 25, "收货员", 2, 11, 1, 1, "avatar/male_06.jpg");
        upsert("郑复核", "zhengfh", 25, "复核员", 3, 11, 1, 0, "avatar/female_04.jpg");
        upsert("冯冷链", "fengll", 26, "冷链仓组经理", 1, 1, 0, 1, "avatar/male_07.jpg");
        upsert("陈养护", "chenyh", 26, "养护员", 2, 11, 1, 0, "avatar/female_05.jpg");
        upsert("褚质管", "chuzg", 4, "质量部经理", 1, 1, 0, 1, "avatar/male_08.jpg");
        upsert("卫放行", "weifx", 4, "放行员", 2, 11, 1, 0, "avatar/female_06.jpg");
        upsert("蒋采购", "jiangcg", 22, "采购部经理", 1, 1, 0, 1, "avatar/male_09.jpg");
        upsert("沈采购", "shencg", 22, "采购员", 2, 12, 1, 0, "avatar/female_07.jpg");
        upsert("韩会计", "hankj", 23, "财务部经理", 1, 1, 0, 1, "avatar/male_11.jpg");
        upsert("杨运维", "yangyw", 27, "IT组经理", 1, 1, 0, 1, "avatar/male_10.jpg");
        upsert("何物流", "hewl", 28, "物流组经理", 1, 1, 0, 1, "avatar/male_12.jpg");
    }

    private void upsert(String name, String login, int deptId, String remark, int ordernum, int roleId, int type, int sex, String img) {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE loginname = ?", Integer.class, login);
        if (exists != null && exists > 0) {
            jdbc.update("""
                    UPDATE sys_user SET name=?, deptid=?, remark=?, ordernum=?, type=?, sex=?, imgpath=?, mgr=NULL, available=1
                    WHERE loginname=?
                    """, name, deptId, remark, ordernum, type, sex, img, login);
        } else {
            jdbc.update("""
                    INSERT INTO sys_user (name, loginname, pwd, address, sex, remark, deptid, hiredate, mgr, available, ordernum, type, imgpath, salt)
                    VALUES (?,?,?,'广州',?,?,?,NOW(),NULL,1,?,?,?,?)
                    """, name, login, PWD, sex, remark, deptId, ordernum, type, img, SALT);
        }
        Integer uid = jdbc.queryForObject("SELECT id FROM sys_user WHERE loginname = ?", Integer.class, login);
        if (uid == null) {
            return;
        }
        Integer ur = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user_role WHERE uid = ? AND rid = ?", Integer.class, uid, roleId);
        if (ur != null && ur == 0) {
            jdbc.update("INSERT INTO sys_user_role (uid, rid) VALUES (?, ?)", uid, roleId);
        }
        if (type == 0) {
            Integer superRole = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user_role WHERE uid = ? AND rid = 1", Integer.class, uid);
            if (superRole != null && superRole == 0) {
                jdbc.update("INSERT INTO sys_user_role (uid, rid) VALUES (?, 1)", uid);
            }
        }
    }

    private void bindManagers() {
        setMgr(21, "chenheng");
        setMgr(5, "liuxj");
        setMgr(6, "zhaox2");
        setMgr(25, "suncg");
        setMgr(26, "fengll");
        setMgr(4, "chuzg");
        setMgr(22, "jiangcg");
        setMgr(23, "hankj");
        setMgr(24, "yangyw");
        setMgr(27, "yangyw");
        setMgr(28, "hewl");
    }

    private void setMgr(int deptId, String login) {
        Integer uid = jdbc.queryForObject("SELECT id FROM sys_user WHERE loginname = ?", Integer.class, login);
        if (uid != null) {
            jdbc.update("UPDATE sys_dept SET manager_user_id = ? WHERE id = ?", uid, deptId);
        }
    }

    private void copyFaces() {
        File srcRoot = findFacesDir();
        if (srcRoot == null) {
            log.warn("faces folder not found, skip avatars");
            return;
        }
        File dest = new File(AppFileUtils.UPLOAD_PATH, "avatar");
        if (!dest.mkdirs() && !dest.isDirectory()) {
            log.warn("cannot create avatar dir {}", dest.getAbsolutePath());
            return;
        }
        copyOne(new File(srcRoot, "male"), dest, "male_", 12);
        copyOne(new File(srcRoot, "female"), dest, "female_", 7);
    }

    private void copyOne(File from, File dest, String prefix, int n) {
        for (int i = 1; i <= n; i++) {
            String name = prefix + String.format("%02d", i) + ".jpg";
            File src = new File(from, name);
            if (!src.isFile()) {
                continue;
            }
            try {
                Files.copy(src.toPath(), new File(dest, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                log.warn("copy face {} failed: {}", name, e.getMessage());
            }
        }
    }

    private File findFacesDir() {
        String[] candidates = {
                "faces",
                "../faces",
                System.getProperty("user.dir") + "/faces",
                System.getProperty("user.dir") + "/../faces"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.isDirectory() && new File(f, "male").isDirectory()) {
                return f.getAbsoluteFile();
            }
        }
        return null;
    }
}
