package com.cognora.platform.system.service;

import com.cognora.platform.common.dto.config.SysConfigItemDTO;

import java.util.List;

/**
 * 平台级默认配置 + 当前租户覆盖的系统配置服务。
 *
 * @author Deng
 * @since 2026-11-06
 */
public interface ISysConfigService {

    /**
     * 返回平台级默认值与当前租户覆盖的合并视图；同键时租户覆盖优先，并标明每项来源。
     */
    List<SysConfigItemDTO> getMergedConfig();

    /**
     * 写入单个配置项。{@code scope=TENANT}（默认）写当前租户覆盖，
     * {@code scope=PLATFORM} 写平台级默认值且仅平台超级管理员可调用。
     *
     * @return 写入后的配置项（含来源与更新时间）
     */
    SysConfigItemDTO updateConfig(SysConfigItemDTO request);
}
