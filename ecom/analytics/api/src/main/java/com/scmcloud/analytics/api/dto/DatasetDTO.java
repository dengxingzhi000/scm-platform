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
public class DatasetDTO {
    private UUID id;
    private String datasetCode;
    private String datasetName;
    private String description;
    private String datasetType;
    private String sourceType;
    private String sourceTable;
    private String sourceConfig;
    private Integer status;
    private Boolean enabled;
    private String remark;
}