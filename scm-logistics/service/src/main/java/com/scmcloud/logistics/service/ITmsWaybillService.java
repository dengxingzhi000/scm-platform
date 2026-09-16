package com.scmcloud.logistics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.logistics.domain.entity.TmsWaybill;

import java.util.List;

public interface ITmsWaybillService extends IService<TmsWaybill> {

    Page<TmsWaybill> pageList(int page, int size, String waybillNo, Integer status, String carrierId);

    TmsWaybill getByWaybillNo(String waybillNo);

    List<TmsWaybill> listByOrderId(String orderId);

    List<TmsWaybill> listByOrderNo(String orderNo);

    TmsWaybill createWaybill(TmsWaybill waybill);

    boolean updateStatus(String waybillId, Integer status, String operator);

    boolean cancelWaybill(String waybillId, String reason, String operator);
}
