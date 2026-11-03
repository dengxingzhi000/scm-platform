package com.scmcloud.document.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.document.file.domain.entity.DocumentFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentFileMapper extends BaseMapper<DocumentFile> {
}
