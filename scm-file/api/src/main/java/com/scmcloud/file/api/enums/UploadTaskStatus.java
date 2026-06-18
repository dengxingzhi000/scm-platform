package com.scmcloud.file.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UploadTaskStatus {
    INIT(0, "Initial"),
    UPLOADING(1, "Uploading"),
    SUCCESS(2, "Success"),
    FAILED(3, "Failed");

    @EnumValue
    private final int code;
    private final String desc;

    UploadTaskStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
