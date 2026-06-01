package com.frog.audit.api.dto;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AuditQueryResult {

    private List<AuditLogVO> items;
    private long total;
    private int page;
    private int size;
}
