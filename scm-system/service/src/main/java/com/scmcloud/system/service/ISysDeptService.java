package com.scmcloud.system.service;

import com.scmcloud.common.dto.dept.DeptDTO;
import com.scmcloud.system.domain.entity.SysDept;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 閮ㄩ棬锟芥湇鍔★拷
 * </p>
 *
 * @author author
 * @since 2025-11-07
 */
public interface ISysDeptService extends IService<SysDept> {

    /**
     * 鏌ヨ閮ㄩ棬鏍戯紙鏍戝舰缁撴瀯锛屼粠鏍硅妭鐐瑰紑濮嬶級锟?
     *
     * @return 閮ㄩ棬鏍戝垪锟?
     */
    List<DeptDTO> getDeptTree();

    /**
     * 鏌ヨ鎸囧畾閮ㄩ棬鍙婂叾鎵€鏈夊瓙閮ㄩ棬ID锛堝寘鍚嚜韬級锟?
     *
     * @param deptId 閮ㄩ棬 ID
     * @return 閮ㄩ棬ID鍒楄〃锛堝寘鍚嚜韬強鎵€鏈夊瓙閮ㄩ棬锟?
     */
    List<UUID> getDeptAndChildren(UUID deptId);

    /**
     * 鏂板閮ㄩ棬锟?
     *
     * @param deptDTO 閮ㄩ棬淇℃伅
     */
    void addDept(DeptDTO deptDTO);

    /**
     * 淇敼閮ㄩ棬锟?
     *
     * @param deptDTO 閮ㄩ棬淇℃伅
     */
    void updateDept(DeptDTO deptDTO);

    /**
     * 鍒犻櫎閮ㄩ棬锟?
     *
     * @param id 閮ㄩ棬 ID
     */
    void deleteDept(UUID id);

    /**
     * 妫€鏌ラ儴闂ㄤ笅鏄惁瀛樺湪鐢ㄦ埛锟?
     *
     * @param deptId 閮ㄩ棬 ID
     * @return true 琛ㄧず瀛樺湪鐢ㄦ埛锛沠alse 琛ㄧず涓嶅瓨锟?
     */
    boolean hasUsers(UUID deptId);

    /**
     * 妫€鏌ラ儴闂ㄤ笅鏄惁瀛樺湪瀛愰儴闂拷
     *
     * @param deptId 閮ㄩ棬 ID
     * @return true 琛ㄧず瀛樺湪瀛愰儴闂紱false 琛ㄧず涓嶅瓨锟?
     */
    boolean hasChildren(UUID deptId);
}
