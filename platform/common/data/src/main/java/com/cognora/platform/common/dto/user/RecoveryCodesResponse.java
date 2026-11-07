package com.cognora.platform.common.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 恢复码只在生成时展示一次，服务端仅保存哈希。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryCodesResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<String> codes;
}
