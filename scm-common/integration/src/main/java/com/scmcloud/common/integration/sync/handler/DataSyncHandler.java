package com.scmcloud.common.integration.sync.handler;

import com.scmcloud.common.integration.sync.event.DataSyncEvent;
import lombok.Getter;

/**
 * 鏁版嵁鍚屾澶勭悊鍣ㄦ帴锟?
 * <p>
 * 姣忎釜鑱氬悎绫诲瀷瀹炵幇鑷繁鐨勫鐞嗗櫒
 *
 * @author Deng
 * @since 2025-12-16
 */
public interface DataSyncHandler {
    /**
     * 鑾峰彇澶勭悊鐨勮仛鍚堢被锟?
     *
     * @return 鑱氬悎绫诲瀷锛堝 User, Dept, Role锟?
     */
    String getAggregateType();

    /**
     * 澶勭悊鍚屾浜嬩欢
     *
     * @param event 鍚屾浜嬩欢
     * @throws DataSyncException 澶勭悊澶辫触鏃舵姏锟?
     */
    void handle(DataSyncEvent event) throws DataSyncException;

    /**
     * 鍏ㄩ噺鍚屾锛堝璐︿慨澶嶆椂璋冪敤锟?
     *
     * @param primaryId 涓婚敭 ID
     */
    default void fullSync(String primaryId) {
        // 榛樿绌哄疄锟?
    }

    /**
     * 鏁版嵁鍚屾寮傚父
     */
    @Getter
    class DataSyncException extends RuntimeException {
        private final boolean retryable;

        public DataSyncException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }

        public DataSyncException(String message, Throwable cause, boolean retryable) {
            super(message, cause);
            this.retryable = retryable;
        }
    }
}
