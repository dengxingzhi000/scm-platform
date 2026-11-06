package com.cognora.platform.common.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 撤销其他设备会话的结果。
 *
 * @author Deng
 * @since 2026-11-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoutOthersResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 实际撤销的会话数量（不含当前会话）。 */
    private int revokedCount;
}
