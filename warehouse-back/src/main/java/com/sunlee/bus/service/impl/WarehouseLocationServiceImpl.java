package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.WarehouseLocation;
import com.sunlee.bus.mapper.WarehouseLocationMapper;
import com.sunlee.bus.service.IWarehouseLocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库位服务实现
 *
 * @author sunlee
 * @since 2026-07-26
 */
@Service
@Transactional
public class WarehouseLocationServiceImpl extends ServiceImpl<WarehouseLocationMapper, WarehouseLocation>
        implements IWarehouseLocationService {
}
