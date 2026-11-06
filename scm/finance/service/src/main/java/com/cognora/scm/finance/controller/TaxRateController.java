package com.cognora.scm.finance.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.log.annotation.AuditLog;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.scm.finance.domain.dto.CreateTaxRateRequest;
import com.cognora.scm.finance.domain.dto.TaxRateResolution;
import com.cognora.scm.finance.domain.dto.UpdateTaxRateRequest;
import com.cognora.scm.finance.domain.entity.TaxRate;
import com.cognora.scm.finance.service.TaxRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tax-rate")
@Tag(name = "税率规则", description = "租户维度的品类/地区税率规则；结算与发票取税率按区间内最新生效匹配。")
public class TaxRateController {

    private final TaxRateService taxRateService;

    @Operation(summary = "新增税率规则",
            description = "税率 0..1；isDefault=true 时同租户原默认规则自动降级。")
    @PostMapping
    @AuditLog(operation = "新增税率规则", businessType = "FINANCE_TAX_RATE", riskLevel = 3)
    public ApiResponse<TaxRate> create(@Valid @RequestBody CreateTaxRateRequest request) {
        return ApiResponse.success(taxRateService.create(request));
    }

    @Operation(summary = "修改税率规则", description = "局部更新，字段为空表示不修改。")
    @PutMapping("/{id}")
    @AuditLog(operation = "修改税率规则", businessType = "FINANCE_TAX_RATE", riskLevel = 3)
    public ApiResponse<TaxRate> update(
            @Parameter(description = "规则 ID") @PathVariable String id,
            @RequestBody UpdateTaxRateRequest request) {
        return ApiResponse.success(taxRateService.update(id, request));
    }

    @Operation(summary = "停用税率规则", description = "停用后不参与匹配，保留历史记录。")
    @DeleteMapping("/{id}")
    @AuditLog(operation = "停用税率规则", businessType = "FINANCE_TAX_RATE", riskLevel = 3)
    public ApiResponse<TaxRate> disable(@Parameter(description = "规则 ID") @PathVariable String id) {
        return ApiResponse.success(taxRateService.disable(id));
    }

    @Operation(summary = "查询税率规则详情")
    @GetMapping("/{id}")
    public ApiResponse<TaxRate> getById(@Parameter(description = "规则 ID") @PathVariable String id) {
        return ApiResponse.success(taxRateService.getById(id));
    }

    @Operation(summary = "分页查询税率规则")
    @GetMapping("/page")
    public ApiResponse<Page<TaxRate>> page(
            @Parameter(description = "页码，默认 1") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量，默认 20") @RequestParam(required = false) Integer size,
            @Parameter(description = "品类编码") @RequestParam(required = false) String category,
            @Parameter(description = "地区编码") @RequestParam(required = false) String region,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(taxRateService.pageQuery(
                page == null ? 1 : page, size == null ? 20 : size, category, region, enabled));
    }

    @Operation(summary = "解析适用税率",
            description = "按业务日期在生效区间内匹配品类/地区规则，取最新生效；未命中回退租户默认/配置默认。"
                    + "响应 source=RULE 表示命中规则，DEFAULT 表示默认值兜底。")
    @GetMapping("/resolve")
    public ApiResponse<TaxRateResolution> resolve(
            @Parameter(description = "品类编码，可为空") @RequestParam(required = false) String category,
            @Parameter(description = "地区编码，可为空") @RequestParam(required = false) String region,
            @Parameter(description = "业务日期 yyyy-MM-dd，为空按当天")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        return ApiResponse.success(taxRateService.resolve(category, region, businessDate));
    }
}
