package com.scmcloud.system.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * OAuth 绗笁鏂圭櫥褰曠粦瀹氳〃
 *
 * @author Deng
 * @since 2025-12-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "sys_user_oauth", autoResultMap = true)
public class SysUserOauth {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private UUID id;

    private UUID userId;

    private String provider;

    @TableField("oauth_openid")
    private String oauthOpenid;

    private String oauthUnionId;

    private String oauthEmail;

    private String oauthNickname;

    private String oauthAvatar;

    private String accessToken;

    private String refreshToken;

    private LocalDateTime tokenExpireTime;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> rawUserInfo;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private LocalDateTime lastLoginTime;

    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;

    /**
     * OAuth 鎻愪緵鍟嗘灇锟?
     */
    @Getter
    public enum Provider {
        GOOGLE("google"),
        GITHUB("github"),
        APPLE("apple"),
        WECHAT("wechat"),
        DINGTALK("dingtalk"),
        FEISHU("feishu");

        private final String code;

        Provider(String code) {
            this.code = code;
        }
    }
}
