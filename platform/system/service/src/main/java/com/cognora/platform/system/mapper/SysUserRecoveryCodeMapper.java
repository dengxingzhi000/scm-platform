package com.cognora.platform.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.system.domain.entity.SysUserRecoveryCode;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.UUID;

@Mapper
@DS("user")
public interface SysUserRecoveryCodeMapper extends BaseMapper<SysUserRecoveryCode> {

    @Update("""
            UPDATE sys_user_recovery_code
            SET used = TRUE, used_at = NOW()
            WHERE user_id = #{userId}
              AND code_hash = #{codeHash}
              AND NOT used
            """)
    int consume(@Param("userId") UUID userId, @Param("codeHash") String codeHash);

    @Delete("DELETE FROM sys_user_recovery_code WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") UUID userId);
}
