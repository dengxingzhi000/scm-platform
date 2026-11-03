package com.scmcloud.fulfillment.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.fulfillment.domain.entity.Tracking;

import java.util.List;

public interface ITrackingService extends IService<Tracking> {

    Tracking recordEvent(Tracking tracking);

    List<Tracking> getByTrackingNo(String trackingNo);

    List<Tracking> getByFulfillmentNo(String fulfillmentNo);

    Tracking getLatest(String fulfillmentNo);
}
