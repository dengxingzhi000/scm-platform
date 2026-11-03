package com.scmcloud.message.api.enums;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    RETRYING
}
