package scm.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.logistics.domain.entity.TmsCarrier;
import scm.logistics.service.ITmsCarrierService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tms-carrier")
@Tag(name = "物流商管理", description = "物流商的增删改查及启用/禁用")
public class TmsCarrierController {

    @Autowired
    private ITmsCarrierService carrierService;

    @GetMapping("/{id}")
    @Operation(summary = "查询物流商详情")
    public ApiResponse<TmsCarrier> getById(
            @Parameter(description = "物流商ID", required = true) @PathVariable String id) {
        TmsCarrier carrier = carrierService.getById(id);
        return ApiResponse.success(carrier);
    }

    @GetMapping("/code/{carrierCode}")
    @Operation(summary = "根据编码查询物流商")
    public ApiResponse<TmsCarrier> getByCarrierCode(
            @Parameter(description = "物流商编码", required = true) @PathVariable String carrierCode) {
        TmsCarrier carrier = carrierService.getByCarrierCode(carrierCode);
        return ApiResponse.success(carrier);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询物流商列表")
    public ApiResponse<Page<TmsCarrier>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String carrierName,
            @RequestParam(required = false) Integer carrierType,
            @RequestParam(required = false) Boolean enabled) {
        Page<TmsCarrier> result = carrierService.pageList(page, size, carrierName, carrierType, enabled);
        return ApiResponse.success(result);
    }

    @GetMapping("/enabled")
    @Operation(summary = "查询已启用的物流商列表")
    public ApiResponse<List<TmsCarrier>> listEnabled() {
        List<TmsCarrier> list = carrierService.listEnabled();
        return ApiResponse.success(list);
    }

    @PostMapping
    @Operation(summary = "新增物流商")
    public ApiResponse<TmsCarrier> create(@RequestBody TmsCarrier carrier) {
        log.info("新增物流商: carrierCode={}, carrierName={}", carrier.getCarrierCode(), carrier.getCarrierName());
        carrier.setId(UUID.randomUUID().toString());
        carrier.setDeleted(false);
        carrier.setCreateTime(LocalDateTime.now());
        carrier.setUpdateTime(LocalDateTime.now());
        carrierService.save(carrier);
        return ApiResponse.success(carrier);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改物流商")
    public ApiResponse<TmsCarrier> update(@PathVariable String id, @RequestBody TmsCarrier carrier) {
        log.info("修改物流商: id={}", id);
        carrier.setId(id);
        carrier.setUpdateTime(LocalDateTime.now());
        carrierService.updateById(carrier);
        return ApiResponse.success(carrier);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除物流商（逻辑删除）")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("删除物流商: id={}", id);
        TmsCarrier carrier = new TmsCarrier();
        carrier.setId(id);
        carrier.setDeleted(true);
        carrier.setUpdateTime(LocalDateTime.now());
        carrierService.updateById(carrier);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用/禁用物流商")
    public ApiResponse<Void> toggleEnabled(@PathVariable String id, @RequestParam boolean enabled) {
        log.info("设置物流商状态: id={}, enabled={}", id, enabled);
        TmsCarrier carrier = new TmsCarrier();
        carrier.setId(id);
        carrier.setEnabled(enabled);
        carrier.setUpdateTime(LocalDateTime.now());
        carrierService.updateById(carrier);
        return ApiResponse.success();
    }
}
