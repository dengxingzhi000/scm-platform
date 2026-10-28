package com.scmcloud.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysDictItem;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("permission")
public interface SysDictItemMapper extends BaseMapper<SysDictItem> {
}
