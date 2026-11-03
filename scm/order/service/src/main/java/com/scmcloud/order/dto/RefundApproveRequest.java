package com.scmcloud.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * 退款审核请求（业务操作）。
 *
 * @author SCM Platform Team
 */
@Data
public class RefundApproveRequest {
    @NotBlank(message = "审核人 ID 不能为空")
    private String handlerId;

    @Size(max = 128)
    private String handlerName;

    @Size(max = 500)
    private String handlerRemark;
}
