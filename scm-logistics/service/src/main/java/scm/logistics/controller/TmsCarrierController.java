package scm.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.logistics.domain.entity.TmsCarrier;
import scm.logistics.service.ITmsCarrierService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/tms-carrier")
public class TmsCarrierController {

    private final ITmsCarrierService carrierService;

    @GetMapping("/{id}")
    public ApiResponse<TmsCarrier> getById(
            @PathVariable String id) {
        TmsCarrier carrier = carrierService.getById(id);
        return ApiResponse.success(carrier);
    }

    @GetMapping("/code/{carrierCode}")
    public ApiResponse<TmsCarrier> getByCarrierCode(
            @PathVariable String carrierCode) {
        TmsCarrier carrier = carrierService.getByCarrierCode(carrierCode);
        return ApiResponse.success(carrier);
    }

    @GetMapping("/list")
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
    public ApiResponse<List<TmsCarrier>> listEnabled() {
        List<TmsCarrier> list = carrierService.listEnabled();
        return ApiResponse.success(list);
    }

    @PostMapping
    public ApiResponse<TmsCarrier> create(@RequestBody TmsCarrier carrier) {
        log.info("新增物流�? carrierCode={}, carrierName={}", carrier.getCarrierCode(), carrier.getCarrierName());
        carrier.setId(UUID.randomUUID().toString());
        carrier.setDeleted(false);
        carrier.setCreateTime(LocalDateTime.now());
        carrier.setUpdateTime(LocalDateTime.now());
        carrierService.save(carrier);
        return ApiResponse.success(carrier);
    }

    @PutMapping("/{id}")
    public ApiResponse<TmsCarrier> update(@PathVariable String id, @RequestBody TmsCarrier carrier) {
        log.info("修改物流�? id={}", id);
        carrier.setId(id);
        carrier.setUpdateTime(LocalDateTime.now());
        carrierService.updateById(carrier);
        return ApiResponse.success(carrier);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("删除物流�? id={}", id);
        TmsCarrier carrier = new TmsCarrier();
        carrier.setId(id);
        carrier.setDeleted(true);
        carrier.setUpdateTime(LocalDateTime.now());
        carrierService.updateById(carrier);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/enable")
    public ApiResponse<Void> toggleEnabled(@PathVariable String id, @RequestParam boolean enabled) {
        log.info("设置物流商状�? id={}, enabled={}", id, enabled);
        TmsCarrier carrier = new TmsCarrier();
        carrier.setId(id);
        carrier.setEnabled(enabled);
        carrier.setUpdateTime(LocalDateTime.now());
        carrierService.updateById(carrier);
        return ApiResponse.success();
    }
}
