package com.cognora.ecom.member.api;

import com.cognora.ecom.member.api.dto.MemberVO;
import com.cognora.ecom.member.api.request.RegisterRequest;

public interface MemberDubboService {

    MemberVO getMember(Long userId);

    MemberVO register(RegisterRequest request);

    void updateMemberLevel(Long userId, Integer level);

    void addPoints(Long userId, Integer points, String source);

    void deductPoints(Long userId, Integer points, String source);
}
