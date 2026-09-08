package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.common.PharmaIds;
import com.sunlee.bus.entity.Customer;
import com.sunlee.bus.mapper.CustomerMapper;
import com.sunlee.bus.service.ICustomerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements ICustomerService {

    @Override
    public boolean save(Customer entity) {
        if (entity.getId() == null) {
            entity.setId(PharmaIds.allocate(baseMapper, PharmaIds.CUSTOMER_START, PharmaIds.CUSTOMER_END));
        }
        if (StringUtils.isBlank(entity.getCode())) {
            entity.setCode(String.valueOf(entity.getId()));
        }
        return super.save(entity);
    }

    @Override
    public void deleteCustomerById(Long id) {
        this.removeById(id);
    }
}
