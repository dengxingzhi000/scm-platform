package com.scmcloud.common.security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scmcloud.common.security.annotation.EncryptField;
import com.scmcloud.common.web.annotation.Sensitive;
import com.scmcloud.common.web.enums.SensitiveType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
/**
 * 鐢ㄦ埛DTO - 甯︽暟鎹劚鏁忓拰鍔犲瘑
 *
 * @author Deng
 * createData 2025/10/30 11:44
 * @version 1.0
 */
@Data
public class UserDTOWithSensitive {
    private UUID id;

    @NotBlank(message = "Username cannot be empty")
    @Size(min = 4, max = 32, message = "Username length must be 4-32 characters")
    private String username;

    @NotBlank(message = "Real name cannot be empty")
    @Sensitive(type = SensitiveType.NAME)  // 濮撳悕鑴辨晱锛氬紶**
    private String realName;

    @EncryptField  // 鏁版嵁搴撳瓨鍌ㄥ姞锟?
    @Sensitive(type = SensitiveType.ID_CARD)  // 鍝嶅簲鑴辨晱锟?0101********1234
    @Pattern(
            regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$",
            message = "ID card number format is incorrect"
    )
    private String idCard;

    @Sensitive(type = SensitiveType.EMAIL)  // 閭鑴辨晱锛歛bc****@example.com
    @Email(message = "Email format is incorrect")
    private String email;

    @Sensitive(type = SensitiveType.MOBILE)  // 鎵嬫満鍙疯劚鏁忥細138****1234
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Phone number format is incorrect")
    private String phone;

    @EncryptField  // 閾惰鍗″彿鍔犲瘑瀛樺偍
    @Sensitive(type = SensitiveType.BANK_CARD)  // 鍝嶅簲鑴辨晱锟?22 **** **** 1234
    private String bankCard;

    @Sensitive(type = SensitiveType.ADDRESS)  // 鍦板潃鑴辨晱锛氫繚鐣欏墠6锟?
    private String address;

    private String avatar;
    private Integer status;
    private UUID deptId;
    private String deptName;
    private Integer userLevel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginTime;

    private String lastLoginIp;
    private List<UUID> roleIds;
    private List<String> roleNames;
}