package com.cognora.platform.common.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 系统配置项（GET 合并视图 / PUT 请求体共用），对应前端 {@code SystemConfigDTO}。
 *
 * <p>{@code scope} 表示这一项来自哪一层：{@code PLATFORM} 平台级默认值，
 * {@code TENANT} 当前租户覆盖值。GET 时用于标明来源；PUT 时用于指定要写入的层级，
 * 省略时按 {@code TENANT} 处理。平台级行 {@code tenantId} 为 {@code null}。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysConfigItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 配置键，例如 {@code order.autoCancelMinutes}。 */
    @NotBlank(message = "配置键不能为空")
    @Pattern(regexp = "^[A-Za-z0-9._-]{1,128}$", message = "配置键只能包含字母、数字、点、下划线和短横线，长度1-128")
    private String configKey;

    /** 配置值，统一按字符串传输。 */
    @NotNull(message = "配置值不能为空")
    private String configValue;

    /** 值类型：STRING / NUMBER / BOOLEAN / JSON，默认 STRING。 */
    @Pattern(regexp = "STRING|NUMBER|BOOLEAN|JSON", message = "值类型只能是 STRING/NUMBER/BOOLEAN/JSON")
    private String valueType;

    /** 配置说明。 */
    @Size(max = 255, message = "配置说明不能超过255个字符")
    private String description;

    /** 来源/写入层级：PLATFORM 或 TENANT。 */
    @Pattern(regexp = "PLATFORM|TENANT", message = "scope 只能是 PLATFORM 或 TENANT")
    private String scope;

    /** 平台级为 {@code null}，租户覆盖为当前租户 ID。 */
    private UUID tenantId;

    /** 最近更新时间。 */
    private LocalDateTime updateTime;
}
