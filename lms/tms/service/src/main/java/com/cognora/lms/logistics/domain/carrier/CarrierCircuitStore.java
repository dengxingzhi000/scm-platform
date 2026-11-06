package com.cognora.lms.logistics.domain.carrier;

/**
 * 承运商熔断状态存储（批次 3 收尾）。
 *
 * <p>单实例/单元测试使用 {@link InMemoryCarrierCircuitStore}；多实例部署应使用
 * {@link RedisCarrierCircuitStore}，保证任一实例累计的失败/打开状态对全部实例可见。</p>
 */
public interface CarrierCircuitStore {

    /** 冷却期内返回 true，调用方应跳过该承运商。 */
    boolean isOpen(String carrierId, long nowMillis);

    /**
     * 记录一次失败；连续失败达到阈值后打开熔断至 {@code nowMillis + cooldownMillis}。
     */
    void recordFailure(String carrierId, int failureThreshold, long cooldownMillis, long nowMillis);

    /** 调用成功：清零失败计数并关闭熔断。 */
    void recordSuccess(String carrierId);
}
