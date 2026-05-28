package scm.finance.service;

import scm.finance.domain.entity.FreightRule;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface IFreightRuleService extends IService<FreightRule> {

    List<FreightRule> listActiveRules();

    BigDecimal calculateFreight(String ruleId, BigDecimal weight, Integer quantity,
                                BigDecimal volume, BigDecimal orderAmount);
}
