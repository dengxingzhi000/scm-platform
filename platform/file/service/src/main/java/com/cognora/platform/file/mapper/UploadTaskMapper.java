package com.cognora.platform.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.file.entity.UploadTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UploadTaskMapper extends BaseMapper<UploadTask> {
}
