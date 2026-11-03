package com.scmcloud.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 退款拒绝请求（业务操作）。
 *
 * @author SCM Platform Team
 */
@Data
public class RefundRejectRequest {
    @NotBlank(message = "审核人 ID 不能为空")
    private String handlerId;

    @Size(max = 128)
    private String handlerName;

    @NotBlank(message = "拒绝原因不能为空")
    @Size(max = 500)
    private String handlerRemark;
}
