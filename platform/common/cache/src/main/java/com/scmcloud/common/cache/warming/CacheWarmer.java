package com.scmcloud.common.cache.warming;

public interface CacheWarmer {
    void warmCache();
    String getWarmerName();
    default int getOrder() { return 0; }
}
