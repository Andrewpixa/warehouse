package com.sunlee.bus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sunlee.bus.entity.PurchaseOrder;
import com.sunlee.bus.vo.PurchaseInvoiceLineVo;
import com.sunlee.bus.vo.PurchaseOrderVo;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    IPage<PurchaseInvoiceLineVo> pageInvoiceLines(Page<PurchaseInvoiceLineVo> page, @Param("vo") PurchaseOrderVo vo);

    Map<String, Object> sumInvoiceLines(@Param("vo") PurchaseOrderVo vo);
}
