package com.cognora.scm.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.scm.purchase.domain.entity.PurRequestItem;

import java.util.List;

public interface IPurRequestItemService extends IService<PurRequestItem> {

    List<PurRequestItem> listByRequestId(String requestId);

    boolean deleteByRequestId(String requestId);
}
