package com.scmcloud.promotion.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.promotion.domain.entity.GroupBuy;
import com.scmcloud.promotion.domain.entity.GroupBuyMember;

import java.util.List;

public interface IGroupBuyService extends IService<GroupBuy> {

    GroupBuy createGroup(Long userId, Long activityId, Long productId, Long skuId);

    GroupBuy joinGroup(Long userId, Long groupBuyId);

    GroupBuy getById(Long groupBuyId);

    List<GroupBuyMember> listMembers(Long groupBuyId);
}
