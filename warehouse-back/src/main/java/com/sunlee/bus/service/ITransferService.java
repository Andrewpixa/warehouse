package com.sunlee.bus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunlee.bus.entity.Transfer;

/**
 * 库存调拨服务
 *
 * @author sunlee
 * @since 2026-07-26
 */
public interface ITransferService extends IService<Transfer> {

    /**
     * 创建调拨单（草稿，状态0）
     */
    void createTransfer(Transfer transfer);

    /**
     * 发出调拨单：源仓原子扣减，状态 0→1（在途）
     */
    void shipTransfer(Integer id);

    /**
     * 收货完成：目的仓增加，状态 1→2（已完成）
     */
    void receiveTransfer(Integer id);

    /**
     * 取消调拨单：草稿直接取消；在途取消需回滚源仓库存
     */
    void cancelTransfer(Integer id);

    /**
     * 删除调拨单（仅草稿/已取消可删）
     */
    void deleteTransfer(Integer id);
}
