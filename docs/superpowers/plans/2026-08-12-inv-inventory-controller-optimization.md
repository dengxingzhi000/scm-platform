# InvInventoryController 优化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构 `InvInventoryController`，全部接口改用 `ApiResponse<T>` 包装、热点查询加两级缓存、写接口加 Redis 幂等保护、高频接口加 Sentinel 限流、删除重复 try/catch。

**Architecture:** 仅修改 controller + 新增 RedisConfig cache name 注册；service/DTO/前端 不动。读接口用 `@Cacheable`（自动接 TwoLevelCache），写接口用 `@Idempotent`（Redis SETNX），全部接口用 `@SentinelResource` 声明资源。

**Tech Stack:** Spring Boot, MyBatis-Plus, Caffeine + Redis (TwoLevelCache), Sentinel, AOP @Idempotent

**Spec 引用:** `docs/superpowers/specs/2026-08-12-inv-inventory-controller-optimization-design.md`

---

## 前置条件

- [ ] **Task 0: 验证构建基线**

Run: `mvn clean compile -pl scm-inventory/service -am -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS（基线编译通过）

Run: `mvn test -pl scm-inventory/service -f com.scm.parent/pom.xml`
Expected: Tests run: 1（现有 `InventoryAllocationEngineTest`）

---

## 已知事实（执行时无需再确认）

1. **不存在现有 controller 测试** — `scm-inventory/service/src/test` 下只有 `engine/InventoryAllocationEngineTest.java`
2. **`@Idempotent` 注解包**: `com.scmcloud.common.lock.Idempotent`（FQN 用于 SpEL key）
3. **缓存注解**: `@Cacheable(value = "inventory", key = "...", unless = "#result == null")`
4. **限流注解**: `@SentinelResource(value = "inventory.xxx", blockHandler = "handleBlock")`
5. **调整请求的幂等 key 字段是 `referenceNo`**（不是 spec 草案里的 `adjustNo`，因为 DTO 中无此字段）
6. **调拨请求的幂等 key 字段是 `transferNo`**
7. **初始化请求没有业务单号字段**，使用 `skuId:warehouseId` 作为幂等 key
8. **`@Validated` 已在类级别**，方法参数校验自动生效
9. **`@WebMvcTest` 需要 Spring 上下文**，scm-inventory/service 的测试依赖已在 `pom.xml` 中包含 spring-boot-starter-test
10. **Sentinel blockHandler** 在本类内定义为 `private ApiResponse<T> handleBlock(T data, BlockException e)`，每个方法签名不同，需各自定义

---

## Task 1: 创建 controller 测试基类

**Files:**
- Create: `scm-inventory/service/src/test/java/com/scmcloud/inventory/controller/InvInventoryControllerTest.java`
- Create: `scm-inventory/service/src/test/resources/application-test.yml`

- [ ] **Step 1.1: 创建测试 application 配置**

Create `scm-inventory/service/src/test/resources/application-test.yml`:

```yaml
spring:
  main:
    web-application-type: servlet
  datasource:
    dynamic:
      enabled: false
  redis:
    enabled: false

scm:
  cache:
    two-level:
      enabled: false
  sentinel:
    enabled: false
  tenant:
    enabled: false

logging:
  level:
    root: WARN
    com.scmcloud.inventory: INFO
```

- [ ] **Step 1.2: 创建 controller 测试基类**

Create `scm-inventory/service/src/test/java/com/scmcloud/inventory/controller/InvInventoryControllerTest.java`:

```java
package com.scmcloud.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.inventory.dto.InventoryAdjustRequest;
import com.scmcloud.inventory.dto.InventoryQueryRequest;
import com.scmcloud.inventory.dto.InventoryResponse;
import com.scmcloud.inventory.dto.InventoryStatsResponse;
import com.scmcloud.inventory.dto.InventoryTransferRequest;
import com.scmcloud.inventory.service.IInvInventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller contract tests for InvInventoryController.
 * Asserts HTTP status, ApiResponse wrapping, and JSON shape.
 */
