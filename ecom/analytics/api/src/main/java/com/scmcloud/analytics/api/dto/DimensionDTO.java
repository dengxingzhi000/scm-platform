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
public class DimensionDTO {
    private UUID id;
    private String dimCode;
    private String dimName;
    private String description;
    private String dimType;
    private String dataType;
    private UUID parentDimId;
    private Integer hierarchyLevel;
    private Boolean isRequired;
    private String dictCode;
    private Integer status;
    private String remark;
}