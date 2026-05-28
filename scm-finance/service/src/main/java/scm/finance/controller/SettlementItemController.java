package scm.finance.controller;

import com.frog.common.response.ApiResponse;
import com.frog.common.util.UUIDv7Util;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.finance.domain.entity.SettlementItem;
import scm.finance.service.ISettlementItemService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/settlement-item")
@Tag(name = "结算明细", description = "结算明细管理接口")
public class SettlementItemController {

    @Autowired
    private ISettlementItemService settlementItemService;

    @GetMapping("/{id}")
    @Operation(summary = "查询结算明细详情")
    public ApiResponse<SettlementItem> getById(@PathVariable String id) {
        SettlementItem item = settlementItemService.getById(id);
        return ApiResponse.success(item);
    }

    @PostMapping
    @Operation(summary = "创建结算明细")
    public ApiResponse<SettlementItem> create(@RequestBody SettlementItem item) {
        item.setId(UUIDv7Util.generateString());
        item.setCreateTime(LocalDateTime.now());
        settlementItemService.save(item);
        log.info("结算明细创建成功: id={}, settlementId={}", item.getId(), item.getSettlementId());
        return ApiResponse.success(item);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新结算明细")
    public ApiResponse<SettlementItem> update(@PathVariable String id, @RequestBody SettlementItem item) {
        item.setId(id);
        settlementItemService.updateById(item);
        log.info("结算明细更新成功: id={}", id);
        return ApiResponse.success(item);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除结算明细")
    public ApiResponse<Void> delete(@PathVariable String id) {
        settlementItemService.removeById(id);
        log.info("结算明细删除成功: id={}", id);
        return ApiResponse.success();
    }

    @GetMapping("/by-settlement/{settlementId}")
    @Operation(summary = "按结算单查询明细列表")
    public ApiResponse<List<SettlementItem>> listBySettlementId(@PathVariable String settlementId) {
        List<SettlementItem> items = settlementItemService.listBySettlementId(settlementId);
        return ApiResponse.success(items);
    }
}
