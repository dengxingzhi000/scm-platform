package com.scmcloud.file.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量删除文件请求。
 */
@Data
public class FileBatchDeleteRequest {
    private List<String> ids;
}
