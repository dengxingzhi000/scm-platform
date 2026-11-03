package com.scmcloud.common.data.rw.tracing;

import com.scmcloud.common.data.rw.routing.ReadWriteRoutingContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("读写路由链路追踪测试")
class ReadWriteTracingInterceptorTest {

    private final Span span = mock(Span.class);
    private final SpanBuilder builder = mock(SpanBuilder.class, RETURNS_SELF);
    private final Tracer tracer = mock(Tracer.class);

    @AfterEach
    void tearDown() {
        ReadWriteRoutingContext.clear();
    }

    private void prepareTracer() {
        when(tracer.spanBuilder(anyString())).thenReturn(builder);
        when(builder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(mock(Scope.class));
    }

    @Test
    @DisplayName("span 里应记录实际路由目标与是否降级")
    void shouldRecordActualTargetAndFallback() throws Throwable {
        prepareTracer();

        MethodInvocation invocation = mock(MethodInvocation.class);
        Method method = TracingTarget.class.getMethod("read");
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.proceed()).thenReturn("ok");

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, "slave1", true);
        ReadWriteRoutingContext.recordRoute("master", "slave1", true);

        Object result = new ReadWriteTracingInterceptor(tracer).invoke(invocation);

        assertEquals("ok", result);
        verify(builder).setAttribute("db.rw.routing_type", "SLAVE");
        verify(span).setAttribute("db.rw.specified_slave", "slave1");
        verify(span).setAttribute("db.rw.target", "master");
        verify(span).setAttribute("db.rw.fallback", true);
        verify(span).setAttribute("db.rw.slave", "slave1");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    @DisplayName("异常时应记录错误状态并把异常抛给调用方")
    void shouldRecordErrorStatus() throws Throwable {
        prepareTracer();

        MethodInvocation invocation = mock(MethodInvocation.class);
        Method method = TracingTarget.class.getMethod("read");
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.proceed()).thenThrow(new IllegalStateException("boom"));

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);

        assertThrows(IllegalStateException.class,
                () -> new ReadWriteTracingInterceptor(tracer).invoke(invocation));

        verify(span).setStatus(StatusCode.ERROR, "boom");
        verify(span).recordException(any(Throwable.class));
        verify(span).end();
    }

    @SuppressWarnings("unused")
    static class TracingTarget {
        public String read() {
            return "ok";
        }
    }
}
