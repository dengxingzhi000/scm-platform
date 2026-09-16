package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysUserOauth;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

/**
 * OAuth绗笁鏂圭櫥褰曠粦锟組apper 鎺ュ彛
 *
 * @author Deng
 * @since 2025-12-15
 */
@Mapper
@DS("user")
public interface SysUserOauthMapper extends BaseMapper<SysUserOauth> {

    /**
     * 鏍规嵁 OAuth鎻愪緵鍟嗗拰OpenID鏌ヨ缁戝畾淇℃伅
     */
    @Select("""
            SELECT * FROM sys_user_oauth
            WHERE provider = #{provider} AND oauth_openid = #{openid} AND NOT deleted
            """)
    SysUserOauth findByProviderAndOpenid(@Param("provider") String provider, @Param("openid") String openid);

    /**
     * 鏍规嵁鐢ㄦ埛 ID鏌ヨ鎵€鏈塐Auth缁戝畾
     */
    @Select("""
            SELECT * FROM sys_user_oauth
            WHERE user_id = #{userId} AND NOT deleted
            """)
    List<SysUserOauth> findByUserId(@Param("userId") UUID userId);

    /**
     * 鏍规嵁鐢ㄦ埛 ID鍜屾彁渚涘晢鏌ヨ缁戝畾淇℃伅
     */
    @Select("""
            SELECT * FROM sys_user_oauth
            WHERE user_id = #{userId} AND provider = #{provider} AND NOT deleted
            """)
    SysUserOauth findByUserIdAndProvider(@Param("userId") UUID userId, @Param("provider") String provider);

    /**
     * 妫€鏌ョ敤鎴锋槸鍚﹀凡缁戝畾鎸囧畾鎻愪緵锟?
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_user_oauth
            WHERE user_id = #{userId} AND provider = #{provider} AND NOT deleted
            """)
    boolean existsByUserIdAndProvider(@Param("userId") UUID userId, @Param("provider") String provider);

    /**
     * 鏇存柊鏈€鍚庣櫥褰曟椂锟?
     */
    @Update("""
            UPDATE sys_user_oauth
            SET last_login_time = NOW(), update_time = NOW()
            WHERE id = #{id}
            """)
    int updateLastLoginTime(@Param("id") UUID id);

    /**
     * 瑙g粦 OAuth璐﹀彿
     */
    @Update("""
            UPDATE sys_user_oauth
            SET deleted = true, update_time = NOW()
            WHERE user_id = #{userId} AND provider = #{provider}
            """)
    int unbind(@Param("userId") UUID userId, @Param("provider") String provider);
}
