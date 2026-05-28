package scm.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import scm.finance.domain.entity.FreightRule;
import scm.finance.mapper.FreightRuleMapper;
import scm.finance.service.IFreightRuleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class FreightRuleServiceImpl extends ServiceImpl<FreightRuleMapper, FreightRule>
        implements IFreightRuleService {

    @Override
    public List<FreightRule> listActiveRules() {
        log.debug("查询生效中的运费规则");
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<FreightRule> wrapper = Wrappers.lambdaQuery()
                .eq(FreightRule::getEnabled, true)
                .eq(FreightRule::getDeleted, false)
                .le(FreightRule::getEffectiveDate, today)
                .ge(FreightRule::getExpiryDate, today)
                .orderByDesc(FreightRule::getPriority);
        return list(wrapper);
    }

    @Override
    public BigDecimal calculateFreight(String ruleId, BigDecimal weight, Integer quantity,
                                       BigDecimal volume, BigDecimal orderAmount) {
        log.info("计算运费: ruleId={}, weight={}, quantity={}, volume={}, orderAmount={}",
                ruleId, weight, quantity, volume, orderAmount);

        FreightRule rule = getById(ruleId);
        if (rule == null || Boolean.TRUE.equals(rule.getDeleted())) {
            throw new IllegalArgumentException("运费规则不存在: " + ruleId);
        }

        if (!Boolean.TRUE.equals(rule.getEnabled())) {
            throw new IllegalArgumentException("运费规则已禁用: " + ruleId);
        }

        if (rule.getFreeThreshold() != null && orderAmount != null
                && orderAmount.compareTo(rule.getFreeThreshold()) >= 0) {
            log.info("订单金额满足免运费阈值, 免运费: orderAmount={}, threshold={}",
                    orderAmount, rule.getFreeThreshold());
            return BigDecimal.ZERO;
        }

        BigDecimal freight;
        switch (rule.getBillingType()) {
            case 1 -> freight = calculateByWeight(rule, weight);
            case 2 -> freight = calculateByQuantity(rule, quantity);
            case 3 -> freight = calculateByVolume(rule, volume);
            case 4 -> freight = rule.getFixedFreight() != null ? rule.getFixedFreight() : BigDecimal.ZERO;
            default -> throw new IllegalArgumentException("不支持的计费类型: " + rule.getBillingType());
        }

        if (rule.getRemoteAreaFee() != null) {
            freight = freight.add(rule.getRemoteAreaFee());
        }
        if (rule.getHandlingFee() != null) {
            freight = freight.add(rule.getHandlingFee());
        }

        freight = freight.setScale(2, RoundingMode.HALF_UP);
        log.info("运费计算结果: ruleId={}, freight={}", ruleId, freight);
        return freight;
    }

    private BigDecimal calculateByWeight(FreightRule rule, BigDecimal weight) {
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal firstWeight = rule.getFirstWeight() != null ? rule.getFirstWeight() : BigDecimal.ONE;
        BigDecimal firstPrice = rule.getFirstPrice() != null ? rule.getFirstPrice() : BigDecimal.ZERO;
        BigDecimal additionalWeight = rule.getAdditionalWeight() != null ? rule.getAdditionalWeight() : BigDecimal.ONE;
        BigDecimal additionalPrice = rule.getAdditionalPrice() != null ? rule.getAdditionalPrice() : BigDecimal.ZERO;

        if (weight.compareTo(firstWeight) <= 0) {
            return firstPrice;
        }
        BigDecimal extraWeight = weight.subtract(firstWeight);
        int additionalUnits = extraWeight.divide(additionalWeight, 0, RoundingMode.CEILING).intValue();
        return firstPrice.add(additionalPrice.multiply(BigDecimal.valueOf(additionalUnits)));
    }

    private BigDecimal calculateByQuantity(FreightRule rule, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal firstPrice = rule.getFirstPrice() != null ? rule.getFirstPrice() : BigDecimal.ZERO;
        BigDecimal additionalPrice = rule.getAdditionalPrice() != null ? rule.getAdditionalPrice() : BigDecimal.ZERO;
        int firstWeight = rule.getFirstWeight() != null ? rule.getFirstWeight().intValue() : 1;
        int additionalWeight = rule.getAdditionalWeight() != null ? rule.getAdditionalWeight().intValue() : 1;

        if (quantity <= firstWeight) {
            return firstPrice;
        }
        int extraUnits = (int) Math.ceil((double) (quantity - firstWeight) / additionalWeight);
        return firstPrice.add(additionalPrice.multiply(BigDecimal.valueOf(extraUnits)));
    }

    private BigDecimal calculateByVolume(FreightRule rule, BigDecimal volume) {
        if (volume == null || volume.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal firstWeight = rule.getFirstWeight() != null ? rule.getFirstWeight() : BigDecimal.ONE;
        BigDecimal firstPrice = rule.getFirstPrice() != null ? rule.getFirstPrice() : BigDecimal.ZERO;
        BigDecimal additionalWeight = rule.getAdditionalWeight() != null ? rule.getAdditionalWeight() : BigDecimal.ONE;
        BigDecimal additionalPrice = rule.getAdditionalPrice() != null ? rule.getAdditionalPrice() : BigDecimal.ZERO;

        if (volume.compareTo(firstWeight) <= 0) {
            return firstPrice;
        }
        BigDecimal extraVolume = volume.subtract(firstWeight);
        int additionalUnits = extraVolume.divide(additionalWeight, 0, RoundingMode.CEILING).intValue();
        return firstPrice.add(additionalPrice.multiply(BigDecimal.valueOf(additionalUnits)));
    }
}
