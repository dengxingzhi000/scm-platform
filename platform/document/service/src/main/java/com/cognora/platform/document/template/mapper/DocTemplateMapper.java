package com.cognora.platform.document.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.document.template.domain.entity.DocTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocTemplateMapper extends BaseMapper<DocTemplate> {
}
