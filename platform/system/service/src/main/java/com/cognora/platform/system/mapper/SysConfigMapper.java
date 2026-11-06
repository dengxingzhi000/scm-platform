package com.cognora.platform.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.system.domain.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统配置 Mapper。
 *
 * <p>表在 {@code db_user}（system 的主数据源），不走 {@code @DS} 路由。</p>
 *
 * @author Deng
 * @since 2026-11-06
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}
