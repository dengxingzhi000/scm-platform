package com.scmcloud.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysDictType;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("permission")
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {
}
