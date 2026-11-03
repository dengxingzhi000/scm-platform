package com.cognora.platform.common.rest.client;

import com.cognora.platform.common.log.dto.AuditLogRecord;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Internal client for platform-audit's record endpoint. The shared RestClient interceptor forwards the
 * caller's JWT, tenant and device headers, so the audit service can authenticate the write.
 */
@HttpExchange("/sys-audit-log")
public interface SysAuditServiceClient {

    @PostExchange
    void create(@RequestBody AuditLogRecord record);
}
