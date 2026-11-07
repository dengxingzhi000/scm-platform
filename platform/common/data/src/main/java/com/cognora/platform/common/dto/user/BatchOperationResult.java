package com.cognora.platform.common.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * 批量操作结果：逐行返回成功/失败，单行失败不会回滚其他行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private int total;
    private int succeeded;
    private int failed;
    private List<BatchUserResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchUserResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private UUID userId;
        private boolean success;
        private Integer code;
        private String message;
    }
}
