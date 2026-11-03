package com.scmcloud.common.dubbo;

import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

import java.util.UUID;

/**
 * Dubbo Filter that propagates tenant context across RPC boundaries.
 *
 * On provider side: reads tenant_id from RpcContext attachment and sets TenantContextHolder.
 * On consumer side: reads tenant_id from TenantContextHolder and sets RpcContext attachment.
 */
@Slf4j
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = -10000)
public class TenantContextFilter implements Filter {

    private static final String TENANT_ID_KEY = "tenant_id";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // Check URL side parameter to determine provider vs consumer
        String side = invoker.getUrl().getParameter("side", "");
        if ("provider".equals(side)) {
            return handleProvider(invoker, invocation);
        } else {
            return handleConsumer(invoker, invocation);
        }
    }

    private Result handleProvider(Invoker<?> invoker, Invocation invocation) {
        String tenantIdStr = RpcContext.getServiceContext().getAttachment(TENANT_ID_KEY);
        UUID previousTenantId = TenantContextHolder.getTenantId();
        try {
            if (tenantIdStr != null && !tenantIdStr.isBlank()) {
                try {
                    TenantContextHolder.setTenantId(UUID.fromString(tenantIdStr));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid tenant_id in RpcContext: {}", tenantIdStr);
                }
            }
            return invoker.invoke(invocation);
        } finally {
            // Restore previous tenant context
            if (previousTenantId != null) {
                TenantContextHolder.setTenantId(previousTenantId);
            } else {
                TenantContextHolder.clear();
            }
        }
    }

    private Result handleConsumer(Invoker<?> invoker, Invocation invocation) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            RpcContext.getClientAttachment().setAttachment(TENANT_ID_KEY, tenantId.toString());
        }
        return invoker.invoke(invocation);
    }
}
