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
public class DatasetFieldDTO {
    private UUID id;
    private UUID datasetId;
    private String fieldCode;
    private String fieldName;
    private String description;
    private String fieldType;
    private String dataType;
    private UUID metricId;
    private UUID dimensionId;
    private String expr;
    private Boolean isPartitionKey;
    private Boolean isHidden;
    private Boolean isRequired;
    private Integer sortOrder;
    private String defaultValue;
    private Integer status;
    private String remark;
}