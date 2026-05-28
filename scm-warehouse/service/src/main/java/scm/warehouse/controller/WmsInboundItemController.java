package scm.warehouse.controller;

import com.frog.common.response.ApiResponse;
import com.frog.common.util.UUIDv7Util;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.warehouse.domain.entity.WmsInboundItem;
import scm.warehouse.service.IWmsInboundItemService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/wms-inbound-item")
@Tag(name = "入库明细管理", description = "入库单明细增删改查接口")
public class WmsInboundItemController {

    @Autowired
    private IWmsInboundItemService inboundItemService;

    @PostMapping
    @Operation(summary = "创建入库明细")
    public ApiResponse<WmsInboundItem> create(@RequestBody WmsInboundItem item) {
        log.info("[API] 创建入库明细: inboundId={}, skuId={}", item.getInboundId(), item.getSkuId());

        item.setId(UUIDv7Util.generateString());
        item.setActualQuantity(0);
        item.setQualityStatus(1); // 1-合格
        item.setCreateTime(LocalDateTime.now());

        inboundItemService.save(item);
        log.info("[API] 入库明细创建成功: id={}", item.getId());
        return ApiResponse.success(item);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新入库明细")
    public ApiResponse<WmsInboundItem> update(@PathVariable String id, @RequestBody WmsInboundItem item) {
        log.info("[API] 更新入库明细: id={}", id);

        WmsInboundItem existing = inboundItemService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "入库明细不存在");
        }

        item.setId(id);
        inboundItemService.updateById(item);
        return ApiResponse.success(inboundItemService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除入库明细")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除入库明细: id={}", id);

        WmsInboundItem existing = inboundItemService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "入库明细不存在");
        }

        existing.setDeleted(true);
        inboundItemService.updateById(existing);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询入库明细详情")
    public ApiResponse<WmsInboundItem> getById(@PathVariable String id) {
        WmsInboundItem item = inboundItemService.getById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return ApiResponse.fail(404, "入库明细不存在");
        }
        return ApiResponse.success(item);
    }

    @GetMapping("/list")
    @Operation(summary = "查询入库单的明细列表")
    public ApiResponse<List<WmsInboundItem>> listByInboundId(
            @Parameter(description = "入库单ID", required = true)
            @RequestParam String inboundId) {
        return ApiResponse.success(inboundItemService.listByInboundId(inboundId));
    }
}
