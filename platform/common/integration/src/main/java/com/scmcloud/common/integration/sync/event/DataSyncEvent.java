package com.scmcloud.common.integration.sync.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 鏁版嵁鍚屾浜嬩欢锛堝叏灞€閫氱敤锟?
 * <p>
 * 璁捐鍙傝€冿細
 * - 闃块噷 Canal binlog 浜嬩欢鏍煎紡
 * - CloudEvents 瑙勮寖
 * - 瀛楄妭璺冲姩鏁版嵁鍚屾涓棿锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSyncEvent {

    /**
     * 浜嬩欢鍞竴ID锛堢敤浜庡箓绛夊鐞嗭級
     */
    private String eventId;

    /**
     * 浜嬩欢绫诲瀷
     */
    private DataSyncEventType eventType;

    /**
     * 浜嬩欢鍙戠敓鏃堕棿
     */
    private Instant eventTime;

    /**
     * 浜嬩欢鐗堟湰锛堜箰瑙傞攣锟?
     */
    private Long version;

    /**
     * 婧愭湇鍔″悕锟?
     */
    private String sourceService;

    /**
     * 婧愭暟鎹簱
     */
    private String sourceDatabase;

    /**
     * 婧愯〃锟?
     */
    private String sourceTable;

    /**
     * 鑱氬悎绫诲瀷锛圲ser, Dept, Role 绛夛級
     */
    private String aggregateType;

    /**
     * 涓婚敭 ID
     */
    private String primaryId;

    /**
     * 鍙樻洿鍓嶆暟锟?
     */
    private Map<String, Object> beforeData;

    /**
     * 鍙樻洿鍚庢暟锟?
     */
    private Map<String, Object> afterData;

    /**
     * 鍙樻洿瀛楁鍒楄〃
     */
    private String[] changedFields;

    /**
     * 鍒嗗竷寮忚拷锟絀D
     */
    private String traceId;

    /**
     * Span ID
     */
    private String spanId;

    /**
     * 鎿嶄綔鐢ㄦ埛 ID
     */
    private String operatorId;

    /**
     * 绉熸埛 ID
     */
    private String tenantId;

    /**
     * 閲嶈瘯娆℃暟
     */
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * 鏈€澶ч噸璇曟锟?
     */
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * 棣栨澶辫触鏃堕棿
     */
    private Instant firstFailureTime;

    /**
     * 鏈€鍚庡け璐ュ師锟?
     */
    private String lastFailureReason;

    // ==================== 宸ュ巶鏂规硶 ====================

    /**
     * 鍒涘缓浜嬩欢
     */
    public static DataSyncEvent create(String aggregateType, String primaryId,
                                        DataSyncEventType eventType,
                                        Map<String, Object> data) {
        return DataSyncEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .eventTime(Instant.now())
                .aggregateType(aggregateType)
                .primaryId(primaryId)
                .afterData(data)
                .version(1L)
                .build();
    }

    /**
     * 鍒涘缓鎻掑叆浜嬩欢
     */
    public static DataSyncEvent ofInsert(String aggregateType, String primaryId,
                                          Map<String, Object> data) {
        return create(aggregateType, primaryId, DataSyncEventType.INSERT, data);
    }

    /**
     * 鍒涘缓鏇存柊浜嬩欢
     */
    public static DataSyncEvent ofUpdate(String aggregateType, String primaryId,
                                          Map<String, Object> beforeData,
                                          Map<String, Object> afterData,
                                          String... changedFields) {
        return DataSyncEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(DataSyncEventType.UPDATE)
                .eventTime(Instant.now())
                .aggregateType(aggregateType)
                .primaryId(primaryId)
                .beforeData(beforeData)
                .afterData(afterData)
                .changedFields(changedFields)
                .version(1L)
                .build();
    }

    /**
     * 鍒涘缓鍒犻櫎浜嬩欢
     */
    public static DataSyncEvent ofDelete(String aggregateType, String primaryId) {
        return create(aggregateType, primaryId, DataSyncEventType.DELETE, null);
    }

    // ==================== 杈呭姪鏂规硶 ====================

    /**
     * 澧炲姞閲嶈瘯娆℃暟
     */
    public void incrementRetry(String failureReason) {
        this.retryCount++;
        this.lastFailureReason = failureReason;
        if (this.firstFailureTime == null) {
            this.firstFailureTime = Instant.now();
        }
    }

    /**
     * 鏄惁鍙噸锟?
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetries;
    }

    /**
     * 鑾峰彇鍒嗗尯閿紙鐢ㄤ簬 Kafka 鍒嗗尯锟?
     */
    public String getPartitionKey() {
        return this.aggregateType + ":" + this.primaryId;
    }

    /**
     * 鑾峰彇涓婚鍚嶇О
     */
    public String getTopicName(String prefix) {
        return prefix + "." + this.aggregateType.toLowerCase();
    }
}
