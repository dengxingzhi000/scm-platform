package com.cognora.scm.finance.service;

import com.cognora.scm.finance.domain.entity.FreightRule;
import com.baomidou.mybatisplus.spring.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface IFreightRuleService extends IService<FreightRule> {

    List<FreightRule> listActiveRules();

    BigDecimal calculateFreight(String ruleId, BigDecimal weight, Integer quantity,
                                BigDecimal volume, BigDecimal orderAmount);

    FreightRule createRule(FreightRule rule);

    FreightRule updateRule(String id, FreightRule rule);

    void deleteRule(String id);
}