package com.cognora.platform.common.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * CSV 导入结果：逐行返回失败原因，成功行不因其他行失败而回滚。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserImportResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private int total;
    private int created;
    private int failed;
    private List<RowError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError implements Serializable {
        private static final long serialVersionUID = 1L;

        private int rowNumber;
        private String username;
        private Integer code;
        private String message;
    }
}
