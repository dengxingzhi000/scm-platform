package com.scmcloud.common.integration.sync.publisher;

import com.scmcloud.common.integration.sync.event.DataSyncEvent;

import java.util.List;

/**
 * 鏁版嵁鍚屾浜嬩欢鍙戝竷鍣ㄦ帴锟?
 *
 * @author Deng
 * @since 2025-12-16
 */
public interface DataSyncPublisher {
    /**
     * 鍚屾鍙戝竷浜嬩欢
     *
     * @param event 鍚屾浜嬩欢
     * @return 鏄惁鍙戝竷鎴愬姛
     */
    boolean publish(DataSyncEvent event);

    /**
     * 寮傛鍙戝竷浜嬩欢锛坒ire-and-forget锟?
     * <p>
     * 鍐呴儴浣跨敤 CompletableFuture 澶勭悊鍥炶皟锛屼絾涓嶆毚闇茬粰璋冪敤锟?
     *
     * @param event 鍚屾浜嬩欢
     */
    void publishAsync(DataSyncEvent event);

    /**
     * 鎵归噺鍙戝竷浜嬩欢
     *
     * @param events 浜嬩欢鍒楄〃
     * @return 鎴愬姛鏁伴噺
     */
    int publishBatch(List<DataSyncEvent> events);

    /**
     * 鍙戝竷鍒版淇¢槦锟?
     *
     * @param event 澶辫触浜嬩欢
     * @param reason 澶辫触鍘熷洜
     */
    void publishToDeadLetter(DataSyncEvent event, String reason);
}
