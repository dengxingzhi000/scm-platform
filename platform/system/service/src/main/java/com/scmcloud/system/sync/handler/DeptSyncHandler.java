package com.scmcloud.system.sync.handler;

import com.scmcloud.common.integration.sync.event.DataSyncEvent;
import com.scmcloud.common.integration.sync.handler.DataSyncHandler;
import com.scmcloud.system.sync.executor.DeptSyncExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 閮ㄩ棬鏁版嵁鍚屾澶勭悊锟?
 * <p>
 * 澶勭悊閮ㄩ棬鏁版嵁鍙樻洿锛屽悓姝ユ洿鏂板啑浣欏瓧娈靛埌鍏朵粬搴擄細
 * - db_audit.sys_audit_log (dept_name)
 * - db_approval.sys_permission_approval (applicant_dept_name)
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeptSyncHandler implements DataSyncHandler {
    private final DeptSyncExecutor syncExecutor;

    @Override
    public String getAggregateType() {
        return "Dept";
    }

    @Override
    public void handle(DataSyncEvent event) throws DataSyncHandler.DataSyncException {
        UUID deptId = UUID.fromString(event.getPrimaryId());
        Map<String, Object> data = event.getAfterData();

        log.debug("[DeptSync] Handling event: deptId={}, type={}", deptId, event.getEventType());

        try {
            switch (event.getEventType()) {
                case INSERT, UPDATE -> syncDeptName(deptId, data);
                case DELETE -> log.info("[DeptSync] Dept deleted, keeping redundant data for audit: {}", deptId);
                default -> log.warn("[DeptSync] Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            throw new DataSyncHandler.DataSyncException("Failed to sync dept: " + deptId, e, true);
        }
    }

    private void syncDeptName(UUID deptId, Map<String, Object> data) {
        String deptName = (String) data.get("deptName");
        // 閫氳繃鐙珛锟紹ean 璋冪敤锛岀'锟紷Transactional 锟紷DS 鐢熸晥
        syncExecutor.syncToAuditDb(deptId, deptName);
        syncExecutor.syncToApprovalDb(deptId, deptName);
    }
}