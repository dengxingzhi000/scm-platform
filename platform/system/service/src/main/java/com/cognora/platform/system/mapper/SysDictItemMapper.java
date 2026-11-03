package com.cognora.platform.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.system.domain.entity.SysDictItem;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("permission")
public interface SysDictItemMapper extends BaseMapper<SysDictItem> {
}
