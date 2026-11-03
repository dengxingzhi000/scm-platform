package com.scmcloud.system.sharding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 跨库分页查询
 */
@Service
@RequiredArgsConstructor
public class ShardingUserService {
    private final SysUserMapper userMapper;

    public Page<SysUser> pageQuery(Integer pageNum, Integer pageSize, String username) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.isNotBlank(username), SysUser::getUsername, username)
                .orderByDesc(SysUser::getCreateTime);
        return userMapper.selectPage(page, wrapper);
    }

    public SysUser getById(UUID userId) {
        return userMapper.selectById(userId);
    }
}

