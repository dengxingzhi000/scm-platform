package com.cognora.platform.common.dto.user;

import com.cognora.platform.common.web.annotation.Sensitive;
import com.cognora.platform.common.web.enums.SensitiveType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * 新增用户请求。
 *
 * <p>只包含允许由调用方写入的字段；状态、登录失败次数、锁定信息、tenantId
 * 均由服务端生成或由独立接口维护，避免批量赋值。</p>
 */
@Data
public class CreateUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度4-32个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /** 可选；为空时使用系统默认密码。密码复杂度由 PasswordPolicyService 校验。 */
    @Size(min = 8, max = 64, message = "密码长度8-64个字符")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
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

    /** 用户时区（IANA 名称，如 Asia/Shanghai）。 */
    private String timezone;

    private UUID deptId;

    private Integer userLevel;

    private Integer accountType;

    private String remark;

    private List<UUID> roleIds;
}
