package com.scmcloud.common.dto.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * API 鏉冮檺 DTO
 * 鐢ㄤ簬鍔ㄦ€佹潈闄愬姞杞斤紝浠呭寘锟紸PI 鏉冮檺鏍¢獙鎵€闇€鐨勬牳蹇冨瓧锟?
 *
 * @author Deng
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiPermissionDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * API 璺緞
     */
    private String apiPath;

    /**
     * HTTP 鏂规硶 (GET, POST, PUT, DELETE 锟?
     */
    private String httpMethod;

    /**
     * 鏉冮檺缂栫爜
     */
    private String permissionCode;
}