package com.cognora.ecom.promotion.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.ecom.promotion.domain.entity.GroupBuy;
import com.cognora.ecom.promotion.domain.entity.GroupBuyMember;

import java.util.List;

public interface IGroupBuyService extends IService<GroupBuy> {

    GroupBuy createGroup(String userId, Long activityId, Long productId, Long skuId);

    GroupBuy joinGroup(String userId, Long groupBuyId);

    GroupBuy getById(Long groupBuyId);

    List<GroupBuyMember> listMembers(Long groupBuyId);
}
