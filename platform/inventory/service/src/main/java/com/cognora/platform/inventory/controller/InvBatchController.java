package com.cognora.platform.inventory.controller;

import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.dto.FefoPreviewRequest;
import com.cognora.platform.inventory.dto.FefoPreviewResponse;
import com.cognora.platform.inventory.service.batch.FefoAllocationPlan;
import com.cognora.platform.inventory.service.batch.FefoAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 库存批次接口。
 *
 * <p>当前只暴露只读的 FEFO 预览，供前端在下单前查看会扣减哪些批次；
 * 实际扣减仍在库存调整/调拨的出库路径内完成。
 */
@RestController
@RequestMapping("/api/v1/inventory/batches")
@RequiredArgsConstructor
public class InvBatchController {

  private final FefoAllocationService fefoAllocationService;

  @PostMapping("/fefo-preview")
  public ApiResponse<FefoPreviewResponse> preview(@Valid @RequestBody FefoPreviewRequest request) {
    UUID tenantId = TenantContextHolder.getRequiredTenantId();
    FefoAllocationPlan plan = fefoAllocationService.preview(
        tenantId.toString(), request.getSkuId(), request.getWarehouseId(), request.getQuantity());
    return ApiResponse.success(FefoPreviewResponse.from(request, plan));
  }
}
