package com.cognora.ecom.member.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.ecom.member.domain.entity.Member;

import java.time.LocalDate;

public interface IMemberService extends IService<Member> {

    Member register(String userId, String nickname, String avatar);

    Member getByUserId(String userId);

    Member updateProfile(String userId, String nickname, String avatar,
                         Integer gender, LocalDate birthday);

    void updateMemberLevel(String userId, Integer level);

    void addTotalSpent(String userId, java.math.BigDecimal amount);

    Page<Member> pageQuery(int page, int size, String keyword, Integer memberLevel, Integer status);
}
