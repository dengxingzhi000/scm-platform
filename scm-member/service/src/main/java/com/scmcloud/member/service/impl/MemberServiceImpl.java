package com.scmcloud.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.member.domain.entity.Member;
import com.scmcloud.member.mapper.MemberMapper;
import com.scmcloud.member.service.IMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements IMemberService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Member register(String userId, String nickname, String avatar) {
        log.info("Registering member: userId={}", userId);

        Member existing = getByUserId(userId);
        if (existing != null) {
            log.warn("Member already exists: userId={}", userId);
            return existing;
        }

        Member member = new Member();
        member.setUserId(userId);
        member.setMemberNo("MEM" + System.currentTimeMillis());
        member.setNickname(nickname);
        member.setAvatar(avatar);
        member.setGender(0);
        member.setMemberLevel(1);
        member.setPoints(0);
        member.setTotalSpent(BigDecimal.ZERO);
        member.setStatus(1);
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());

        save(member);
        log.info("Member registered: userId={}, memberNo={}", userId, member.getMemberNo());
        return member;
    }

    @Override
    public Member getByUserId(String userId) {
        return getOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberLevel(String userId, Integer level) {
        Member member = getByUserId(userId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + userId);
        }
        member.setMemberLevel(level);
        member.setUpdatedAt(LocalDateTime.now());
        updateById(member);
        log.info("Member level updated: userId={}, level={}", userId, level);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTotalSpent(String userId, BigDecimal amount) {
        Member member = getByUserId(userId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + userId);
        }
        member.setTotalSpent(member.getTotalSpent().add(amount));
        member.setUpdatedAt(LocalDateTime.now());
        updateById(member);
    }
}
