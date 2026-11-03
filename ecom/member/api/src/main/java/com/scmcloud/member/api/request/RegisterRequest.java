package com.scmcloud.member.api.request;

import lombok.Data;

@Data
public class RegisterRequest {

    private String userId;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
}
