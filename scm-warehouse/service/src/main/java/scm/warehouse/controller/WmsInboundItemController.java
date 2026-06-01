package scm.warehouse.controller;

import com.frog.common.response.ApiResponse;
import com.frog.common.util.UUIDv7Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.warehouse.domain.entity.WmsInboundItem;
import scm.warehouse.service.IWmsInboundItemService;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-inbound-item")
public class WmsInboundItemController {

    private final IWmsInboundItemService inboundItemService;

    @PostMapping
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
    public ApiResponse<WmsInboundItem> getById(@PathVariable String id) {
        WmsInboundItem item = inboundItemService.getById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return ApiResponse.fail(404, "入库明细不存在");
        }
        return ApiResponse.success(item);
    }

    @GetMapping("/list")
    public ApiResponse<List<WmsInboundItem>> listByInboundId(
            @RequestParam String inboundId) {
        return ApiResponse.success(inboundItemService.listByInboundId(inboundId));
    }
}
