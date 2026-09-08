package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.PurchaseOrderItem;
import com.sunlee.bus.mapper.PurchaseOrderItemMapper;
import com.sunlee.bus.service.IPurchaseOrderItemService;
import org.springframework.stereotype.Service;

@Service
public class PurchaseOrderItemServiceImpl extends ServiceImpl<PurchaseOrderItemMapper, PurchaseOrderItem>
        implements IPurchaseOrderItemService {
}
