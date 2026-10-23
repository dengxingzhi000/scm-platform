package com.scmcloud.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.promotion.domain.entity.Activity;
import com.scmcloud.promotion.domain.entity.GroupBuy;
import com.scmcloud.promotion.domain.entity.GroupBuyMember;
import com.scmcloud.promotion.mapper.ActivityMapper;
import com.scmcloud.promotion.mapper.GroupBuyMapper;
import com.scmcloud.promotion.mapper.GroupBuyMemberMapper;
import com.scmcloud.promotion.service.IGroupBuyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class GroupBuyServiceImpl extends ServiceImpl<GroupBuyMapper, GroupBuy> implements IGroupBuyService {

    private static final int GROUP_FORMING = 1;
    private static final int GROUP_SUCCESS = 2;
    private static final int MEMBER_JOINED = 1;
    private static final int DEFAULT_GROUP_SIZE = 2;
    private static final int DEFAULT_DURATION_HOURS = 24;

    private final ActivityMapper activityMapper;
    private final GroupBuyMemberMapper memberMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupBuy createGroup(Long userId, Long activityId, Long productId, Long skuId) {
        if (userId == null || activityId == null || productId == null) {
            throw new IllegalArgumentException("userId, activityId and productId are required");
        }
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("Activity not found: " + activityId);
        }
        if (activity.getStatus() == null || activity.getStatus() != 1) {
            throw new IllegalStateException("Activity is not active: " + activityId);
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            throw new IllegalStateException("Activity has not started: " + activityId);
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            throw new IllegalStateException("Activity has ended: " + activityId);
        }

        JsonNode rules = parseRules(activity.getRulesJson());
        int groupSize = Math.max(2, rules.path("groupSize").asInt(DEFAULT_GROUP_SIZE));
        int durationHours = Math.max(1, rules.path("durationHours").asInt(DEFAULT_DURATION_HOURS));
        BigDecimal groupPrice = rules.hasNonNull("groupPrice")
                ? new BigDecimal(rules.get("groupPrice").asText())
                : BigDecimal.ZERO;

        GroupBuy group = new GroupBuy()
                .setActivityId(activityId)
                .setProductId(productId)
                .setSkuId(skuId)
                .setGroupPrice(groupPrice)
                .setGroupSize(groupSize)
                .setCurrentSize(1)
                .setExpireTime(now.plusHours(durationHours))
                .setStatus(GROUP_FORMING)
                .setCreatedAt(now);
        save(group);

        memberMapper.insert(new GroupBuyMember()
                .setGroupBuyId(group.getId())
                .setUserId(String.valueOf(userId))
                .setStatus(MEMBER_JOINED)
                .setJoinedAt(now));
        log.info("Group buy created: groupId={}, activityId={}, userId={}, size={}",
                group.getId(), activityId, userId, groupSize);
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupBuy joinGroup(Long userId, Long groupBuyId) {
        if (userId == null || groupBuyId == null) {
            throw new IllegalArgumentException("userId and groupBuyId are required");
        }
        GroupBuy group = getById(groupBuyId);
        if (group == null) {
            throw new IllegalArgumentException("Group buy not found: " + groupBuyId);
        }
        if (group.getStatus() == null || group.getStatus() != GROUP_FORMING) {
            throw new IllegalStateException("Group buy is not open: " + groupBuyId);
        }
        if (group.getExpireTime() != null && LocalDateTime.now().isAfter(group.getExpireTime())) {
            group.setStatus(3);
            updateById(group);
            throw new IllegalStateException("Group buy has expired: " + groupBuyId);
        }

        GroupBuyMember existing = memberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getGroupBuyId, groupBuyId)
                .eq(GroupBuyMember::getUserId, String.valueOf(userId)));
        if (existing != null) {
            return group;
        }
        int current = group.getCurrentSize() == null ? 0 : group.getCurrentSize();
        if (current >= group.getGroupSize()) {
            throw new IllegalStateException("Group buy is full: " + groupBuyId);
        }

        memberMapper.insert(new GroupBuyMember()
                .setGroupBuyId(groupBuyId)
                .setUserId(String.valueOf(userId))
                .setStatus(MEMBER_JOINED)
                .setJoinedAt(LocalDateTime.now()));

        group.setCurrentSize(current + 1);
        if (group.getCurrentSize() >= group.getGroupSize()) {
            group.setStatus(GROUP_SUCCESS);
        }
        updateById(group);
        log.info("User {} joined group {}, currentSize={}/{}", userId, groupBuyId,
                group.getCurrentSize(), group.getGroupSize());
        return group;
    }

    @Override
    public GroupBuy getById(Long groupBuyId) {
        return super.getById(groupBuyId);
    }

    @Override
    public List<GroupBuyMember> listMembers(Long groupBuyId) {
        return memberMapper.selectList(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getGroupBuyId, groupBuyId)
                .orderByAsc(GroupBuyMember::getJoinedAt));
    }

    private JsonNode parseRules(String rulesJson) {
        if (rulesJson == null || rulesJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(rulesJson);
        } catch (Exception ex) {
            log.warn("Ignoring malformed activity rules JSON: {}", ex.getMessage());
            return objectMapper.createObjectNode();
        }
    }
}
