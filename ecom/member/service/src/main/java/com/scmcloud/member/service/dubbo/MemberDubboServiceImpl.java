package com.scmcloud.member.service.dubbo;

import com.scmcloud.member.api.MemberDubboService;
import com.scmcloud.member.api.dto.MemberVO;
import com.scmcloud.member.api.request.RegisterRequest;
import com.scmcloud.member.domain.entity.Member;
import com.scmcloud.member.service.IMemberPointsService;
import com.scmcloud.member.service.IMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

@RequiredArgsConstructor
@Slf4j
@DubboService
public class MemberDubboServiceImpl implements MemberDubboService {

    private final IMemberService memberService;
    private final IMemberPointsService pointsService;

    @Override
    public MemberVO getMember(Long userId) {
        Member member = memberService.getByUserId(String.valueOf(userId));
        if (member == null) {
            return null;
        }
        return convertToVO(member);
    }

    @Override
    public MemberVO register(RegisterRequest request) {
        Member member = memberService.register(request.getUserId(), request.getNickname(), request.getAvatar());
        return convertToVO(member);
    }

    @Override
    public void updateMemberLevel(Long userId, Integer level) {
        memberService.updateMemberLevel(String.valueOf(userId), level);
    }

    @Override
    public void addPoints(Long userId, Integer points, String source) {
        pointsService.addPoints(String.valueOf(userId), points, source, null, null);
    }

    @Override
    public void deductPoints(Long userId, Integer points, String source) {
        pointsService.deductPoints(String.valueOf(userId), points, source, null, null);
    }

    private MemberVO convertToVO(Member member) {
        MemberVO vo = new MemberVO();
        vo.setId(member.getId());
        vo.setUserId(member.getUserId());
        vo.setMemberNo(member.getMemberNo());
        vo.setNickname(member.getNickname());
        vo.setAvatar(member.getAvatar());
        vo.setGender(member.getGender());
        vo.setBirthday(member.getBirthday());
        vo.setMemberLevel(member.getMemberLevel());
        vo.setPoints(member.getPoints());
        vo.setTotalSpent(member.getTotalSpent());
        vo.setStatus(member.getStatus());
        return vo;
    }
}
