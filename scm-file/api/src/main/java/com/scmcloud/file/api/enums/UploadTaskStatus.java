package com.scmcloud.file.api.enums;

public enum UploadTaskStatus {
    PENDING(0),
    UPLOADING(1),
    COMPLETED(2),
    FAILED(3);

    private final int code;

    UploadTaskStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static UploadTaskStatus fromCode(int code) {
        for (UploadTaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown UploadTaskStatus code: " + code);
    }
}
