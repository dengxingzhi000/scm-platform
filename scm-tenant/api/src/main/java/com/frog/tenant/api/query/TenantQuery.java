package com.frog.tenant.api.query;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TenantQuery {

    private String tenantName;
    private Integer tenantType;
    private Integer status;
    private String industry;
    private int pageNum = 1;
    private int pageSize = 20;
    private String orderBy;
    private boolean asc = false;
}
