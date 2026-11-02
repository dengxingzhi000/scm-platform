package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.decision.entity.WeightProfileEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("permission")
public interface WeightProfileMapper extends BaseMapper<WeightProfileEntity> {
}
