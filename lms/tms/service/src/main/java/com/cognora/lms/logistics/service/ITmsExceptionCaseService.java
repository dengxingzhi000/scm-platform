package com.cognora.lms.logistics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.lms.logistics.domain.entity.TmsExceptionCase;

/**
 * 异常件认领与闭环（批次 3）。
 */
public interface ITmsExceptionCaseService extends IService<TmsExceptionCase> {

    Page<TmsExceptionCase> pageList(int page, int size, String waybillNo, String status);

    TmsExceptionCase createCase(String waybillId, String exceptionType, String description, String operator);

    TmsExceptionCase claim(String id, String responsibleParty, String operator);

    TmsExceptionCase transition(String id, String targetStatus, String resolution, String operator);
}
