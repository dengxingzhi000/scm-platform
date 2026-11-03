package com.cognora.platform.file.service;

/**
 * 当前租户下找不到文件元数据时抛出。
 */
public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(String fileId) {
        super("File not found: " + fileId);
    }
}
