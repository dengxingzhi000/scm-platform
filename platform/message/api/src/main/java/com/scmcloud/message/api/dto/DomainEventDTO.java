package com.scmcloud.message.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
public class DomainEventDTO implements Serializable {
    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private Long tenantId;
    private Date timestamp;
    private Map<String, Object> payload;
}
