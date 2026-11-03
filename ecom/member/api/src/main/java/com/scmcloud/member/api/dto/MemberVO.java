package com.scmcloud.member.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MemberVO {

    private Long id;
    private String userId;
    private String memberNo;
    private String nickname;
    private String avatar;
    private Integer gender;
    private LocalDate birthday;
    private Integer memberLevel;
    private Integer points;
    private BigDecimal totalSpent;
    private Integer status;
}