@WebMvcTest(InvInventoryController.class)
@ActiveProfiles("test")
class InvInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IInvInventoryService inventoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private InventoryResponse sampleResponse() {
        InventoryResponse r = new InventoryResponse();
        r.setId("inv-1");
        r.setSkuId("sku-1");
        r.setWarehouseId("wh-1");
        r.setAvailableStock(100);
        return r;
    }

    @Test
    void getInventory_returnsApiResponse() throws Exception {
        when(inventoryService.getInventory("sku-1", "wh-1")).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/inventory")
                        .param("skuId", "sku-1")
                        .param("warehouseId", "wh-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.skuId").value("sku-1"))
                .andExpect(jsonPath("$.data.availableStock").value(100));
    }

    @Test
    void batchGetInventory_returnsApiResponse() throws Exception {
        when(inventoryService.batchGetInventory(any(), any())).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(post("/api/v1/inventory/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"sku-1\"]")
                        .param("warehouseId", "wh-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].skuId").value("sku-1"));
    }

    @Test
    void adjustInventory_returnsApiResponse() throws Exception {
        InventoryAdjustRequest req = new InventoryAdjustRequest();
        req.setSkuId("sku-1");
        req.setWarehouseId("wh-1");
        req.setQuantity(10);
        req.setAdjustType(1);
        req.setReferenceNo("ADJ-001");

        when(inventoryService.adjustInventory(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/inventory/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.skuId").value("sku-1"));
    }

    @Test
    void transferInventory_returnsApiResponse() throws Exception {
        InventoryTransferRequest req = new InventoryTransferRequest();
        req.setSkuId("sku-1");
        req.setFromWarehouseId("wh-1");
        req.setToWarehouseId("wh-2");
        req.setQuantity(5);
        req.setTransferNo("TR-001");

        when(inventoryService.transferInventory(any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/inventory/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void checkStockAvailable_returnsApiResponse() throws Exception {
        when(inventoryService.checkStockAvailable(anyString(), anyString(), anyInt())).thenReturn(true);

        mockMvc.perform(get("/api/v1/inventory/check")
                        .param("skuId", "sku-1")
                        .param("warehouseId", "wh-1")
                        .param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getInventoryStats_returnsApiResponse() throws Exception {
        InventoryStatsResponse stats = new InventoryStatsResponse();
        stats.setTotalSkuCount(100);
        when(inventoryService.getInventoryStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/inventory/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalSkuCount").value(100));
    }

    @Test
    void initInventory_returnsApiResponse() throws Exception {
        when(inventoryService.initInventory(anyString(), anyString(), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/inventory/init")
                        .param("skuId", "sku-1")
                        .param("warehouseId", "wh-1")
                        .param("initialStock", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.skuId").value("sku-1"));
    }
}
```

- [ ] **Step 1.3: 运行测试，验证全部失败（因为 controller 还未重构）**

Run: `mvn test -pl scm-inventory/service -f com.scm.parent/pom.xml -Dtest=InvInventoryControllerTest`
Expected: 7 个测试全部 FAIL（当前 controller 返回 raw entity，jsonPath 找不到 `$.code`/`$.data`）

---

## Task 2: 给 RedisConfig 添加 inventory cache name

**Files:**
- Modify: `scm-common/cache/src/main/java/com/scmcloud/common/redis/config/RedisConfig.java:45-70`

- [ ] **Step 2.1: 修改 CACHE_TTLS Map 添加两个 entry**

在 `RedisConfig.java` 的 `CACHE_TTLS` Map（line 45-70 附近）末尾追加：

```java
            // 库存
            Map.entry("inventory", Duration.ofSeconds(60)),
            Map.entry("inventoryStats", Duration.ofSeconds(60))
```

完整修改后的 Map：

```java
    private static final Map<String, Duration> CACHE_TTLS = Map.ofEntries(
            // 用户信息
            Map.entry("user", Duration.ofMinutes(30)),
            Map.entry("userInfo", Duration.ofMinutes(30)),
            Map.entry("userDetails", Duration.ofMinutes(30)),
            // 权限和角色
            Map.entry("userRoles", Duration.ofHours(1)),
            Map.entry("userPermissions", Duration.ofHours(1)),
            Map.entry("userDataScope", Duration.ofHours(1)),
            Map.entry("userMaxRoleLevel", Duration.ofHours(1)),
            Map.entry("roleLevel", Duration.ofHours(2)),
            Map.entry("permissionTree", Duration.ofHours(2)),
            Map.entry("permissionMapping", Duration.ofMinutes(5)),
            Map.entry("roles", Duration.ofHours(1)),
            Map.entry("role", Duration.ofHours(1)),
            Map.entry("rolePermissions", Duration.ofHours(1)),
            Map.entry("apiPermissions", Duration.ofHours(2)),
            // 部门相关
            Map.entry("userDeptId", Duration.ofMinutes(30)),
            Map.entry("deptPath", Duration.ofHours(2)),
            Map.entry("deptTree", Duration.ofHours(1)),
            Map.entry("deptChildren", Duration.ofHours(1)),
            Map.entry("accessibleDeptIds", Duration.ofHours(1)),
            // 临时角色
            Map.entry("userTemporaryRoles", Duration.ofMinutes(15)),
            // 库存
            Map.entry("inventory", Duration.ofSeconds(60)),
            Map.entry("inventoryStats", Duration.ofSeconds(60))
    );
```

- [ ] **Step 2.2: 验证编译**

Run: `mvn compile -pl scm-common/cache -am -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

- [ ] **Step 2.3: 提交**

```bash
git add scm-common/cache/src/main/java/com/scmcloud/common/redis/config/RedisConfig.java
git commit -m "feat(cache): register inventory and inventoryStats cache names (60s TTL)"
```

---

## Task 3: 重构 getInventory（引入 ApiResponse + 缓存 + 限流样板）

**Files:**
- Modify: `scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InvInventoryController.java:40-62`

- [ ] **Step 3.1: 添加 controller 类顶部 imports**

在文件 imports 区域追加：

```java
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.scmcloud.common.lock.Idempotent;
import com.scmcloud.common.log.util.LogUtils;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.response.ResultCode;
import org.springframework.cache.annotation.Cacheable;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
```

- [ ] **Step 3.2: 重写 getInventory 方法**

替换原 `getInventory` 方法（line 45-62）：

```java
  @GetMapping
  @Cacheable(value = "inventory", key = "#skuId + ':' + #warehouseId", unless = "#result == null || #result.data() == null")
  @SentinelResource(value = "inventory.get", blockHandler = "handleGetBlock")
  public ApiResponse<InventoryResponse> getInventory(
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId) {

    InventoryResponse response = inventoryService.getInventory(skuId, warehouseId);
    return response == null
        ? ApiResponse.fail(ResultCode.NOT_FOUND.getCode(), "库存不存在")
        : ApiResponse.success(response);
  }

  public ApiResponse<InventoryResponse> handleGetBlock(String skuId, String warehouseId, BlockException e) {
    log.warn("Sentinel block: inventory.get skuId={}, warehouseId={}", skuId, warehouseId);
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }
```

- [ ] **Step 3.3: 运行测试，验证 getInventory 通过**

Run: `mvn test -pl scm-inventory/service -f com.scm.parent/pom.xml -Dtest=InvInventoryControllerTest#getInventory_returnsApiResponse`
Expected: PASS

---

## Task 4: 重构 batchGetInventory

**Files:**
- Modify: `scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InvInventoryController.java:64-79`

- [ ] **Step 4.1: 添加验证 imports**

在文件顶部追加：

```java
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
```

- [ ] **Step 4.2: 重写 batchGetInventory 方法**

替换原方法：

```java
  @PostMapping("/batch")
  @Cacheable(value = "inventory", key = "(#warehouseId ?: 'ALL') + ':' + (#skuIds?.toString() ?: '')", unless = "#result == null")
  @SentinelResource(value = "inventory.batchGet", blockHandler = "handleBatchGetBlock")
  public ApiResponse<List<InventoryResponse>> batchGetInventory(
      @RequestBody @NotEmpty(message = "SKU ID 列表不能为空") @Size(max = 100, message = "批量查询 SKU 数量不能超过 100") List<String> skuIds,
      @RequestParam(required = false) String warehouseId) {

    List<InventoryResponse> responses = inventoryService.batchGetInventory(skuIds, warehouseId);
    return ApiResponse.success(responses);
  }

  public ApiResponse<List<InventoryResponse>> handleBatchGetBlock(List<String> skuIds, String warehouseId, BlockException e) {
    log.warn("Sentinel block: inventory.batchGet size={}", skuIds == null ? 0 : skuIds.size());
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }
```

- [ ] **Step 4.3: 运行测试**

Run: `mvn test -pl scm-inventory/service -f com.scm.parent/pom.xml -Dtest=InvInventoryControllerTest#batchGetInventory_returnsApiResponse`
Expected: PASS

---

## Task 5: 重构 queryInventory（分页校验）

**Files:**
- Modify: `scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InvInventoryController.java:81-97`
- Modify: `scm-inventory/service/src/main/java/com/scmcloud/inventory/dto/InventoryQueryRequest.java`（添加 size 上限校验）

- [ ] **Step 5.1: 先查 InventoryQueryRequest 当前结构**

Read: `scm-inventory/service/src/main/java/com/scmcloud/inventory/dto/InventoryQueryRequest.java`

找到 `current` 和 `size` 字段定义位置。如果已有 `@Min/@Max` 注解则跳过本步骤。

- [ ] **Step 5.2: 给 InventoryQueryRequest 添加分页校验**

在 `size` 字段上添加 `@Max`（如果还没有）：

```java
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Min(value = 1, message = "页码不能小于1")
private Integer current;

@Min(value = 1, message = "每页数量不能小于1")
@Max(value = 500, message = "每页数量不能超过500")
private Integer size;
```

如果字段已存在但没有校验注解，按上述添加；如有不同注解名（如 `pageSize`），仅在现有字段上加。

- [ ] **Step 5.3: 重写 queryInventory 方法**

替换原方法：

```java
  @PostMapping("/query")
  @SentinelResource(value = "inventory.query", blockHandler = "handleQueryBlock")
  public ApiResponse<Page<InventoryResponse>> queryInventory(
      @RequestBody @Valid InventoryQueryRequest request) {

    Page<InventoryResponse> page = inventoryService.queryInventory(request);
    return ApiResponse.success(page);
  }

  public ApiResponse<Page<InventoryResponse>> handleQueryBlock(InventoryQueryRequest request, BlockException e) {
    log.warn("Sentinel block: inventory.query");
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }
```

- [ ] **Step 5.4: 不为 queryInventory 写测试**

理由：InventoryQueryRequest 当前不带分页字段，需要先看现有 DTO 结构确认测试所需 mock 参数。本任务专注 controller 重构。Pagination 校验由 DTO 注解保证（`@Min/@Max`），如果有问题会在集成测试中暴露。

- [ ] **Step 5.5: 编译验证**

Run: `mvn compile -pl scm-inventory/service -am -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

---

## Task 6: 重构 checkStockAvailable

**Files:**
- Modify: `scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InvInventoryController.java:161-182`

- [ ] **Step 6.1: 重写 checkStockAvailable 方法**

替换原方法：

```java
  @GetMapping("/check")
  @Cacheable(value = "inventory", key = "#skuId + ':' + #warehouseId + ':' + #quantity", unless = "#result == null")
  @SentinelResource(value = "inventory.check", blockHandler = "handleCheckBlock")
  public ApiResponse<Boolean> checkStockAvailable(
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId,
      @RequestParam @Positive(message = "数量必须大于0") Integer quantity) {

    boolean available = inventoryService.checkStockAvailable(skuId, warehouseId, quantity);
    return ApiResponse.success(available);
  }

  public ApiResponse<Boolean> handleCheckBlock(String skuId, String warehouseId, Integer quantity, BlockException e) {
    log.warn("Sentinel block: inventory.check skuId={}", skuId);
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }
```

- [ ] **Step 6.2: 运行测试**

Run: `mvn test -pl scm-inventory/service -f com.scm.parent/pom.xml -Dtest=InvInventoryControllerTest#checkStockAvailable_returnsApiResponse`
Expected: PASS

---

## Task 7: 重构 getInventoryStats

**Files:**
- Modify: `scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InvInventoryController.java:184-197`

- [ ] **Step 7.1: 重写 getInventoryStats 方法**

替换原方法：

```java
  @GetMapping("/stats")
  @Cacheable(value = "inventoryStats", key = "'global'")
  @SentinelResource(value = "inventory.stats", blockHandler = "handleStatsBlock")
  public ApiResponse<InventoryStatsResponse> getInventoryStats() {
    return ApiResponse.success(inventoryService.getInventoryStats());
  }

  public ApiResponse<InventoryStatsResponse> handleStatsBlock(BlockException e) {
    log.warn("Sentinel block: inventory.stats");
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }
```

- [ ] **Step 7.2: 运行测试**

Run: `mvn test -pl scm-inventory/service -f com.scm.parent/pom.xml -Dtest=InvInventoryControllerTest#getInventoryStats_returnsApiResponse`
Expected: PASS

---

## Task 8: 重构 adjustInventory（写接口 + 幂等）

**Files:**
- Modify: `scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InvInventoryController.java:99-126`

- [ ] **Step 8.1: 重写 adjustInventory 方法**

替换原方法：

```java
  @PostMapping("/adjust")
  @Idempotent(key = "#request.referenceNo", ttl = 24, unit = java.util.concurrent.TimeUnit.HOURS,
              errorMessage = "重复的库存调整请求，请勿重复提交")
  @SentinelResource(value = "inventory.adjust", blockHandler = "handleAdjustBlock")
  public ApiResponse<InventoryResponse> adjustInventory(
      @RequestBody @Valid InventoryAdjustRequest request) {

    LogUtils.business("inventory.adjust", "start", request);
    InventoryResponse response = inventoryService.adjustInventory(request);
    LogUtils.business("inventory.adjust", "success", response);
    return ApiResponse.success(response);
  }

  public ApiResponse<InventoryResponse> handleAdjustBlock(InventoryAdjustRequest request, BlockException e) {
    log.warn("Sentinel block: inventory.adjust skuId={}", request.getSkuId());
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }
```

需要 import：`com.scmcloud.common.log.util.LogUtils`

- [ ] **Step 8.2: 运行测试**

Run: `mvn test -pl scm-inventory/service -f com.scm.parent/pom.xml -Dtest=InvInventoryControllerTest#adjustInventory_returnsApiResponse`
Expected: PASS

> 注意：`@Idempotent` 需要 Redis 连接，@WebMvcTest 默认不加载 Redis。若测试因 Redis 报错，在 `InvInventoryControllerTest.java` 类注解上方加 `@MockBean(IdempotentAspect.class)` 或在 application-test.yml 中 mock RedisTemplate。

---

## Task 9: 重构 transferInventory

**Files:**
- Modify: `scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InvInventoryController.java:128-159`

- [ ] **Step 9.1: 重写 transferInventory 方法**

替换原方法：

```java
  @PostMapping("/transfer")
  @Idempotent(key = "#request.transferNo", ttl = 24, unit = java.util.concurrent.TimeUnit.HOURS,
              errorMessage = "重复的库存调拨请求，请勿重复提交")
  @SentinelResource(value = "inventory.transfer", blockHandler = "handleTransferBlock")
  public ApiResponse<Boolean> transferInventory(
      @RequestBody @Valid InventoryTransferRequest request) {

    LogUtils.business("inventory.transfer", "start", request);
    boolean success = inventoryService.transferInventory(request);
    LogUtils.business("inventory.transfer", success ? "success" : "fail", request);
    return ApiResponse.success(success);
  }

  public ApiResponse<Boolean> handleTransferBlock(InventoryTransferRequest request, BlockException e) {
    log.warn("Sentinel block: inventory.transfer skuId={}", request.getSkuId());
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }
```

- [ ] **Step 9.2: 运行测试**

Run: `mvn test -pl scm-inventory/service -f com.scm.parent/pom.xml -Dtest=InvInventoryControllerTest#transferInventory_returnsApiResponse`
Expected: PASS

---

## Task 10: 重构 initInventory

**Files:**
- Modify: `scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InvInventoryController.java:199-222`

- [ ] **Step 10.1: 重写 initInventory 方法**

替换原方法：

```java
  @PostMapping("/init")
  @Idempotent(key = "#skuId + ':' + #warehouseId", ttl = 24, unit = java.util.concurrent.TimeUnit.HOURS,
              errorMessage = "重复的库存初始化请求，请勿重复提交")
  @SentinelResource(value = "inventory.init", blockHandler = "handleInitBlock")
  public ApiResponse<InventoryResponse> initInventory(
      @RequestParam @NotBlank(message = "SKU ID 不能为空") String skuId,
      @RequestParam @NotBlank(message = "仓库 ID 不能为空") String warehouseId,
      @RequestParam(required = false) Integer initialStock) {

    LogUtils.business("inventory.init", "start", new Object[]{skuId, warehouseId, initialStock});
    InventoryResponse response = inventoryService.initInventory(skuId, warehouseId, initialStock);
    LogUtils.business("inventory.init", "success", response);
    return ApiResponse.success(response);
  }

  public ApiResponse<InventoryResponse> handleInitBlock(String skuId, String warehouseId, Integer initialStock, BlockException e) {
    log.warn("Sentinel block: inventory.init skuId={}", skuId);
    return ApiResponse.fail(ResultCode.TOO_MANY_REQUESTS.getCode(), "请求过于频繁，请稍后再试");
  }
```

- [ ] **Step 10.2: 运行测试**

Run: `mvn test -pl scm-inventory/service -f com.scm.parent/pom.xml -Dtest=InvInventoryControllerTest#initInventory_returnsApiResponse`
Expected: PASS

---

## Task 11: 清理未使用的 imports 和最终验证

**Files:**
- Modify: `scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InvInventoryController.java`

- [ ] **Step 11.1: 移除已不使用的 imports**

下列 imports 在重构后已无引用，从文件 imports 区移除：

- `org.springframework.web.bind.annotation.RequestParam`（如果完全没用到 — 检查确认；GetMapping 仍可能用到）
- 任何不再使用的 import

**操作**：用 IDE 格式化+优化 import，或手动检查 imports 列表确认每个都有使用。

- [ ] **Step 11.2: 运行完整 controller 测试**

Run: `mvn test -pl scm-inventory/service -f com.scm.parent/pom.xml -Dtest=InvInventoryControllerTest`
Expected: 7 tests, all PASS

- [ ] **Step 11.3: 运行模块全部测试**

Run: `mvn test -pl scm-inventory/service -f com.scm.parent/pom.xml`
Expected: All tests PASS

- [ ] **Step 11.4: 全模块验证**

Run: `mvn verify -pl scm-inventory/service -am -f com.scm.parent/pom.xml`
Expected: BUILD SUCCESS

- [ ] **Step 11.5: 提交**

```bash
git add scm-inventory/service/src/main/java/com/scmcloud/inventory/controller/InvInventoryController.java \
        scm-inventory/service/src/main/java/com/scmcloud/inventory/dto/InventoryQueryRequest.java \
        scm-inventory/service/src/test/java/com/scmcloud/inventory/controller/InvInventoryControllerTest.java \
        scm-inventory/service/src/test/resources/application-test.yml
git commit -m "feat(inventory): optimize InvInventoryController

- Wrap all responses in ApiResponse<T>
- Add @Cacheable for hot read paths (inventory 60s, inventoryStats 60s)
- Add @Idempotent for write paths (adjust, transfer, init, 24h TTL)
- Add @SentinelResource rate limiting on all endpoints
- Remove manual try/catch (handled by GlobalExceptionHandler)
- Strengthen validation: @NotEmpty, @Size, @Min/@Max on pagination
- Add controller contract tests"
```

---

## 完成定义（DoD）

- [ ] 全部 7 个 controller 测试通过
- [ ] 现有 `InventoryAllocationEngineTest` 通过
- [ ] `mvn verify -pl scm-inventory/service -am` BUILD SUCCESS
- [ ] 无遗留的 controller 内 try/catch
- [ ] 所有方法返回 `ApiResponse<T>`
- [ ] 所有写接口标注 `@Idempotent`
- [ ] 所有热点查询接口标注 `@Cacheable`
- [ ] 所有接口标注 `@SentinelResource`
- [ ] 没有未使用的 imports

## 已知风险

1. **Redis 测试依赖**：`@Idempotent` 需要 StringRedisTemplate bean。如果 `@WebMvcTest` 不能 mock 它，需改用 `@SpringBootTest` + 完整 application-test.yml，或 mock `IdempotentAspect` 本身。届时按错误信息调整。
2. **`@Cacheable` 在 @WebMvcTest 下不生效**：因为缓存切面依赖 CacheManager。测试只验证 HTTP 响应包装，不验证缓存是否真的写入——这是接受的（缓存是实现细节，行为契约由业务侧保证）。
3. **ApiResponse breaking change**：前端所有 inventory 调用需在下一 PR 同步解包 `.data`。本 PR 不涉及前端。

## 不在范围

- 不修改 service 层
- 不修改 DTO 字段定义（只补校验注解）
- 不动前端
- 不引入 Swagger/OpenAPI
- 不配置 Sentinel 规则阈值（仅声明资源名，规则后续单独 PR 通过 Nacos 下发）