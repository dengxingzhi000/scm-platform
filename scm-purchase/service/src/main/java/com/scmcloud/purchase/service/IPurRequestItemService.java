package com.scmcloud.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.purchase.domain.entity.PurRequestItem;

import java.util.List;

public interface IPurRequestItemService extends IService<PurRequestItem> {

    List<PurRequestItem> listByRequestId(String requestId);

    boolean deleteByRequestId(String requestId);
}
