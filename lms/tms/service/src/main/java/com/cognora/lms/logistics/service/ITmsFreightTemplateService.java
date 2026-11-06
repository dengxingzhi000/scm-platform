package com.cognora.lms.logistics.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.lms.logistics.domain.entity.TmsFreightTemplate;
import com.cognora.lms.logistics.domain.freight.FreightQuote;

import java.math.BigDecimal;
import java.util.List;

/**
 * 运费模板与运费试算（批次 2）。
 */
public interface ITmsFreightTemplateService extends IService<TmsFreightTemplate> {

    Page<TmsFreightTemplate> pageList(int page, int size, String carrierId, String regionCode);

    List<TmsFreightTemplate> listByCarrier(String carrierId);

    TmsFreightTemplate createTemplate(TmsFreightTemplate template);

    TmsFreightTemplate updateTemplate(String id, TmsFreightTemplate template);

    void deleteTemplate(String id);

    /**
     * 按承运商 + 区域解析模板：精确匹配 → 承运商兜底 → 区域兜底 → 全局兜底。
     *
     * @return 命中的模板；未配置返回 null
     */
    TmsFreightTemplate resolveTemplate(String carrierId, String regionCode);

    /**
     * 运费试算。缺少模板或参数非法时抛出 {@link IllegalArgumentException}。
     */
    FreightQuote quote(String carrierId, String regionCode, BigDecimal weight, BigDecimal volume);
}
