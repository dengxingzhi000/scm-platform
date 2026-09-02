package com.scmcloud.analytics.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricDTO {
    private UUID id;
    private String metricCode;
    private String metricName;
    private String description;
    private String metricType;
    private String dataType;
    private String aggFunc;
    private String expr;
    private String unit;
    private String format;
    private Boolean visible;
    private Integer status;
    private Integer sortOrder;
    private String remark;
}