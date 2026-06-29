package com.scmcloud.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.promotion.domain.entity.GroupBuy;

public interface IGroupBuyService extends IService<GroupBuy> {

    GroupBuy createGroup(Long userId, Long activityId, Long skuId);

    GroupBuy joinGroup(Long userId, Long groupBuyId);

    GroupBuy getById(Long groupBuyId);
}
