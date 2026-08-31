package com.scmcloud.file.service.upload;

import com.scmcloud.file.api.enums.UploadTaskStatus;
import com.scmcloud.file.config.StorageConfig;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.entity.UploadTask;
import com.scmcloud.file.mapper.UploadTaskMapper;
import com.scmcloud.file.service.metadata.FileMetadataService;
import com.scmcloud.file.service.scan.FileVirusScanner;
import com.scmcloud.file.service.storage.StorageEngine;
import com.scmcloud.file.service.storage.StorageException;
import com.scmcloud.file.service.storage.StorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {
    private final StorageFactory storageFactory;
    private final FileMetadataService metadataService;
    private final InstantUploadService instantUploadService;
    private final UploadTaskMapper uploadTaskMapper;
    private final FileUploadValidator fileUploadValidator;
    private final FileVirusScanner virusScanner;
    private final StorageConfig storageConfig;

    public FileMetadata upload(MultipartFile file, String fileName, String contentType,
                               String bizType, String bizId, String tenantId) {
        fileUploadValidator.validate(file);

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("scm-upload-", ".tmp");
            file.transferTo(tempFile);

            String md5 = computeMd5(tempFile);
            Path finalTempFile = tempFile;

            return instantUploadService.checkExist(md5, tenantId)
                    .orElseGet(() -> doUpload(finalTempFile, fileName, contentType, md5, bizType, bizId, tenantId));

        } catch (StorageException | FileValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("File upload failed, fileName={}, tenantId={}", fileName, tenantId, e);
            throw new StorageException("File upload failed", e);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    private FileMetadata doUpload(Path tempFile, String fileName, String contentType,
                                  String md5, String bizType, String bizId, String tenantId) {
        FileVirusScanner.ScanResult scan = scan(tempFile);
        if (!scan.isClean()) {
            throw new FileValidationException("File rejected by virus scanner: " + scan.getThreatName());
        }

        StorageEngine engine = storageFactory.getDefaultEngine();
        FileMetadata metadata;
        String storageKey = null;
        try (InputStream content = Files.newInputStream(tempFile)) {
            metadata = engine.upload(content, Files.size(tempFile), fileName, contentType, tenantId);
            storageKey = metadata.getStorageKey();
        } catch (Exception e) {
            throw new StorageException("Failed to store file content", e);
        }

        try {
            metadata.setMd5(md5);
            metadata.setFileSize(Files.size(tempFile));
            metadata.setVersion(1);
            metadata.setBizType(bizType);
            metadata.setBizId(bizId);
            metadata.setCreateBy(tenantId);
            metadataService.saveMetadata(metadata);
            return metadata;
        } catch (Exception e) {
            if (storageKey != null) {
                try {
                    engine.delete(storageKey);
                    log.warn("Rolled back orphan object {} after metadata save failure", storageKey);
                } catch (Exception deleteEx) {
                    log.error("Failed to clean up orphan object {}", storageKey, deleteEx);
                }
            }
            throw new StorageException("Failed to persist file metadata", e);
        }
    }

    private FileVirusScanner.ScanResult scan(Path tempFile) {
        if (!virusScanner.enabled()) {
            return FileVirusScanner.ScanResult.clean();
        }
        try (InputStream content = Files.newInputStream(tempFile)) {
            return virusScanner.scan(content);
        } catch (Exception e) {
            log.error("Virus scan failed for temp file", e);
            throw new StorageException("Virus scan failed", e);
        }
    }

    private String computeMd5(Path tempFile) {
        try (InputStream content = Files.newInputStream(tempFile)) {
            return DigestUtils.md5Hex(content);
        } catch (Exception e) {
            throw new StorageException("Failed to compute file hash", e);
        }
    }

    public FileMetadata uploadBytes(byte[] content, String fileName, String contentType,
                                    String bizType, String bizId, String tenantId) {
        fileUploadValidator.validate(content, fileName);

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("scm-upload-", ".tmp");
            Files.write(tempFile, content);

            String md5 = computeMd5(tempFile);
            final Path finalTempFile = tempFile;
            return instantUploadService.checkExist(md5, tenantId)
                    .orElseGet(() -> doUpload(finalTempFile, fileName, contentType, md5, bizType, bizId, tenantId));
        } catch (StorageException | FileValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Byte upload failed, fileName={}, tenantId={}", fileName, tenantId, e);
            throw new StorageException("Byte upload failed", e);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    public String initMultipartUpload(String fileName, long fileSize, String tenantId) {
        UploadTask task = new UploadTask();
        task.setId(UUID.randomUUID().toString());
        task.setFileName(fileName);
        task.setFileSize(fileSize);
        task.setStatus(UploadTaskStatus.INIT);
        task.setTenantId(tenantId);
        uploadTaskMapper.insert(task);
        return task.getId();
    }

    private void deleteQuietly(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception e) {
                log.warn("Failed to delete temp upload file {}", tempFile, e);
            }
        }
    }
}
