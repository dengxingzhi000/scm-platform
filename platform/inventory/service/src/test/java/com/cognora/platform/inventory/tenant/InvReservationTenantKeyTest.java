package com.cognora.platform.inventory.tenant;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.tenant.TenantParseException;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.dto.InventoryReservationRequest;
import com.cognora.platform.inventory.engine.InventoryAllocationEngine;
import com.cognora.platform.inventory.lock.DistributedLock;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.service.command.InvReservationCommandService;
import com.cognora.platform.inventory.service.query.InvReservationQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库存预留键的租户隔离：A 租户写入的预留，B 租户读不到。
 */
@ExtendWith(MockitoExtension.class)
class InvReservationTenantKeyTest {

    private static final String SKU = "SKU-001";
    private static final String WAREHOUSE = "WH-001";
    private static final String BUSINESS_KEY = "ORDER-001";

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private SetOperations<String, Object> setOperations;
    @Mock private InvInventoryMapper inventoryMapper;
    @Mock private DistributedLock distributedLock;
    @Mock private InventoryAllocationEngine allocationEngine;

    private InvReservationCommandService commandService;
    private InvReservationQueryService queryService;

    /** 模拟 Redis：只保存实际写入过的键。 */
    private final Set<String> storedKeys = new HashSet<>();

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.hasKey(anyString()))
                .thenAnswer(invocation -> storedKeys.contains(invocation.getArgument(0)));
        lenient().when(distributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(mock(DistributedLock.LockHandle.class));

        commandService = new InvReservationCommandService(
                redisTemplate, inventoryMapper, distributedLock, allocationEngine);
        queryService = new InvReservationQueryService(redisTemplate);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void reservationWrittenByTenantAIsInvisibleToTenantB() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        TenantContextHolder.setTenantId(tenantA);
        reserveAsTenantA();

        TenantContextHolder.setTenantId(tenantB);
        assertThat(queryService.checkReservationExists(BUSINESS_KEY)).isFalse();

        TenantContextHolder.setTenantId(tenantA);
        assertThat(queryService.checkReservationExists(BUSINESS_KEY)).isTrue();
    }

    @Test
    void reservationKeyCarriesTenantSegmentAndKeepsPrefix() {
        UUID tenantA = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantA);

        reserveAsTenantA();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(hashOperations).putAll(keyCaptor.capture(), any(Map.class));
        String reservationKey = keyCaptor.getValue();
        assertThat(reservationKey).startsWith("inventory:reservation:");
        assertThat(reservationKey).contains(tenantA.toString());

        ArgumentCaptor<String> indexKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> indexElement = ArgumentCaptor.forClass(Object.class);
        verify(setOperations).add(indexKeyCaptor.capture(), indexElement.capture());
        assertThat(indexKeyCaptor.getValue()).startsWith("inventory:reservation:index:");
        assertThat(indexKeyCaptor.getValue()).contains(tenantA.toString());
        assertThat(indexElement.getValue()).isEqualTo(BUSINESS_KEY);
    }

    @Test
    void reservationKeysFailClosedWithoutTenantContext() {
        TenantContextHolder.clear();
        assertThatThrownBy(() -> queryService.checkReservationExists(BUSINESS_KEY))
                .isInstanceOf(TenantParseException.class);
    }

    private void reserveAsTenantA() {
        Inventory inventory = new Inventory();
        inventory.setId("INV-001");
        inventory.setSkuId(SKU);
        inventory.setWarehouseId(WAREHOUSE);
        inventory.setAvailableStock(10);
        inventory.setLockedStock(0);
        inventory.setDeleted(false);
        when(inventoryMapper.selectOne(any())).thenReturn(inventory);
        when(inventoryMapper.update(any(), any())).thenReturn(1);

        InventoryReservationRequest request = new InventoryReservationRequest();
        request.setBusinessKey(BUSINESS_KEY);
        request.setSkuId(SKU);
        request.setWarehouseId(WAREHOUSE);
        request.setQuantity(1);

        assertThat(commandService.reserveInventory(request)).isTrue();

        // 记录 Redis 实际写入的键（reservation + index），供 hasKey() 模拟读取。
        ArgumentCaptor<String> expireKey = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate, times(2)).expire(expireKey.capture(), any(Duration.class));
        storedKeys.addAll(expireKey.getAllValues());
    }
}
