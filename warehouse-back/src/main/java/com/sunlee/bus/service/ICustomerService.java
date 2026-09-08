package com.sunlee.bus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.Customer;

public interface ICustomerService extends IService<Customer> {

    void deleteCustomerById(Long id);
}
