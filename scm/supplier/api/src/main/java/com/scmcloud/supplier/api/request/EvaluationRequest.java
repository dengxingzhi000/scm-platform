package com.scmcloud.supplier.api.request;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 供应商评估请求
 */
@Data
@Accessors(chain = true)
public class EvaluationRequest {

    private Integer qualityScore;
    private Integer deliveryScore;
    private Integer serviceScore;
    private String comment;
    private Long evaluatorId;
}
