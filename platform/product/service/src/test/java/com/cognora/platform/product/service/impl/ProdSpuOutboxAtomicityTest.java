package com.cognora.platform.product.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cognora.platform.product.domain.entity.ProdSpu;
import com.cognora.platform.product.mapper.ProdSpuMapper;
import com.cognora.platform.product.outbox.ProdOutboxService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SPU 写入与 outbox 的原子性。
 *
 * <p>单元测试无法启动真实 PostgreSQL，因此这里固定三件事：写入方法本身带
 * {@code @Transactional(rollbackFor = Exception.class)}；outbox 的 insert 与业务写入
 * 在同一个方法边界内、按「业务写 → 回读 → outbox」顺序执行；outbox 写失败必须
 * 抛出去，让外层事务把商品行一起回滚。真库回滚见 PR 里的验收命令。</p>
 */
class ProdSpuOutboxAtomicityTest {

    private ProdSpuMapper mapper;
    private ProdOutboxService outboxService;
    private ProdSpuServiceImpl service;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, ProdSpu.class);

        mapper = mock(ProdSpuMapper.class);
        outboxService = mock(ProdOutboxService.class);
        service = new ProdSpuServiceImpl(outboxService);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    private ProdSpu persisted(String id) {
        ProdSpu spu = new ProdSpu();
        spu.setId(id);
        spu.setTenantId("00000000-0000-0000-0000-00000000000a");
        spu.setSpuCode("SPU-1");
        spu.setSpuName("DB name");
        return spu;
    }

    @Test
    void spuWriteMethodsDeclareTransactionsThatRollBackOnException() throws Exception {
        for (String name : new String[]{"createSpu", "updateSpu", "deleteSpu"}) {
            var method = Arrays.stream(ProdSpuServiceImpl.class.getMethods())
                    .filter(m -> m.getName().equals(name))
                    .findFirst()
                    .orElseThrow();
            Transactional tx = method.getAnnotation(Transactional.class);
            assertTrue(tx != null, name + " 必须声明 @Transactional");
            assertTrue(Arrays.asList(tx.rollbackFor()).contains(Exception.class),
                    name + " 必须对 Exception 回滚");
        }
    }

    @Test
    void createWritesOutboxAfterTheInsertInTheSameMethod() {
        when(mapper.selectById(anyString())).thenAnswer(invocation ->
                persisted(invocation.getArgument(0, String.class)));

        ProdSpu spu = new ProdSpu();
        service.createSpu(spu);

        InOrder order = inOrder(mapper, outboxService);
        order.verify(mapper).insert(spu);
        order.verify(mapper).selectById(spu.getId());
        order.verify(outboxService).recordSpuUpsert(any(ProdSpu.class), anyBoolean());
    }

    @Test
    void outboxFailureOnCreatePropagatesAndAbortsTheBusinessWrite() {
        when(mapper.selectById(anyString())).thenAnswer(invocation ->
                persisted(invocation.getArgument(0, String.class)));
        doThrow(new RuntimeException("outbox insert failed"))
                .when(outboxService).recordSpuUpsert(any(ProdSpu.class), anyBoolean());

        assertThrows(RuntimeException.class, () -> service.createSpu(new ProdSpu()));
        // 异常穿透 createSpu 的 @Transactional 边界 → 商品 insert 与 outbox 一起回滚
    }

    @Test
    void updateReloadsTheWholeRowBeforeEmittingTheEvent() {
        when(mapper.updateById(any(ProdSpu.class))).thenReturn(1);
        ProdSpu reloaded = persisted("spu-1");
        when(mapper.selectById("spu-1")).thenReturn(reloaded);

        ProdSpu patch = new ProdSpu();
        patch.setSpuName("request name");
        service.updateSpu("spu-1", patch);

        ArgumentCaptor<ProdSpu> captor = ArgumentCaptor.forClass(ProdSpu.class);
        verify(outboxService).recordSpuUpsert(captor.capture(), anyBoolean());
        // 事件必须用回读的整行，部分更新不能把未提交字段写成 null
        assertSame(reloaded, captor.getValue());
        assertEquals("DB name", captor.getValue().getSpuName());
    }

    @Test
    void updateWithoutAffectedRowEmitsNoEvent() {
        when(mapper.updateById(any(ProdSpu.class))).thenReturn(0);

        service.updateSpu("missing", new ProdSpu());

        verify(outboxService, never()).recordSpuUpsert(any(ProdSpu.class), anyBoolean());
    }

    @Test
    void deleteReadsTheRowBeforeTheLogicalDeleteAndEmitsDeleteEvent() {
        ProdSpu existing = persisted("spu-1");
        when(mapper.selectById("spu-1")).thenReturn(existing);
        when(mapper.update(isNull(), any())).thenReturn(1);

        service.deleteSpu("spu-1");

        InOrder order = inOrder(mapper, outboxService);
        order.verify(mapper).selectById("spu-1");
        order.verify(mapper).update(isNull(), any());
        order.verify(outboxService).recordSpuDelete(existing);
    }

    @Test
    void deleteOfMissingSpuEmitsNoEvent() {
        when(mapper.selectById("spu-404")).thenReturn(null);
        when(mapper.update(isNull(), any())).thenReturn(0);

        service.deleteSpu("spu-404");

        verify(outboxService, never()).recordSpuDelete(any(ProdSpu.class));
    }
}
