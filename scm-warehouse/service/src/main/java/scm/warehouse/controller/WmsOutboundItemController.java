package scm.warehouse.controller;

import com.frog.common.response.ApiResponse;
import com.frog.common.util.UUIDv7Util;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.warehouse.domain.entity.WmsOutboundItem;
import scm.warehouse.service.IWmsOutboundItemService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/wms-outbound-item")
@Tag(name = "出库明细管理", description = "出库单明细增删改查接口")
public class WmsOutboundItemController {

    @Autowired
    private IWmsOutboundItemService outboundItemService;

    @PostMapping
    @Operation(summary = "创建出库明细")
    public ApiResponse<WmsOutboundItem> create(@RequestBody WmsOutboundItem item) {
        log.info("[API] 创建出库明细: outboundId={}, skuId={}", item.getOutboundId(), item.getSkuId());

        item.setId(UUIDv7Util.generateString());
        item.setActualQuantity(0);
        item.setCreateTime(LocalDateTime.now());

        outboundItemService.save(item);
        log.info("[API] 出库明细创建成功: id={}", item.getId());
        return ApiResponse.success(item);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新出库明细")
    public ApiResponse<WmsOutboundItem> update(@PathVariable String id, @RequestBody WmsOutboundItem item) {
        log.info("[API] 更新出库明细: id={}", id);

        WmsOutboundItem existing = outboundItemService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "出库明细不存在");
        }

        item.setId(id);
        outboundItemService.updateById(item);
        return ApiResponse.success(outboundItemService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除出库明细")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除出库明细: id={}", id);

        WmsOutboundItem existing = outboundItemService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "出库明细不存在");
        }

        existing.setDeleted(true);
        outboundItemService.updateById(existing);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询出库明细详情")
    public ApiResponse<WmsOutboundItem> getById(@PathVariable String id) {
        WmsOutboundItem item = outboundItemService.getById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return ApiResponse.fail(404, "出库明细不存在");
        }
        return ApiResponse.success(item);
    }

    @GetMapping("/list")
    @Operation(summary = "查询出库单的明细列表")
    public ApiResponse<List<WmsOutboundItem>> listByOutboundId(
            @Parameter(description = "出库单ID", required = true)
            @RequestParam String outboundId) {
        return ApiResponse.success(outboundItemService.listByOutboundId(outboundId));
    }
}
