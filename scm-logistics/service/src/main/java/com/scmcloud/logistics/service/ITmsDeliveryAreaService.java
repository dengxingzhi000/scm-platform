package com.scmcloud.logistics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.logistics.domain.entity.TmsDeliveryArea;

import java.util.List;

public interface ITmsDeliveryAreaService extends IService<TmsDeliveryArea> {

    Page<TmsDeliveryArea> pageList(int page, int size, String carrierId, String province, String city);

    List<TmsDeliveryArea> listByCarrier(String carrierId);

    boolean checkCoverage(String carrierId, String province, String city, String district);
}
