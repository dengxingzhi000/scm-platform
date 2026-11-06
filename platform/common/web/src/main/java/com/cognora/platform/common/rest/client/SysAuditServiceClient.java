package com.cognora.platform.common.rest.client;

import com.cognora.platform.common.log.dto.AuditLogRecord;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Internal client for platform-audit's record endpoint. The shared RestClient interceptor forwards the
 * caller's JWT, tenant and device headers, so the audit service can authenticate the write.
 *
 * <p>{@code nonce} is the same value the signature interceptor puts in {@code X-Nonce}: it is the
 * write-idempotency key on the receiving side, and a retried delivery must reuse it (the
 * interceptor only generates a nonce when the request does not already carry one).
 */
@HttpExchange("/sys-audit-log")
public interface SysAuditServiceClient {

    @PostExchange
    void create(@RequestBody AuditLogRecord record, @RequestHeader("X-Nonce") String nonce);
}
