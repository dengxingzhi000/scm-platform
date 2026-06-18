package com.scmcloud.file.api.enums;

public enum UploadTaskStatus {
    INIT(0),
    PENDING(1),
    UPLOADING(2),
    COMPLETED(3),
    FAILED(4);

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
