package scm.logistics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.logistics.domain.entity.TmsDeliveryArea;
import scm.logistics.service.ITmsDeliveryAreaService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/tms-delivery-area")
public class TmsDeliveryAreaController {

    private final ITmsDeliveryAreaService deliveryAreaService;

    @GetMapping("/{id}")
    public ApiResponse<TmsDeliveryArea> getById(
            @PathVariable String id) {
        TmsDeliveryArea area = deliveryAreaService.getById(id);
        return ApiResponse.success(area);
    }

    @GetMapping("/list")
    public ApiResponse<Page<TmsDeliveryArea>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String carrierId,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city) {
        Page<TmsDeliveryArea> result = deliveryAreaService.pageList(page, size, carrierId, province, city);
        return ApiResponse.success(result);
    }

    @GetMapping("/carrier/{carrierId}")
    public ApiResponse<List<TmsDeliveryArea>> listByCarrier(
            @PathVariable String carrierId) {
        List<TmsDeliveryArea> list = deliveryAreaService.listByCarrier(carrierId);
        return ApiResponse.success(list);
    }

    @GetMapping("/check-coverage")
    public ApiResponse<Boolean> checkCoverage(
            @RequestParam String carrierId,
            @RequestParam String province,
            @RequestParam String city,
            @RequestParam(required = false) String district) {
        boolean covered = deliveryAreaService.checkCoverage(carrierId, province, city, district);
        return ApiResponse.success(covered);
    }

    @PostMapping
    public ApiResponse<TmsDeliveryArea> create(@RequestBody TmsDeliveryArea area) {
        log.info("新增配送区�? areaCode={}, areaName={}", area.getAreaCode(), area.getAreaName());
        area.setId(UUID.randomUUID().toString());
        area.setDeleted(false);
        area.setCreateTime(LocalDateTime.now());
        area.setUpdateTime(LocalDateTime.now());
        deliveryAreaService.save(area);
        return ApiResponse.success(area);
    }

    @PutMapping("/{id}")
    public ApiResponse<TmsDeliveryArea> update(@PathVariable String id, @RequestBody TmsDeliveryArea area) {
        log.info("修改配送区�? id={}", id);
        area.setId(id);
        area.setUpdateTime(LocalDateTime.now());
        deliveryAreaService.updateById(area);
        return ApiResponse.success(area);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("删除配送区�? id={}", id);
        TmsDeliveryArea area = new TmsDeliveryArea();
        area.setId(id);
        area.setDeleted(true);
        area.setUpdateTime(LocalDateTime.now());
        deliveryAreaService.updateById(area);
        return ApiResponse.success();
    }
}
