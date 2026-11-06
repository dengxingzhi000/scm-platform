package com.cognora.lms.logistics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.lms.logistics.domain.entity.TmsSlaAlert;

import java.time.LocalDateTime;

/**
 * SLA 超时预警（批次 3）。
 */
public interface ITmsSlaAlertService extends IService<TmsSlaAlert> {

    /**
     * 扫描未签收且已超过承诺时效的运单，按「运单号 + 承诺时间」幂等生成预警。
     *
     * @return 本次新增预警数量
     */
    int scanOverdue(LocalDateTime now);

    Page<TmsSlaAlert> pageList(int page, int size, String waybillNo, String status, String level);

    void acknowledge(String id, String operator);
}
