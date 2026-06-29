package com.scmcloud.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.member.domain.entity.Member;

public interface IMemberService extends IService<Member> {

    Member register(String userId, String nickname, String avatar);

    Member getByUserId(String userId);

    void updateMemberLevel(String userId, Integer level);

    void addTotalSpent(String userId, java.math.BigDecimal amount);
}
