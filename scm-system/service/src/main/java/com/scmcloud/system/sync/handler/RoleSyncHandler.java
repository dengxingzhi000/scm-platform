package com.scmcloud.system.sync.handler;

import com.scmcloud.common.integration.sync.event.DataSyncEvent;
import com.scmcloud.common.integration.sync.handler.DataSyncHandler;
import com.scmcloud.system.sync.executor.RoleSyncExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 瑙掕壊鏁版嵁鍚屾澶勭悊锟?
 * <p>
 * 澶勭悊瑙掕壊鏁版嵁鍙樻洿锛屽悓姝ユ洿鏂板啑浣欏瓧娈靛埌鍏朵粬搴擄細
 * - db_approval.sys_permission_approval (role_names)
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleSyncHandler implements DataSyncHandler {
    private final RoleSyncExecutor syncExecutor;

    @Override
    public String getAggregateType() {
        return "Role";
    }

    @Override
    public void handle(DataSyncEvent event) throws DataSyncHandler.DataSyncException {
        UUID roleId = UUID.fromString(event.getPrimaryId());
        Map<String, Object> data = event.getAfterData();

        log.debug("[RoleSync] Handling event: roleId={}, type={}", roleId, event.getEventType());

        try {
            switch (event.getEventType()) {
                case INSERT, UPDATE -> syncRoleInfo(roleId, data);
                case DELETE -> syncExecutor.markRoleDeletedInApprovalDb(roleId);
                default -> log.warn("[RoleSync] Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            throw new DataSyncHandler.DataSyncException("Failed to sync role: " + roleId, e, true);
        }
    }

    private void syncRoleInfo(UUID roleId, Map<String, Object> data) {
        String roleName = (String) data.get("roleName");
        String roleCode = (String) data.get("roleCode");
        syncExecutor.syncToApprovalDb(roleId, roleName, roleCode);
    }
}