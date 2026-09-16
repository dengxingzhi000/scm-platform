package com.scmcloud.system.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.common.dto.permission.ApiPermissionDTO;
import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.system.domain.entity.SysPermission;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * 鏉冮檺锟芥湇鍔★拷
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
public interface ISysPermissionService extends IService<SysPermission> {

    /**
     * 鍒ゆ柇鐢ㄦ埛鏄惁鎷ユ湁鎸囧畾鏉冮檺缂栫爜锟?
     *
     * @param userId 鐢ㄦ埛 ID
     * @param permissionCode 鏉冮檺缂栫爜
     * @return true 琛ㄧず鎷ユ湁璇ユ潈闄愶紱false 琛ㄧず涓嶆嫢锟?
     */
    boolean hasPermission(UUID userId, String permissionCode);

    /**
     * 鍒ゆ柇鐢ㄦ埛鏄惁瀵规寚瀹氳祫婧愭嫢鏈夋煇椤规潈闄愶拷
     *
     * @param userId 鐢ㄦ埛 ID
     * @param resourceType 璧勬簮绫诲瀷锛堝锛歅ROJECT銆丏EPT 绛夛級
     * @param resourceId 璧勬簮ID锛堝彲搴忓垪鍖栵級
     * @param permission 鏉冮檺鍔ㄤ綔锛堝锛歊EAD銆乄RITE銆丏ELETE 绛夛級
     * @return true 琛ㄧず鎷ユ湁璇ヨ祫婧愭潈闄愶紱false 琛ㄧず涓嶆嫢锟?
     */
    boolean hasResourcePermission(UUID userId, String resourceType, Serializable resourceId, String permission);

    /**
     * 鏌ヨ鐢ㄦ埛鎵€鎷ユ湁鐨勮鑹茬紪鐮侀泦鍚堬拷
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 瑙掕壊缂栫爜闆嗗悎
     */
    Set<String> getUserRoles(UUID userId);

    /**
     * 鏌ヨ鐢ㄦ埛鎵€鎷ユ湁鐨勬潈闄愮紪鐮侀泦鍚堬拷
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏉冮檺缂栫爜闆嗗悎
     */
    Set<String> getUserPermissions(UUID userId);

    /**
     * 鑾峰彇鏉冮檺鏍戯紙鏍戝舰缁撴瀯锛夛拷
     *
     * @return 鏉冮檺鏍戝垪锟?
     */
    List<PermissionDTO> getPermissionTree();

    /**
     * 鏍规嵁ID鑾峰彇鏉冮檺璇︽儏锟?
     *
     * @param id 鏉冮檺 ID
     * @return 鏉冮檺璇︽儏
     */
    PermissionDTO getPermissionById(UUID id);

    /**
     * 鏍规嵁 API 璺緞锟紿TTP 鏂规硶鍖归厤鎵€闇€鐨勬潈闄愮紪鐮佸垪琛拷
     *
     * @param url 璇锋眰璺緞锛圥ath锟?
     * @param method HTTP 鏂规硶锛堝锛欸ET銆丳OST銆丳UT銆丏ELETE锟?
     * @return 璁块棶璇ユ帴鍙f墍闇€鐨勬潈闄愮紪鐮佸垪锟?
     */
    List<String> findPermissionsByUrl(String url, String method);

    /**
     * 鏌ヨ鎵€锟紸PI 绫诲瀷鐨勬潈闄愶拷
     * 鐢ㄤ簬鍔ㄦ€佹潈闄愬姞杞斤紙DynamicPermissionLoader锛夛拷
     *
     * @return API 鏉冮檺鍒楄〃锛屽寘鍚矾寰勩€丠TTP 鏂规硶鍜屾潈闄愮紪锟?
     */
    List<ApiPermissionDTO> findApiPermissions();

    /**
     * 鏂板鏉冮檺锟?
     *
     * @param permissionDTO 鏉冮檺淇℃伅
     */
    void addPermission(PermissionDTO permissionDTO);

    /**
     * 淇敼鏉冮檺锟?
     *
     * @param permissionDTO 鏉冮檺淇℃伅
     */
    void updatePermission(PermissionDTO permissionDTO);

    /**
     * 鍒犻櫎鏉冮檺锟?
     *
     * @param id 鏉冮檺 ID
     */
    void deletePermission(UUID id);
}
