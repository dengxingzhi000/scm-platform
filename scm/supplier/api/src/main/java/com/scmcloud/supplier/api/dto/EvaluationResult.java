package com.scmcloud.supplier.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 评估结果
 */
@Data
@Accessors(chain = true)
public class EvaluationResult {

    private Long evaluationId;
    private Integer totalScore;
    private Integer level;
    private String message;
}
