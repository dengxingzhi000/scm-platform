package com.scmcloud.file.service.upload;

import com.scmcloud.file.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadValidator {

    private final StorageConfig storageConfig;

    public void validate(MultipartFile file) {        if (file == null || file.isEmpty()) {
            throw new FileValidationException("Uploaded file is empty");
        }

        long maxSize = storageConfig.getMaxFileSize();
        if (maxSize > 0 && file.getSize() > maxSize) {
            throw new FileValidationException(
                    String.format("File size %d exceeds the maximum allowed size %d", file.getSize(), maxSize));
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new FileValidationException("File name is missing");
        }

        String extension = extractExtension(originalName);
        List<String> allowedExtensions = storageConfig.getAllowedExtensions();
        if (allowedExtensions != null && !allowedExtensions.isEmpty()) {
            boolean matched = allowedExtensions.stream()
                    .anyMatch(allowed -> allowed.equalsIgnoreCase(extension));
            if (!matched) {
                throw new FileValidationException("File extension not allowed: " + extension);
            }
        }

        String contentType = file.getContentType();
        List<String> allowedContentTypes = storageConfig.getAllowedContentTypes();
        if (allowedContentTypes != null && !allowedContentTypes.isEmpty() && contentType != null && !contentType.isBlank()) {
            boolean matched = allowedContentTypes.stream()
                    .anyMatch(allowed -> contentTypeMatches(allowed, contentType));
            if (!matched) {
                throw new FileValidationException("Content type not allowed: " + contentType);
            }
        }
    }

    public void validate(byte[] content, String originalName) {
        if (content == null || content.length == 0) {
            throw new FileValidationException("Uploaded file is empty");
        }

        long maxSize = storageConfig.getMaxFileSize();
        if (maxSize > 0 && content.length > maxSize) {
            throw new FileValidationException(
                    String.format("File size %d exceeds the maximum allowed size %d", content.length, maxSize));
        }

        if (originalName == null || originalName.isBlank()) {
            throw new FileValidationException("File name is missing");
        }
    }

    private boolean contentTypeMatches(String allowed, String actual) {
        if (allowed.endsWith("/*")) {
            return actual.startsWith(allowed.substring(0, allowed.indexOf("/*") + 1));
        }
        return allowed.equalsIgnoreCase(actual);
    }

    private String extractExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }
}
