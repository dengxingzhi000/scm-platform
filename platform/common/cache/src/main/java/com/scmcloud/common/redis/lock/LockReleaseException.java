package com.scmcloud.common.redis.lock;

public class LockReleaseException extends RuntimeException {

    public LockReleaseException(String lockKey) {
        super("Failed to release lock '" + lockKey + "'");
    }
}

