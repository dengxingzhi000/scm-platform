package com.cognora.platform.message.api.enums;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    RETRYING
}
