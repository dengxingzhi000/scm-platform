package com.scmcloud.purchase.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.purchase.domain.entity.PurPlanItem;

import java.util.List;

public interface IPurPlanItemService extends IService<PurPlanItem> {

    List<PurPlanItem> listByPlanId(String planId);

    boolean deleteByPlanId(String planId);
}
