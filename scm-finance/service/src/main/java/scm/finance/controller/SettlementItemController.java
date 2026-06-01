package scm.finance.controller;

import com.frog.common.response.ApiResponse;
import com.frog.common.util.UUIDv7Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.finance.domain.entity.SettlementItem;
import scm.finance.service.ISettlementItemService;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/settlement-item")
public class SettlementItemController {

    private final ISettlementItemService settlementItemService;

    @GetMapping("/{id}")
    public ApiResponse<SettlementItem> getById(@PathVariable String id) {
        SettlementItem item = settlementItemService.getById(id);
        return ApiResponse.success(item);
    }

    @PostMapping
    public ApiResponse<SettlementItem> create(@RequestBody SettlementItem item) {
        item.setId(UUIDv7Util.generateString());
        item.setCreateTime(LocalDateTime.now());
        settlementItemService.save(item);
        log.info("结算明细创建成功: id={}, settlementId={}", item.getId(), item.getSettlementId());
        return ApiResponse.success(item);
    }

    @PutMapping("/{id}")
    public ApiResponse<SettlementItem> update(@PathVariable String id, @RequestBody SettlementItem item) {
        item.setId(id);
        settlementItemService.updateById(item);
        log.info("结算明细更新成功: id={}", id);
        return ApiResponse.success(item);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        settlementItemService.removeById(id);
        log.info("结算明细删除成功: id={}", id);
        return ApiResponse.success();
    }

    @GetMapping("/by-settlement/{settlementId}")
    public ApiResponse<List<SettlementItem>> listBySettlementId(@PathVariable String settlementId) {
        List<SettlementItem> items = settlementItemService.listBySettlementId(settlementId);
        return ApiResponse.success(items);
    }
}
