package com.cognora.platform.document.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.document.file.domain.entity.DocumentFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentFileMapper extends BaseMapper<DocumentFile> {
}
