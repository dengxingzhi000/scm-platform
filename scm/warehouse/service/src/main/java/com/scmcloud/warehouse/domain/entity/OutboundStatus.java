package com.scmcloud.warehouse.domain.entity;

import lombok.Getter;

@Getter
public enum OutboundStatus {

    WAITING(0, "待拣货"),
    PICKING(1, "拣货中"),
    PACKED(2, "已打包"),
    SHIPPED(3, "已出库"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String description;

    OutboundStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OutboundStatus fromCode(int code) {
        for (OutboundStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown outbound status code: " + code);
    }
}
