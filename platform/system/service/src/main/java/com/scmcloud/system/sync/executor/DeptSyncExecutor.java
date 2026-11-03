package com.scmcloud.system.sync.executor;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 閮ㄩ棬鍚屾鎵ц锟?
 * <p>
 * 鐙珛锟紹ean锛岀敤浜庢墽琛岃法搴撲簨鍔℃搷浣滐拷
 * 閬垮厤 @Transactional 鑷皟鐢ㄩ棶棰橈紙Spring AOP 浠g悊涓嶆嫤鎴悓绫绘柟娉曡皟鐢級
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Component
public class DeptSyncExecutor {

    /**
     * 鍚屾閮ㄩ棬淇℃伅锟絘udit 锟?
     *
     * @param deptId   閮ㄩ棬 ID
     * @param deptName 閮ㄩ棬鍚嶇О
     */
    @DS("audit")
    @Transactional(rollbackFor = Exception.class)
    public void syncToAuditDb(UUID deptId, String deptName) {
        // 鏇存柊瀹¤鏃ュ織涓殑閮ㄩ棬鍚嶇О
        // 娉ㄦ剰锛氬璁℃棩蹇楅€氬父涓嶆洿鏂板巻鍙茶褰曪紝杩欓噷鍙槸绀轰緥
        log.debug("[DeptSync] Would update audit logs for dept: {}, name: {}", deptId, deptName);
    }

    /**
     * 鍚屾閮ㄩ棬淇℃伅锟絘pproval 锟?
     *
     * @param deptId   閮ㄩ棬 ID
     * @param deptName 閮ㄩ棬鍚嶇О
     */
    @DS("approval")
    @Transactional(rollbackFor = Exception.class)
    public void syncToApprovalDb(UUID deptId, String deptName) {
        // 鏇存柊瀹℃壒璁板綍涓殑閮ㄩ棬鍚嶇О
        log.debug("[DeptSync] Would update approval records for dept: {}, name: {}", deptId, deptName);
    }
}