package com.scmcloud.warehouse.domain.entity;

import lombok.Getter;

@Getter
public enum WavePickingStatus {

    WAITING(0, "待拣货"),
    PICKING(1, "拣货中"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String description;

    WavePickingStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static WavePickingStatus fromCode(int code) {
        for (WavePickingStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown wave picking status code: " + code);
    }
}
