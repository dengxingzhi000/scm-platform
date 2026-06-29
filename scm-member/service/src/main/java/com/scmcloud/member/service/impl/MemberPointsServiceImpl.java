package com.scmcloud.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.member.domain.entity.Member;
import com.scmcloud.member.domain.entity.MemberPointsLog;
import com.scmcloud.member.mapper.MemberPointsLogMapper;
import com.scmcloud.member.service.IMemberPointsService;
import com.scmcloud.member.service.IMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class MemberPointsServiceImpl extends ServiceImpl<MemberPointsLogMapper, MemberPointsLog> implements IMemberPointsService {

    private final IMemberService memberService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(String userId, Integer points, String source, String orderNo, String description) {
        log.info("Adding points: userId={}, points={}, source={}", userId, points, source);

        Member member = memberService.getByUserId(userId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + userId);
        }

        member.setPoints(member.getPoints() + points);
        member.setUpdatedAt(LocalDateTime.now());
        memberService.updateById(member);

        MemberPointsLog logEntry = new MemberPointsLog();
        logEntry.setUserId(userId);
        logEntry.setPoints(points);
        logEntry.setType("EARN");
        logEntry.setSource(source);
        logEntry.setOrderNo(orderNo);
        logEntry.setDescription(description);
        logEntry.setCreatedAt(LocalDateTime.now());
        save(logEntry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(String userId, Integer points, String source, String orderNo, String description) {
        log.info("Deducting points: userId={}, points={}, source={}", userId, points, source);

        Member member = memberService.getByUserId(userId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + userId);
        }

        if (member.getPoints() < points) {
            throw new IllegalStateException("Insufficient points: available=" + member.getPoints() + ", required=" + points);
        }

        member.setPoints(member.getPoints() - points);
        member.setUpdatedAt(LocalDateTime.now());
        memberService.updateById(member);

        MemberPointsLog logEntry = new MemberPointsLog();
        logEntry.setUserId(userId);
        logEntry.setPoints(-points);
        logEntry.setType("DEDUCT");
        logEntry.setSource(source);
        logEntry.setOrderNo(orderNo);
        logEntry.setDescription(description);
        logEntry.setCreatedAt(LocalDateTime.now());
        save(logEntry);
    }

    @Override
    public List<MemberPointsLog> getByUserId(String userId) {
        return list(new LambdaQueryWrapper<MemberPointsLog>()
                .eq(MemberPointsLog::getUserId, userId)
                .orderByDesc(MemberPointsLog::getCreatedAt));
    }

    @Override
    public int getPointsBalance(String userId) {
        Member member = memberService.getByUserId(userId);
        return member != null ? member.getPoints() : 0;
    }
}
