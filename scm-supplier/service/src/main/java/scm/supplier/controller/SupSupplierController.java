package scm.supplier.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.supplier.domain.entity.SupSupplier;
import scm.supplier.service.ISupSupplierService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/suppliers")
public class SupSupplierController {

    private final ISupSupplierService supplierService;

    @GetMapping("/{id}")
    public ApiResponse<SupSupplier> getById(@PathVariable String id) {
        log.info("[API] 查询供应商详情: id={}", id);
        SupSupplier supplier = supplierService.getById(id);
        if (supplier == null || Boolean.TRUE.equals(supplier.getDeleted())) {
            return ApiResponse.fail(404, "供应商不存在");
        }
        return ApiResponse.success(supplier);
    }

    @PostMapping
    public ApiResponse<SupSupplier> create(@RequestBody SupSupplier supplier) {
        log.info("[API] 创建供应商: name={}", supplier.getSupplierName());
        supplier.setId(java.util.UUID.randomUUID().toString());
        supplier.setDeleted(false);
        if (supplier.getEnabled() == null) {
            supplier.setEnabled(true);
        }
        if (supplier.getCooperationStatus() == null) {
            supplier.setCooperationStatus(0);
        }
        supplier.setCreateTime(java.time.LocalDateTime.now());
        supplier.setUpdateTime(java.time.LocalDateTime.now());
        supplierService.save(supplier);
        log.info("[API] 供应商创建成功: id={}", supplier.getId());
        return ApiResponse.success(supplier);
    }

    @PutMapping("/{id}")
    public ApiResponse<SupSupplier> update(@PathVariable String id, @RequestBody SupSupplier supplier) {
        log.info("[API] 更新供应商: id={}", id);
        SupSupplier existing = supplierService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "供应商不存在");
        }
        supplier.setId(id);
        supplier.setUpdateTime(java.time.LocalDateTime.now());
        supplierService.updateById(supplier);
        return ApiResponse.success(supplierService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除供应商: id={}", id);
        SupSupplier existing = supplierService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "供应商不存在");
        }
        existing.setDeleted(true);
        existing.setUpdateTime(java.time.LocalDateTime.now());
        supplierService.updateById(existing);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<Page<SupSupplier>> pageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer supplierType,
            @RequestParam(required = false) Integer cooperationStatus,
            @RequestParam(required = false) Boolean enabled) {
        log.info("[API] 分页查询供应商: page={}, size={}, keyword={}", page, size, keyword);
        Page<SupSupplier> result = supplierService.pageList(page, size, keyword, supplierType,
                cooperationStatus, enabled);
        return ApiResponse.success(result);
    }

    @GetMapping("/active")
    public ApiResponse<List<SupSupplier>> listActive() {
        log.info("[API] 查询所有启用供应商");
        return ApiResponse.success(supplierService.listActive());
    }

    @GetMapping("/search")
    public ApiResponse<List<SupSupplier>> searchByName(
            @RequestParam String name) {
        log.info("[API] 搜索供应商: name={}", name);
        return ApiResponse.success(supplierService.searchByName(name));
    }

    @PutMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable String id) {
        log.info("[API] 启用供应商: id={}", id);
        boolean success = supplierService.enableSupplier(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "启用失败");
    }

    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable String id) {
        log.info("[API] 停用供应商: id={}", id);
        boolean success = supplierService.disableSupplier(id);
        return success ? ApiResponse.success() : ApiResponse.fail(400, "停用失败");
    }
}
