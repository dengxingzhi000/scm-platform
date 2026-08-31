package com.scmcloud.file.service.upload;

import com.scmcloud.file.config.StorageConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class FileUploadValidatorTest {

    private StorageConfig configWith(long maxFileSize, java.util.List<String> ext, java.util.List<String> types) {
        StorageConfig config = new StorageConfig();
        config.setMaxFileSize(maxFileSize);
        config.setAllowedExtensions(ext);
        config.setAllowedContentTypes(types);
        return config;
    }

    @Test
    void shouldAcceptValidFile() {
        FileUploadValidator validator = new FileUploadValidator(
                configWith(100, java.util.List.of("txt"), java.util.List.of("text/plain")));
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());
        assertDoesNotThrow(() -> validator.validate(file));
    }

    @Test
    void shouldRejectOversizedFile() {
        FileUploadValidator validator = new FileUploadValidator(configWith(2, java.util.List.of(), java.util.List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hello world".getBytes());
        FileValidationException ex = assertThrows(FileValidationException.class, () -> validator.validate(file));
        assertTrue(ex.getMessage().contains("exceeds"));
    }

    @Test
    void shouldRejectDisallowedExtension() {
        FileUploadValidator validator = new FileUploadValidator(
                configWith(100, java.util.List.of("pdf"), java.util.List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "a.exe", "application/octet-stream", "x".getBytes());
        assertThrows(FileValidationException.class, () -> validator.validate(file));
    }

    @Test
    void shouldRejectEmptyFile() {
        FileUploadValidator validator = new FileUploadValidator(configWith(100, java.util.List.of(), java.util.List.of()));
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[0]);
        assertThrows(FileValidationException.class, () -> validator.validate(file));
    }
}
