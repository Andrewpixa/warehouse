package com.sunlee.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.sys.entity.Dept;
import com.sunlee.sys.entity.User;

import java.util.List;

public interface IDeptService extends IService<Dept> {

    Dept getDetail(Integer id);

    void saveDept(Dept dept);

    void disableDept(Integer id);

    List<User> listByGspRole(String gspRole);

    void fill(Dept dept);

    void assertEnabled(Integer deptId);

    /** 本部门及全部下级（含停用节点，便于点上级仍能看到挂在下级的人） */
    List<Integer> listSelfAndDescendantIds(Integer rootId);
}
