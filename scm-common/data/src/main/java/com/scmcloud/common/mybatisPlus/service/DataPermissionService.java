package com.scmcloud.common.mybatisPlus.service;

import java.util.List;
import java.util.UUID;

/**
 * 鏁版嵁鏉冮檺鏈嶅姟鎺ュ彛
 * 鐢ㄤ簬鏌ヨ鐢ㄦ埛鐨勮嚜瀹氫箟鏁版嵁鏉冮檺瑙勫垯
 *
 * @author Deng
 * @since 2025-12-15
 */
public interface DataPermissionService {

    /**
     * 鏌ヨ鐢ㄦ埛鐨勮嚜瀹氫箟鏁版嵁鏉冮檺閮ㄩ棬鍒楄〃
     * 锟絪ys_role_dept 琛ㄦ煡璇㈢敤鎴烽€氳繃瑙掕壊鑾峰緱鐨勫彲璁块棶閮ㄩ棬
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鍙闂殑閮ㄩ棬 ID鍒楄〃
     */
    List<UUID> findCustomDeptPermissions(UUID userId);

    /**
     * 妫€鏌ユ槸鍚﹀瓨鍦ㄨ嚜瀹氫箟鏁版嵁鏉冮檺閰嶇疆
     *
     * @param userId 鐢ㄦ埛 ID
     * @return true 濡傛灉鏈夎嚜瀹氫箟閰嶇疆
     */
    boolean hasCustomDataPermission(UUID userId);
}
