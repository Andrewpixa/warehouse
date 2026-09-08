package com.sunlee.bus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunlee.bus.entity.TracePackRelation;
import com.sunlee.bus.mapper.TracePackRelationMapper;
import com.sunlee.bus.service.ITracePackRelationService;
import org.springframework.stereotype.Service;

@Service
public class TracePackRelationServiceImpl extends ServiceImpl<TracePackRelationMapper, TracePackRelation>
        implements ITracePackRelationService {
}
