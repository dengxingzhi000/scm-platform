package com.scmcloud.warehouse.domain.entity;

import lombok.Getter;

@Getter
public enum InboundStatus {

    WAITING(0, "待入库"),
    PROCESSING(1, "入库中"),
    PARTIAL(2, "部分入库"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String description;

    InboundStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static InboundStatus fromCode(int code) {
        for (InboundStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown inbound status code: " + code);
    }
}
