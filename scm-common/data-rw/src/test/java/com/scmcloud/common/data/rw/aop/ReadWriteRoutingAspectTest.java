package com.scmcloud.common.data.rw.aop;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.data.rw.routing.ReadWriteRoutingContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("读写路由切面测试")
class ReadWriteRoutingAspectTest {

    private final ReadWriteRoutingAspect aspect = new ReadWriteRoutingAspect();

    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature signature;

    private final MasterTarget masterTarget = new MasterTarget();
    private final SlaveTarget slaveTarget = new SlaveTarget();

    @BeforeEach
    void setUp() {
        ReadWriteRoutingContext.clear();
    }

    @AfterEach
    void tearDown() {
        ReadWriteRoutingContext.clear();
    }

    private void bind(Object target, String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(target);
    }

    @Test
    @DisplayName("@Master 成功返回后应标记写操作，保证后续读走主库")
    void masterShouldMarkWriteOnSuccess() throws Throwable {
        bind(masterTarget, "write");
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.aroundMaster(joinPoint);

        assertEquals("ok", result);
        assertNotNull(ReadWriteRoutingContext.getLastWriteTime(),
                "@Master 方法必须调用 markWrite()，否则写后读窗口失效");
        assertPoppedBackToAuto();
    }

    @Test
    @DisplayName("@Master 抛异常时不应标记写操作")
    void masterShouldNotMarkWriteOnFailure() throws Throwable {
        bind(masterTarget, "write");
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> aspect.aroundMaster(joinPoint));
        assertNull(ReadWriteRoutingContext.getLastWriteTime());
        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current(),
                "异常后必须弹栈，不能把 MASTER 泄漏给后续调用");
    }

    @Test
    @DisplayName("@Slave 应压入 SLAVE 并在返回后弹栈")
    void slaveShouldPushAndPop() throws Throwable {
        bind(slaveTarget, "read");
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.aroundSlave(joinPoint);

        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current());
        assertNull(ReadWriteRoutingContext.getLastWriteTime(), "只读方法不应标记写");
    }

    private void assertPoppedBackToAuto() {
        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current(),
                "@Master 返回后必须弹栈");
    }

    @SuppressWarnings("unused")
    static class MasterTarget {
        @Master(reason = "test")
        public String write() {
            return "ok";
        }
    }

    @SuppressWarnings("unused")
    static class SlaveTarget {
        @Slave
        public String read() {
            return "ok";
        }
    }
}
