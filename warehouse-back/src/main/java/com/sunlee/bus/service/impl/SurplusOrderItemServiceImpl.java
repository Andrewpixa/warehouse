package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.SurplusOrderItem;
import com.sunlee.bus.mapper.SurplusOrderItemMapper;
import com.sunlee.bus.service.ISurplusOrderItemService;
import org.springframework.stereotype.Service;

@Service
public class SurplusOrderItemServiceImpl extends ServiceImpl<SurplusOrderItemMapper, SurplusOrderItem>
        implements ISurplusOrderItemService {
}
