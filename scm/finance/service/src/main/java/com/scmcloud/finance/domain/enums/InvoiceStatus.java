package com.scmcloud.finance.domain.enums;

import lombok.Getter;

@Getter
public enum InvoiceStatus {

    DRAFT(0, "草稿"),
    ISSUED(1, "已开具"),
    MAILED(2, "已寄出"),
    VOIDED(3, "已作废"),
    RED_FLUSHED(4, "已红冲");

    private final int code;
    private final String description;

    InvoiceStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static InvoiceStatus fromCode(int code) {
        for (InvoiceStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown invoice status code: " + code);
    }
}
