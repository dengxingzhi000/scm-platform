package com.scmcloud.supplier.dto;

import lombok.Data;

/**
 * 供应商准入审核请求。
 */
@Data
public class SupplierAuditRequest {

    /** APPROVE-通过；REJECT-驳回。 */
    private String action;
    private String operatorId;
    private String operatorName;
    private String remark;
}
