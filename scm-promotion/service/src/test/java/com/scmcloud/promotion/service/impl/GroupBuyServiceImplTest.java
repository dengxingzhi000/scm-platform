package com.scmcloud.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.scmcloud.promotion.domain.entity.Activity;
import com.scmcloud.promotion.domain.entity.GroupBuy;
import com.scmcloud.promotion.domain.entity.GroupBuyMember;
import com.scmcloud.promotion.mapper.ActivityMapper;
import com.scmcloud.promotion.mapper.GroupBuyMapper;
import com.scmcloud.promotion.mapper.GroupBuyMemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupBuyServiceImplTest {

    @Mock private GroupBuyMapper groupBuyMapper;
    @Mock private ActivityMapper activityMapper;
    @Mock private GroupBuyMemberMapper memberMapper;

    private GroupBuyServiceImpl groupBuyService;

    @BeforeEach
    void setUp() {
        groupBuyService = new GroupBuyServiceImpl(activityMapper, memberMapper);
        ReflectionTestUtils.setField(groupBuyService, "baseMapper", groupBuyMapper);
    }

    @Test
    void createsGroupUsingActivityRules() {
        when(activityMapper.selectById(7L)).thenReturn(activeActivity());
        when(groupBuyMapper.insert(any(GroupBuy.class))).thenAnswer(invocation -> {
            GroupBuy group = invocation.getArgument(0);
            group.setId(100L);
            return 1;
        });
        when(memberMapper.insert(any(GroupBuyMember.class))).thenReturn(1);

        GroupBuy group = groupBuyService.createGroup(1L, 7L, 8L, 9L);

        assertEquals(3, group.getGroupSize());
        assertEquals(1, group.getCurrentSize());
        assertEquals(1, group.getStatus());
        assertEquals(0, new BigDecimal("99.00").compareTo(group.getGroupPrice()));
        assertNotNull(group.getExpireTime());
        verify(memberMapper).insert(any(GroupBuyMember.class));
    }

    @Test
    void joinGroupCompletesWhenCapacityReached() {
        GroupBuy group = new GroupBuy()
                .setId(100L)
                .setActivityId(7L)
                .setGroupSize(2)
                .setCurrentSize(1)
                .setStatus(1)
                .setExpireTime(LocalDateTime.now().plusHours(1));
        when(groupBuyMapper.selectById(100L)).thenReturn(group);
        when(memberMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(memberMapper.insert(any(GroupBuyMember.class))).thenReturn(1);
        when(groupBuyMapper.updateById(any(GroupBuy.class))).thenReturn(1);

        GroupBuy joined = groupBuyService.joinGroup(2L, 100L);

        assertEquals(2, joined.getCurrentSize());
        assertEquals(2, joined.getStatus());
    }

    @Test
    void duplicateJoinReturnsGroupWithoutInsertingMember() {
        GroupBuy group = new GroupBuy()
                .setId(100L)
                .setGroupSize(3)
                .setCurrentSize(1)
                .setStatus(1)
                .setExpireTime(LocalDateTime.now().plusHours(1));
        when(groupBuyMapper.selectById(100L)).thenReturn(group);
        when(memberMapper.selectOne(any(Wrapper.class)))
                .thenReturn(new GroupBuyMember().setUserId("2"));

        GroupBuy joined = groupBuyService.joinGroup(2L, 100L);

        assertEquals(1, joined.getCurrentSize());
        verify(memberMapper, never()).insert(any(GroupBuyMember.class));
    }

    @Test
    void rejectsExpiredGroup() {
        GroupBuy group = new GroupBuy()
                .setId(100L)
                .setGroupSize(3)
                .setCurrentSize(1)
                .setStatus(1)
                .setExpireTime(LocalDateTime.now().minusHours(1));
        when(groupBuyMapper.selectById(100L)).thenReturn(group);
        when(groupBuyMapper.updateById(any(GroupBuy.class))).thenReturn(1);

        assertThrows(IllegalStateException.class, () -> groupBuyService.joinGroup(2L, 100L));
        assertEquals(3, group.getStatus());
    }

    @Test
    void listsMembersInJoinOrder() {
        when(memberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(new GroupBuyMember()));

        assertEquals(1, groupBuyService.listMembers(100L).size());
    }

    private Activity activeActivity() {
        return new Activity()
                .setId(7L)
                .setActivityName("Group buy")
                .setStatus(1)
                .setStartTime(LocalDateTime.now().minusHours(1))
                .setEndTime(LocalDateTime.now().plusDays(1))
                .setRulesJson("{\"groupSize\":3,\"groupPrice\":99.00,\"durationHours\":12}");
    }
}
