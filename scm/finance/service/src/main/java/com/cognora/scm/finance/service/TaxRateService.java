package com.cognora.scm.finance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.scm.finance.domain.dto.CreateTaxRateRequest;
import com.cognora.scm.finance.domain.dto.TaxRateResolution;
import com.cognora.scm.finance.domain.dto.UpdateTaxRateRequest;
import com.cognora.scm.finance.domain.entity.TaxRate;

import java.time.LocalDate;

public interface TaxRateService {

    /**
     * 新增税率规则；{@code isDefault=true} 时会先取消同租户原来的默认规则。
     */
    TaxRate create(CreateTaxRateRequest request);

    /**
     * 局部更新；字段为空表示不修改。
     */
    TaxRate update(String id, UpdateTaxRateRequest request);

    /**
     * 停用规则（不物理删除，保留历史供追溯）。
     */
    TaxRate disable(String id);

    TaxRate getById(String id);

    Page<TaxRate> pageQuery(int page, int size, String category, String region, Boolean enabled);

    /**
     * 按「区间内最新生效」解析税率，未命中回退租户默认 / 配置默认。
     */
    TaxRateResolution resolve(String category, String region, LocalDate businessDate);
}
