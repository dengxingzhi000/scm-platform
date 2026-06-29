package com.scmcloud.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.member.domain.entity.MemberPointsLog;

import java.util.List;

public interface IMemberPointsService extends IService<MemberPointsLog> {

    void addPoints(String userId, Integer points, String source, String orderNo, String description);

    void deductPoints(String userId, Integer points, String source, String orderNo, String description);

    List<MemberPointsLog> getByUserId(String userId);

    int getPointsBalance(String userId);
}
