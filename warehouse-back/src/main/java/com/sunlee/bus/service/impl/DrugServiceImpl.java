package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.common.PharmaIds;
import com.sunlee.bus.entity.Drug;
import com.sunlee.bus.mapper.DrugMapper;
import com.sunlee.bus.service.IDrugService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DrugServiceImpl extends ServiceImpl<DrugMapper, Drug> implements IDrugService {

    @Override
    public boolean save(Drug entity) {
        if (StringUtils.isBlank(entity.getCategory())) {
            entity.setCategory(PharmaIds.categoryOf(entity.getId()));
        }
        if (entity.getId() == null) {
            long[] range = PharmaIds.rangeOfCategory(entity.getCategory());
            entity.setId(PharmaIds.allocate(baseMapper, range[0], range[1]));
        }
        if (StringUtils.isBlank(entity.getCode())) {
            entity.setCode(String.valueOf(entity.getId()));
        }
        return super.save(entity);
    }
}
