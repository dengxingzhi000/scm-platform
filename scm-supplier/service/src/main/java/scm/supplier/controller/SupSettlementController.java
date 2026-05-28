package scm.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.supplier.domain.entity.SupSettlement;
import scm.supplier.service.ISupSettlementService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/settlements")
@Tag(name = "对账单管理", description = "对账单CRUD及工作流接口")
public class SupSettlementController {

    @Autowired
    private ISupSettlementService settlementService;

    @GetMapping("/{id}")
    @Operation(summary = "查询对账单详情")
    public ApiResponse<SupSettlement> getById(@PathVariable String id) {
        log.info("[API] 查询对账单详情: id={}", id);
        SupSettlement settlement = settlementService.getById(id);
        if (settlement == null || Boolean.TRUE.equals(settlement.getDeleted())) {
            return ApiResponse.fail(404, "对账单不存在");
        }
        return ApiResponse.success(settlement);
    }

    @PostMapping
    @Operation(summary = "创建对账单")
    public ApiResponse<SupSettlement> create(@RequestBody SupSettlement settlement) {
        log.info("[API] 创建对账单: supplierId={}", settlement.getSupplierId());
        settlement.setId(UUID.randomUUID());
        settlement.setStatus(0);
        settlement.setDeleted(false);
        settlement.setCreateTime(LocalDateTime.now());
        settlement.setUpdateTime(LocalDateTime.now());
        settlementService.save(settlement);
        log.info("[API] 对账单创建成功: id={}", settlement.getId());
        return ApiResponse.success(settlement);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新对账单")
    public ApiResponse<SupSettlement> update(@PathVariable String id, @RequestBody SupSettlement settlement) {
        log.info("[API] 更新对账单: id={}", id);
        SupSettlement existing = settlementService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "对账单不存在");
        }
        if (existing.getStatus() != 0) {
            return ApiResponse.fail(400, "只有待确认状态的对账单才能修改");
        }
        settlement.setId(UUID.fromString(id));
        settlement.setUpdateTime(LocalDateTime.now());
        settlementService.updateById(settlement);
        return ApiResponse.success(settlementService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除对账单（逻辑删除）")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除对账单: id={}", id);
        SupSettlement existing = settlementService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "对账单不存在");
        }
        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        settlementService.updateById(existing);
        return ApiResponse.success();
    }

    @GetMapping
    @Operation(summary = "分页查询对账单列表")
    public ApiResponse<Page<SupSettlement>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String settlementPeriod) {
        log.info("[API] 分页查询对账单: page={}, size={}, supplierId={}, status={}", page, size, supplierId, status);
        Page<SupSettlement> result = settlementService.pageList(page, size, supplierId, status, settlementPeriod);
        return ApiResponse.success(result);
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "查询供应商的所有对账单")
    public ApiResponse<List<SupSettlement>> listBySupplierId(@PathVariable String supplierId) {
        log.info("[API] 查询供应商对账单列表: supplierId={}", supplierId);
        return ApiResponse.success(settlementService.listBySupplierId(supplierId));
    }

    @PutMapping("/{id}/confirm")
    @Operation(summary = "确认对账单")
    public ApiResponse<Void> confirm(@PathVariable String id,
                                     @RequestParam String approverId,
                                     @RequestParam String approverName) {
        log.info("[API] 确认对账单: id={}, approverId={}", id, approverId);
        boolean success = settlementService.confirm(id, approverId, approverName);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "确认失败，对账单状态不正确");
    }

    @PutMapping("/{id}/pay")
    @Operation(summary = "标记对账单已付款")
    public ApiResponse<Void> markAsPaid(@PathVariable String id, @RequestParam String updateBy) {
        log.info("[API] 标记对账单已付款: id={}", id);
        boolean success = settlementService.markAsPaid(id, updateBy);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "标记付款失败，对账单状态不正确");
    }
}
