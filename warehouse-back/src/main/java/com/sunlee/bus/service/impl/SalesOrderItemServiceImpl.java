package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.SalesOrderItem;
import com.sunlee.bus.mapper.SalesOrderItemMapper;
import com.sunlee.bus.service.ISalesOrderItemService;
import org.springframework.stereotype.Service;

@Service
public class SalesOrderItemServiceImpl extends ServiceImpl<SalesOrderItemMapper, SalesOrderItem>
        implements ISalesOrderItemService {
}
