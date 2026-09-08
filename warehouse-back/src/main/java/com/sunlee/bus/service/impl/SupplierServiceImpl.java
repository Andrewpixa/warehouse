package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.common.PharmaIds;
import com.sunlee.bus.entity.Supplier;
import com.sunlee.bus.mapper.SupplierMapper;
import com.sunlee.bus.service.ISupplierService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements ISupplierService {

    @Override
    public boolean save(Supplier entity) {
        if (entity.getId() == null) {
            entity.setId(PharmaIds.allocate(baseMapper, PharmaIds.SUPPLIER_START, PharmaIds.SUPPLIER_END));
        }
        if (StringUtils.isBlank(entity.getCode())) {
            entity.setCode(String.valueOf(entity.getId()));
        }
        return super.save(entity);
    }
}
