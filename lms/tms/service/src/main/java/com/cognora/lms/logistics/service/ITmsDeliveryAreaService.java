package com.cognora.lms.logistics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.lms.logistics.domain.entity.TmsDeliveryArea;

import java.util.List;

public interface ITmsDeliveryAreaService extends IService<TmsDeliveryArea> {

    Page<TmsDeliveryArea> pageList(int page, int size, String carrierId, String province, String city);

    List<TmsDeliveryArea> listByCarrier(String carrierId);

    boolean checkCoverage(String carrierId, String province, String city, String district);

    TmsDeliveryArea createArea(TmsDeliveryArea area);

    TmsDeliveryArea updateArea(String id, TmsDeliveryArea area);

    void deleteArea(String id);
}
