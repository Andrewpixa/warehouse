package com.sunlee.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.sys.entity.Dept;
import com.sunlee.sys.entity.User;
import com.sunlee.sys.mapper.DeptMapper;
import com.sunlee.sys.mapper.UserMapper;
import com.sunlee.sys.service.IDeptService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements IDeptService {

    private static final Set<String> TYPES = Set.of(
            "公司", "总经办", "销售", "仓储", "质量", "采购", "财务", "信息", "物流", "其他");

    @Autowired
    private UserMapper userMapper;

    @Override
    public Dept getDetail(Integer id) {
        Dept dept = this.getById(id);
        if (dept == null) {
            throw new IllegalArgumentException("部门不存在");
        }
        fill(dept);
        return dept;
    }

    @Override
    public void saveDept(Dept dept) {
        if (StringUtils.isBlank(dept.getName())) {
            throw new IllegalArgumentException("部门名称必填");
        }
        if (StringUtils.isBlank(dept.getDeptCode())) {
            throw new IllegalArgumentException("部门编码必填");
        }
        if (StringUtils.isBlank(dept.getDeptType()) || !TYPES.contains(dept.getDeptType())) {
            throw new IllegalArgumentException("部门类型须为批发 GSP 枚举");
        }
        if (dept.getPid() == null) {
            throw new IllegalArgumentException("须指定上级部门（根挂公司节点）");
        }
        if (dept.getOrdernum() == null) {
            throw new IllegalArgumentException("排序号必填");
        }
        if (needGsp(dept.getDeptType()) && StringUtils.isBlank(dept.getGspRoles())) {
            throw new IllegalArgumentException("质量/仓储部门须选择 GSP 职责");
        }
        QueryWrapper<Dept> codeQw = new QueryWrapper<Dept>().eq("dept_code", dept.getDeptCode().trim());
        if (dept.getId() != null) {
            codeQw.ne("id", dept.getId());
        }
        if (this.count(codeQw) > 0) {
            throw new IllegalArgumentException("部门编码已存在");
        }
        dept.setDeptCode(dept.getDeptCode().trim().toUpperCase());
        dept.setName(dept.getName().trim());
        if (dept.getAvailable() == null) {
            dept.setAvailable(1);
        }
        if (dept.getOpen() == null) {
            dept.setOpen(dept.getPid() == 0 ? 1 : 0);
        }
        if (dept.getId() == null) {
            dept.setCreatetime(new Date());
            this.save(dept);
            return;
        }
        Dept old = this.getById(dept.getId());
        if (old == null) {
            throw new IllegalArgumentException("部门不存在");
        }
        if (StringUtils.isNotBlank(old.getDeptCode()) && !old.getDeptCode().equalsIgnoreCase(dept.getDeptCode())) {
            throw new IllegalArgumentException("部门编码保存后不可改");
        }
        dept.setAddress(null);
        this.updateById(dept);
    }

    @Override
    public void disableDept(Integer id) {
        Dept dept = this.getById(id);
        if (dept == null) {
            throw new IllegalArgumentException("部门不存在");
        }
        long members = userMapper.selectCount(new QueryWrapper<User>().eq("deptid", id).eq("available", 1));
        if (members > 0) {
            throw new IllegalArgumentException("该部门仍有启用中的员工，不能停用");
        }
        long children = this.count(new QueryWrapper<Dept>().eq("pid", id).eq("available", 1));
        if (children > 0) {
            throw new IllegalArgumentException("仍有启用中的下级部门，不能停用");
        }
        dept.setAvailable(0);
        this.updateById(dept);
    }

    @Override
    public List<User> listByGspRole(String gspRole) {
        List<Dept> depts = this.list(new QueryWrapper<Dept>().eq("available", 1)
                .in("dept_type", Arrays.asList("质量", "仓储")));
        return depts.stream()
                .filter(d -> StringUtils.isNotBlank(d.getGspRoles())
                        && (StringUtils.isBlank(gspRole) || Arrays.asList(d.getGspRoles().split(",")).contains(gspRole)))
                .flatMap(d -> userMapper.selectList(new QueryWrapper<User>()
                        .eq("deptid", d.getId()).eq("available", 1)).stream())
                .toList();
    }

    @Override
    public void fill(Dept dept) {
        if (dept.getPid() != null && dept.getPid() != 0) {
            Dept p = this.getById(dept.getPid());
            if (p != null) {
                dept.setParentName(p.getName());
            }
        } else {
            dept.setParentName("—");
        }
        if (dept.getManagerUserId() != null) {
            User u = userMapper.selectById(dept.getManagerUserId());
            if (u != null) {
                dept.setManagerName(u.getName());
            }
        }
        List<Integer> ids = listSelfAndDescendantIds(dept.getId());
        Long members = ids.isEmpty() ? 0L
                : userMapper.selectCount(new QueryWrapper<User>().in("deptid", ids).eq("available", 1));
        dept.setMemberCount(members == null ? 0 : members.intValue());
    }

    @Override
    public List<Integer> listSelfAndDescendantIds(Integer rootId) {
        List<Integer> ids = new ArrayList<>();
        if (rootId == null) {
            return ids;
        }
        ids.add(rootId);
        List<Dept> all = this.list();
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Integer pid = queue.poll();
            for (Dept d : all) {
                if (pid.equals(d.getPid()) && d.getId() != null && !ids.contains(d.getId())) {
                    ids.add(d.getId());
                    queue.add(d.getId());
                }
            }
        }
        return ids;
    }

    @Override
    public void assertEnabled(Integer deptId) {
        if (deptId == null) {
            throw new IllegalArgumentException("用户必须归属一个启用中的部门");
        }
        Dept dept = this.getById(deptId);
        if (dept == null || dept.getAvailable() == null || dept.getAvailable() != 1) {
            throw new IllegalArgumentException("只能归属启用中的部门");
        }
    }

    private boolean needGsp(String type) {
        return "质量".equals(type) || "仓储".equals(type);
    }
}
