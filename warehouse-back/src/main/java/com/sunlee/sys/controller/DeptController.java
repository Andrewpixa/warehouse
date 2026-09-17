package com.sunlee.sys.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunlee.sys.common.DataGridView;
import com.sunlee.sys.common.ResultObj;
import com.sunlee.sys.common.TreeNode;
import com.sunlee.sys.common.TreeNodeBuilder;
import com.sunlee.sys.entity.Dept;
import com.sunlee.sys.service.IDeptService;
import com.sunlee.sys.vo.DeptVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * InnoDB free: 9216 kB 前端控制器
 * </p>
 *
 * @author sunlee
 * @since 2026-02-20
 */
@Slf4j
@RestController
@RequestMapping("/dept")
public class DeptController {

    @Autowired
    private IDeptService deptService;

    /**
     * 加载部门左边的菜单树
     * @param deptVo
     * @return
     */
    @RequestMapping("loadDeptManagerLeftTreeJson")
    public DataGridView loadManagerLeftTreeJson(DeptVo deptVo){
        //查询出所有的部门，存放进list中
//        QueryWrapper<Dept> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq('1');
        QueryWrapper<Dept> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("available", 1);
        queryWrapper.orderByAsc("ordernum");
        List<Dept> list = deptService.list(queryWrapper);

        List<TreeNode> treeNodes = new ArrayList<>();
        //将部门放入treeNodes中，组装成json
        for (Dept dept : list) {
            boolean open = dept.getOpen() != null && dept.getOpen() == 1;
            String title = StringUtils.isNotBlank(dept.getDeptCode())
                    ? dept.getDeptCode() + " " + dept.getName()
                    : dept.getName();
            treeNodes.add(new TreeNode(dept.getId(), dept.getPid(), title, open));
        }
        //构造层级关系
        List<TreeNode> tree = TreeNodeBuilder.build(treeNodes, 0);
        return new DataGridView(tree);
    }

    /**
     * 查询所有部门数据
     * @param deptVo
     * @return
     */
    @RequestMapping("loadAllDept")
    public DataGridView loadAllDept(DeptVo deptVo){
        IPage<Dept> page = new Page<>(deptVo.getPage(),deptVo.getLimit());
        QueryWrapper<Dept> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(deptVo.getDeptCode()), "dept_code", deptVo.getDeptCode());
        queryWrapper.like(StringUtils.isNotBlank(deptVo.getName()),"name",deptVo.getName());
        queryWrapper.eq(deptVo.getAvailable()!=null,"available",deptVo.getAvailable());
        if (deptVo.getId() != null) {
            queryWrapper.eq("pid", deptVo.getId());
        }
        queryWrapper.orderByAsc("ordernum");
        deptService.page(page,queryWrapper);
        page.getRecords().forEach(deptService::fill);
        return new DataGridView(page.getTotal(),page.getRecords());
    }

    @RequestMapping("loadDeptDetail")
    public DataGridView loadDeptDetail(Integer id) {
        return new DataGridView(deptService.getDetail(id));
    }

    @RequestMapping("loadGspUsers")
    public DataGridView loadGspUsers(String gspRole) {
        return new DataGridView(deptService.listByGspRole(gspRole));
    }

    /**
     * 添加部门
     * @param deptVo
     * @return
     */
    @RequestMapping("addDept")
    public ResultObj addDept(DeptVo deptVo){
        try {
            deptService.saveDept(deptVo);
            return ResultObj.ADD_SUCCESS;
        } catch (Exception e) {
            log.error("操作失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

    /**
     * 加载排序码
     * @return
     */
    @RequestMapping("loadDeptMaxOrderNum")
    public Map<String,Object> loadDeptMaxOrderNum(){
        Map<String,Object> map = new HashMap<String,Object>();
        QueryWrapper<Dept> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("ordernum");
        IPage<Dept> page = new Page<>(1,1);
        List<Dept> list = deptService.page(page,queryWrapper).getRecords();
        if (list.size()>0){
            map.put("value",list.get(0).getOrdernum()+1);
        }else {
            map.put("value",1);
        }
        return map;
    }

    /**
     * 更新部门
     * @param deptVo
     * @return
     */
    @RequestMapping("updateDept")
    public ResultObj updateDept(DeptVo deptVo){
        try {
            deptService.saveDept(deptVo);
            return ResultObj.UPDATE_SUCCESS;
        } catch (Exception e) {
            log.error("操作失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

    /**
     * 检查当前部门是否有子部门
     * @param deptVo
     * @return
     */
    @RequestMapping("checkDeptHasChildrenNode")
    public Map<String,Object> checkDeptHasChildrenNode(DeptVo deptVo){
        Map<String,Object> map = new HashMap<String, Object>();
        QueryWrapper<Dept> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pid",deptVo.getId());
        List<Dept> list = deptService.list(queryWrapper);
        if (list.size()>0){
            map.put("value",true);
        }else {
            map.put("value",false);
        }
        return map;
    }

    /**
     * 删除部门
     * @param deptVo
     * @return
     */
    @RequestMapping("deleteDept")
    public ResultObj deleteDept(DeptVo deptVo){
        try {
            deptService.disableDept(deptVo.getId());
            return ResultObj.ok("已停用");
        } catch (Exception e) {
            log.error("操作失败: {}", e.getMessage(), e);
            return ResultObj.error(e.getMessage());
        }
    }

}

