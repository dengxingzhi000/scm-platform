package com.scmcloud.common.dubbo;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

@Slf4j
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = -9000)
public class HopCountFilter implements Filter {

    private static final String HOP_COUNT_KEY = "rpc-hop-count";
    private static final int MAX_HOPS = 3;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String side = invoker.getUrl().getParameter("side", "");
        if ("provider".equals(side)) {
            return handleProvider(invoker, invocation);
        } else {
            return handleConsumer(invoker, invocation);
        }
    }

    private Result handleConsumer(Invoker<?> invoker, Invocation invocation) {
        String hopCountStr = RpcContext.getClientAttachment().getAttachment(HOP_COUNT_KEY);
        int hopCount = hopCountStr != null ? Integer.parseInt(hopCountStr) : 0;
        hopCount++;
        RpcContext.getClientAttachment().setAttachment(HOP_COUNT_KEY, String.valueOf(hopCount));
        return invoker.invoke(invocation);
    }

    private Result handleProvider(Invoker<?> invoker, Invocation invocation) {
        String hopCountStr = RpcContext.getServiceContext().getAttachment(HOP_COUNT_KEY);
        if (hopCountStr != null) {
            int hopCount = Integer.parseInt(hopCountStr);
            if (hopCount > MAX_HOPS) {
                log.warn("RPC hop count {} exceeds maximum of {}, rejecting request", hopCount, MAX_HOPS);
                throw new RpcException(RpcException.LIMIT_EXCEEDED_EXCEPTION,
                        "RPC hop count " + hopCount + " exceeds maximum of " + MAX_HOPS);
            }
        }
        return invoker.invoke(invocation);
    }
}
