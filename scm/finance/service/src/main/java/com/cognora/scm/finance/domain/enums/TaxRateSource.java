package com.cognora.scm.finance.domain.enums;

/**
 * 税率取值来源，用于在响应里标明「规则命中」还是「默认值兜底」。
 */
public enum TaxRateSource {

    /** 命中品类 × 地区 × 生效区间规则（区间内最新生效优先）。 */
    RULE,

    /** 未命中规则，使用租户默认税率或全局配置默认税率。 */
    DEFAULT
}
