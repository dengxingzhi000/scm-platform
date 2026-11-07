package com.cognora.platform.system.service.support;

import com.cognora.platform.common.dto.user.UserDetailDTO;
import com.cognora.platform.common.web.enums.SensitiveType;

/**
 * 详情响应的服务层脱敏。只有持有 {@code system:user:view:sensitive} 的调用方
 * 才能看到完整手机号/邮箱/身份证/姓名。
 */
public final class UserSensitiveMasker {

    private UserSensitiveMasker() {
    }

    public static void mask(UserDetailDTO dto) {
        if (dto == null) {
            return;
        }
        dto.setRealName(SensitiveType.NAME.desensitize(dto.getRealName()));
        dto.setIdCard(SensitiveType.ID_CARD.desensitize(dto.getIdCard()));
        dto.setEmail(SensitiveType.EMAIL.desensitize(dto.getEmail()));
        dto.setPhone(SensitiveType.MOBILE.desensitize(dto.getPhone()));
    }
}
