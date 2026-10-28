package com.scmcloud.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysStatusDict;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("permission")
public interface SysStatusDictMapper extends BaseMapper<SysStatusDict> {
}
