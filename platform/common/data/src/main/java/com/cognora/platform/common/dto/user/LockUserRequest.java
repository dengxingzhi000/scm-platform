package com.cognora.platform.common.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 锁定用户请求。
 */
@Data
public class LockUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Size(max = 255, message = "锁定原因不能超过255个字符")
    private String reason;

    /** 锁定时长（分钟），默认 1440（24 小时）。 */
    @Min(value = 1, message = "锁定时长必须大于0")
    @Max(value = 525600, message = "锁定时长不能超过一年")
    private Integer durationMinutes = 1440;
}
