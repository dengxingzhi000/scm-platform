package com.cognora.platform.common.dto.user;

import com.cognora.platform.common.web.annotation.Sensitive;
import com.cognora.platform.common.web.enums.SensitiveType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * 修改用户请求。
 *
 * <p>字段为 null 表示不修改。用户名、密码、状态、租户、登录失败次数、锁定信息与
 * 数据范围不在本 DTO 中，必须通过独立接口维护。</p>
 */
@Data
public class UpdateUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 乐观锁版本号，来自详情接口；版本不一致返回 409。 */
    @NotNull(message = "版本号不能为空")
    @Min(value = 0, message = "版本号不能为负数")
    private Integer version;

    @Size(max = 64, message = "姓名长度不能超过64个字符")
    @Sensitive(type = SensitiveType.NAME)
    private String realName;

    @Pattern(
            regexp = "^$|^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$",
            message = "身份证号格式不正确"
    )
    @Sensitive(type = SensitiveType.ID_CARD)
    private String idCard;

    @Email(message = "邮箱格式不正确")
    @Sensitive(type = SensitiveType.EMAIL)
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Sensitive(type = SensitiveType.MOBILE)
    private String phone;

    private String avatar;

    private String timezone;

    private UUID deptId;

    private Integer userLevel;

    private Integer accountType;

    private String remark;
}
