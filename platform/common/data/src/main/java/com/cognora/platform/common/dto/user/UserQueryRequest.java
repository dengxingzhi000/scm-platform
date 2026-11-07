package com.cognora.platform.common.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户列表查询条件。
 *
 * <p>排序字段只允许白名单值，避免把请求参数直接拼进 ORDER BY。</p>
 */
@Data
public class UserQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer size = 10;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private UUID deptId;

    private UUID roleId;

    private Integer status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createTimeFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createTimeTo;

    @Pattern(
            regexp = "^(?:createTime|lastLoginTime|username|realName|status)?$",
            message = "排序字段不在允许范围内"
    )
    private String sortBy;

    @Pattern(regexp = "^(?:asc|desc|ASC|DESC)?$", message = "排序方向只能是 asc 或 desc")
    private String sortOrder;
}
