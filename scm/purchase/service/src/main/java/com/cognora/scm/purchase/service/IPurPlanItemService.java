package com.cognora.scm.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.scm.purchase.domain.entity.PurPlanItem;

import java.util.List;

public interface IPurPlanItemService extends IService<PurPlanItem> {

    List<PurPlanItem> listByPlanId(String planId);

    boolean deleteByPlanId(String planId);
}
