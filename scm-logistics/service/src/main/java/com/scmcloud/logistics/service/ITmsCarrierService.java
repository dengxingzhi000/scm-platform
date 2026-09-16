package com.scmcloud.logistics.service;

import com.scmcloud.logistics.domain.entity.TmsCarrier;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

public interface ITmsCarrierService extends IService<TmsCarrier> {

    Page<TmsCarrier> pageList(int page, int size, String carrierName, Integer carrierType, Boolean enabled);

    List<TmsCarrier> listEnabled();

    TmsCarrier getByCarrierCode(String carrierCode);
}
