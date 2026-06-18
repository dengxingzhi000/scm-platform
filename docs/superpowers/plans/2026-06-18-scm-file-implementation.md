# scm-file Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build unified file storage service with multi-backend support, instant upload, resume upload, and version management.

**Architecture:** New Spring Boot module with SPI-based storage engine abstraction (MinIO/OSS/S3), metadata in PostgreSQL, cache in Redis. Uses Dubbo RPC for service exposure.

**Tech Stack:** Spring Boot 4.x, MinIO SDK, MyBatis-Plus, Redis, Dubbo 3.x

---

## File Structure

```
scm-file/
├── pom.xml
├── api/
│   ├── pom.xml
│   └── src/main/java/com/scmcloud/file/api/
│       ├── FileQueryApi.java
│       ├── FileManageApi.java
│       ├── dto/
│       │   └── FileMetadataDTO.java
│       └── enums/
│           ├── StorageType.java
│           └── UploadTaskStatus.java
├── service/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/scmcloud/file/
│       │   │   ├── ScmFileApplication.java
│       │   │   ├── config/
│       │   │   │   ├── FileConfig.java
│       │   │   │   └── StorageConfig.java
│       │   │   ├── controller/
│       │   │   │   └── FileController.java
│       │   │   ├── convert/
│       │   │   │   └── FileMetadataConvert.java
│       │   │   ├── service/
│       │   │   │   ├── storage/
│       │   │   │   │   ├── StorageEngine.java
│       │   │   │   │   ├── StorageFactory.java
│       │   │   │   │   ├── MinioStorageEngine.java
│       │   │   │   │   ├── OssStorageEngine.java
│       │   │   │   │   └── S3StorageEngine.java
│       │   │   │   ├── upload/
│       │   │   │   │   ├── UploadService.java
│       │   │   │   │   ├── InstantUploadService.java
│       │   │   │   │   └── ResumeUploadService.java
│       │   │   │   ├── metadata/
│       │   │   │   │   ├── FileMetadataService.java
│       │   │   │   │   └── FileVersionService.java
│       │   │   │   └── preview/
│       │   │   │       ├── ImagePreviewService.java
│       │   │   │       └── DocumentPreviewService.java
│       │   │   ├── entity/
│       │   │   │   ├── FileMetadata.java
│       │   │   │   ├── FileVersion.java
│       │   │   │   └── UploadTask.java
│       │   │   └── mapper/
│       │   │       ├── FileMetadataMapper.java
│       │   │       ├── FileVersionMapper.java
│       │   │       └── UploadTaskMapper.java
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       │           └── V1_0_0__create_file_tables.sql
│       └── test/
│           └── java/com/scmcloud/file/
│               ├── service/
│               │   ├── storage/
│               │   │   └── StorageEngineTest.java
│               │   ├── upload/
│               │   │   ├── UploadServiceTest.java
│               │   │   └── InstantUploadServiceTest.java
│               │   └── metadata/
│               │       └── FileMetadataServiceTest.java
│               └── integration/
│                   └── FileUploadIntegrationTest.java
```

---

## Task 1: Create Module Structure

**Files:**
- Create: `scm-file/pom.xml`
- Create: `scm-file/api/pom.xml`
- Create: `scm-file/service/pom.xml`
- Modify: `com.scm.parent/pom.xml`

- [ ] **Step 1: Create parent POM for scm-file**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.scmcloud</groupId>
    <artifactId>scm-file</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>SCM File Service</name>
    <description>Unified file storage service</description>

    <modules>
        <module>api</module>
        <module>service</module>
    </modules>
</project>
```

- [ ] **Step 2: Create API module POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.scmcloud</groupId>
        <artifactId>scm-file</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>scm-file-api</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.scmcloud</groupId>
            <artifactId>scm-common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Create service module POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.scmcloud</groupId>
        <artifactId>scm-file</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>scm-file-service</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.scmcloud</groupId>
            <artifactId>scm-file-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.scmcloud</groupId>
            <artifactId>scm-common-data</artifactId>
        </dependency>
        <dependency>
            <groupId>com.scmcloud</groupId>
            <artifactId>scm-common-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>8.5.7</version>
        </dependency>
        <dependency>
            <groupId>com.aliyun.oss</groupId>
            <artifactId>aliyun-sdk-oss</artifactId>
            <version>3.17.4</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Add scm-file to parent POM**

Add to `com.scm.parent/pom.xml` in `<modules>`:
```xml
<module>../scm-file</module>
```

- [ ] **Step 5: Verify build**

```bash
mvn clean install -f scm-file/pom.xml -DskipTests
```

- [ ] **Step 6: Commit**

```bash
git add scm-file/ com.scm.parent/pom.xml
git commit -m "feat(file): create scm-file module structure"
```

---

## Task 2: Create Database Schema

**Files:**
- Create: `scm-file/service/src/main/resources/db/migration/V1_0_0__create_file_tables.sql`

- [ ] **Step 1: Write migration SQL**

```sql
-- File metadata table
CREATE TABLE sys_file_metadata (
    id              VARCHAR(36) PRIMARY KEY,
    original_name   VARCHAR(255) NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    content_type    VARCHAR(100),
    file_size       BIGINT,
    storage_engine  VARCHAR(50) NOT NULL,
    md5             VARCHAR(32),
    version         INTEGER DEFAULT 1,
    biz_type        VARCHAR(50),
    biz_id          VARCHAR(100),
    status          VARCHAR(20) DEFAULT 'NORMAL',
    ref_count       INTEGER DEFAULT 1,
    tenant_id       BIGINT NOT NULL,
    create_by       BIGINT,
    update_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0
);

CREATE INDEX idx_file_metadata_md5 ON sys_file_metadata(md5);
CREATE INDEX idx_file_metadata_tenant ON sys_file_metadata(tenant_id);
CREATE INDEX idx_file_metadata_biz ON sys_file_metadata(biz_type, biz_id);

-- File version table
CREATE TABLE sys_file_version (
    id              VARCHAR(36) PRIMARY KEY,
    file_id         VARCHAR(36) NOT NULL,
    version         INTEGER NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    file_size       BIGINT,
    md5             VARCHAR(32),
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id       BIGINT NOT NULL
);

CREATE INDEX idx_file_version_file_id ON sys_file_version(file_id);

-- Upload task table
CREATE TABLE sys_upload_task (
    id              VARCHAR(36) PRIMARY KEY,
    file_name       VARCHAR(255) NOT NULL,
    file_size       BIGINT NOT NULL,
    md5             VARCHAR(32),
    storage_key     VARCHAR(500),
    total_parts     INTEGER,
    completed_parts INTEGER DEFAULT 0,
    status          SMALLINT NOT NULL DEFAULT 0,
    upload_id       VARCHAR(100),
    tenant_id       BIGINT NOT NULL,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0,
    lock_version    INTEGER DEFAULT 0
);

CREATE INDEX idx_upload_task_status ON sys_upload_task(status);
CREATE INDEX idx_upload_task_md5 ON sys_upload_task(md5);
```

- [ ] **Step 2: Run migration**

```bash
mvn flyway:migrate -f scm-file/service/pom.xml
```

- [ ] **Step 3: Commit**

```bash
git add scm-file/service/src/main/resources/db/migration/
git commit -m "feat(file): add database schema for file storage"
```

---

## Task 3: Create Entity and Mapper Classes

**Files:**
- Create: `scm-file/service/src/main/java/com/scmcloud/file/entity/FileMetadata.java`
- Create: `scm-file/service/src/main/java/com/scmcloud/file/entity/FileVersion.java`
- Create: `scm-file/service/src/main/java/com/scmcloud/file/entity/UploadTask.java`
- Create: `scm-file/service/src/main/java/com/scmcloud/file/mapper/FileMetadataMapper.java`
- Create: `scm-file/service/src/main/java/com/scmcloud/file/mapper/FileVersionMapper.java`
- Create: `scm-file/service/src/main/java/com/scmcloud/file/mapper/UploadTaskMapper.java`

- [ ] **Step 1: Create FileMetadata entity**

```java
package com.scmcloud.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_file_metadata")
public class FileMetadata {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String originalName;
    private String storageKey;
    private String contentType;
    private Long fileSize;
    private String storageEngine;
    private String md5;
    private Integer version;
    private String bizType;
    private String bizId;
    private String status;
    private Integer refCount;
    private Long tenantId;
    private Long createBy;
    private Long updateBy;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 2: Create FileVersion entity**

```java
package com.scmcloud.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_file_version")
public class FileVersion {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String fileId;
    private Integer version;
    private String storageKey;
    private Long fileSize;
    private String md5;
    private Long createBy;
    private Date createTime;
    private Long tenantId;
}
```

- [ ] **Step 3: Create UploadTask entity**

```java
package com.scmcloud.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.scmcloud.file.api.enums.UploadTaskStatus;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_upload_task")
public class UploadTask {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String fileName;
    private Long fileSize;
    private String md5;
    private String storageKey;
    private Integer totalParts;
    private Integer completedParts;
    private UploadTaskStatus status;
    private String uploadId;
    private Long tenantId;
    private Long createBy;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer deleted;
    @Version
    private Integer lockVersion;
}
```

- [ ] **Step 4: Create mapper interfaces**

```java
package com.scmcloud.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.file.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {
}
```

```java
package com.scmcloud.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.file.entity.FileVersion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileVersionMapper extends BaseMapper<FileVersion> {
}
```

```java
package com.scmcloud.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.file.entity.UploadTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UploadTaskMapper extends BaseMapper<UploadTask> {
}
```

- [ ] **Step 5: Create FileMetadataConvert (MapStruct)**

```java
package com.scmcloud.file.convert;

import com.scmcloud.file.api.dto.FileMetadataDTO;
import com.scmcloud.file.entity.FileMetadata;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FileMetadataConvert {
    
    FileMetadataDTO toDTO(FileMetadata entity);
    
    FileMetadata toEntity(FileMetadataDTO dto);
}
```

- [ ] **Step 6: Commit**

```bash
git add scm-file/service/src/main/java/com/scmcloud/file/entity/ scm-file/service/src/main/java/com/scmcloud/file/mapper/ scm-file/service/src/main/java/com/scmcloud/file/convert/
git commit -m "feat(file): add entity, mapper and converter classes"
```

---

## Task 4: Create Storage SPI Interface

**Files:**
- Create: `scm-file/service/src/main/java/com/scmcloud/file/service/storage/StorageEngine.java`
- Create: `scm-file/service/src/main/java/com/scmcloud/file/service/storage/StorageFactory.java`
- Create: `scm-file/api/src/main/java/com/scmcloud/file/api/enums/StorageType.java`

- [ ] **Step 1: Create StorageType enum**

```java
package com.scmcloud.file.api.enums;

public enum StorageType {
    MINIO,
    OSS,
    S3
}
```

Create UploadTaskStatus enum:

```java
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
```

- [ ] **Step 2: Create StorageEngine interface**

```java
package com.scmcloud.file.service.storage;

import com.scmcloud.file.entity.FileMetadata;
import java.io.InputStream;
import java.time.Duration;

public interface StorageEngine {
    
    FileMetadata upload(byte[] fileBytes, String fileName, String contentType, Long tenantId);
    
    InputStream download(String fileKey);
    
    String generatePresignedUrl(String fileKey, Duration expiry);
    
    void delete(String fileKey);
    
    boolean exists(String fileKey);
    
    StorageType support();
}
```

- [ ] **Step 3: Create StorageFactory**

```java
package com.scmcloud.file.service.storage;

import com.scmcloud.file.api.enums.StorageType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class StorageFactory {
    
    private final List<StorageEngine> engineList;
    private final Map<StorageType, StorageEngine> engineMap = new EnumMap<>(StorageType.class);
    
    public StorageFactory(List<StorageEngine> engineList) {
        this.engineList = engineList;
    }
    
    @PostConstruct
    public void init() {
        for (StorageEngine engine : engineList) {
            StorageType type = engine.support();
            engineMap.put(type, engine);
            log.info("Registered storage engine: {} -> {}", type, engine.getClass().getSimpleName());
        }
    }
    
    public StorageEngine getEngine(StorageType type) {
        StorageEngine engine = engineMap.get(type);
        if (engine == null) {
            throw new IllegalArgumentException("No storage engine found for type: " + type);
        }
        return engine;
    }
    
    public StorageEngine getDefaultEngine() {
        return getEngine(StorageType.MINIO);
    }
}
```

- [ ] **Step 4: Write test for StorageFactory**

```java
package com.scmcloud.file.service.storage;

import com.scmcloud.file.api.enums.StorageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageFactoryTest {
    
    @Test
    void shouldGetEngineByType() {
        // Given
        MinioStorageEngine minioEngine = mock(MinioStorageEngine.class);
        when(minioEngine.support()).thenReturn(StorageType.MINIO);
        
        StorageFactory factory = new StorageFactory(List.of(minioEngine));
        factory.init();
        
        // When
        StorageEngine result = factory.getEngine(StorageType.MINIO);
        
        // Then
        assertEquals(minioEngine, result);
    }
    
    @Test
    void shouldThrowExceptionForUnsupportedType() {
        // Given
        StorageFactory factory = new StorageFactory(List.of());
        factory.init();
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> factory.getEngine(StorageType.MINIO));
    }
}
```

- [ ] **Step 5: Run test**

```bash
mvn test -pl scm-file/service -Dtest=StorageFactoryTest -f com.scm.parent/pom.xml
```

- [ ] **Step 6: Commit**

```bash
git add scm-file/service/src/main/java/com/scmcloud/file/service/storage/
git commit -m "feat(file): add storage SPI interface and factory"
```

---

## Task 5: Implement MinIO Storage Engine

**Files:**
- Create: `scm-file/service/src/main/java/com/scmcloud/file/service/storage/MinioStorageEngine.java`
- Create: `scm-file/service/src/main/java/com/scmcloud/file/config/StorageConfig.java`

- [ ] **Step 1: Create StorageConfig**

```java
package com.scmcloud.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage.minio")
public class StorageConfig {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private boolean createBucket = true;
}
```

- [ ] **Step 2: Write test for MinioStorageEngine**

```java
package com.scmcloud.file.service.storage;

import com.scmcloud.file.config.StorageConfig;
import com.scmcloud.file.entity.FileMetadata;
import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioStorageEngineTest {
    
    @Mock
    private MinioClient minioClient;
    
    @Mock
    private StorageConfig config;
    
    @InjectMocks
    private MinioStorageEngine engine;
    
    @BeforeEach
    void setUp() {
        when(config.getBucketName()).thenReturn("test-bucket");
    }
    
    @Test
    void shouldUploadFile() throws Exception {
        // Given
        byte[] fileBytes = "test content".getBytes();
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        
        // When
        FileMetadata result = engine.upload(fileBytes, "test.txt", "text/plain", 1L);
        
        // Then
        assertNotNull(result);
        assertEquals("test.txt", result.getOriginalName());
        assertEquals("text/plain", result.getContentType());
        assertEquals(12L, result.getFileSize());
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }
    
    @Test
    void shouldCheckFileExists() throws Exception {
        // Given
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        
        // When
        boolean exists = engine.exists("test-key");
        
        // Then
        assertTrue(exists);
    }
}
```

- [ ] **Step 3: Implement MinioStorageEngine**

```java
package com.scmcloud.file.service.storage;

import com.scmcloud.file.api.enums.StorageType;
import com.scmcloud.file.config.StorageConfig;
import com.scmcloud.file.entity.FileMetadata;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioStorageEngine implements StorageEngine {
    
    private final MinioClient minioClient;
    private final StorageConfig config;
    
    @Override
    public FileMetadata upload(byte[] fileBytes, String fileName, String contentType, Long tenantId) {
        try {
            String storageKey = generateStorageKey(tenantId, fileName);
            
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(config.getBucketName())
                    .object(storageKey)
                    .stream(new ByteArrayInputStream(fileBytes), fileBytes.length, 10485760)
                    .contentType(contentType)
                    .build());
            
            FileMetadata metadata = new FileMetadata();
            metadata.setOriginalName(fileName);
            metadata.setStorageKey(storageKey);
            metadata.setContentType(contentType);
            metadata.setFileSize((long) fileBytes.length);
            metadata.setStorageEngine("MINIO");
            metadata.setTenantId(tenantId);
            
            return metadata;
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new RuntimeException("File upload failed", e);
        }
    }
    
    @Override
    public InputStream download(String fileKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(config.getBucketName())
                    .object(fileKey)
                    .build());
        } catch (Exception e) {
            log.error("Failed to download file from MinIO", e);
            throw new RuntimeException("File download failed", e);
        }
    }
    
    @Override
    public String generatePresignedUrl(String fileKey, Duration expiry) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(config.getBucketName())
                    .object(fileKey)
                    .expiry((int) expiry.toSeconds())
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            throw new RuntimeException("URL generation failed", e);
        }
    }
    
    @Override
    public void delete(String fileKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(config.getBucketName())
                    .object(fileKey)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO", e);
            throw new RuntimeException("File deletion failed", e);
        }
    }
    
    @Override
    public boolean exists(String fileKey) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(config.getBucketName())
                    .object(fileKey)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public StorageType support() {
        return StorageType.MINIO;
    }
    
    private String generateStorageKey(Long tenantId, String fileName) {
        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = fileName.substring(lastDot);
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return tenantId + "/" + datePath + "/" + UUID.randomUUID() + extension;
    }
}
```

- [ ] **Step 4: Run test**

```bash
mvn test -pl scm-file/service -Dtest=MinioStorageEngineTest -f com.scm.parent/pom.xml
```

- [ ] **Step 5: Commit**

```bash
git add scm-file/service/src/main/java/com/scmcloud/file/service/storage/MinioStorageEngine.java
git add scm-file/service/src/main/java/com/scmcloud/file/config/StorageConfig.java
git commit -m "feat(file): implement MinIO storage engine"
```

---

## Task 6: Create FileMetadata Service

**Files:**
- Create: `scm-file/service/src/main/java/com/scmcloud/file/service/metadata/FileMetadataService.java`

- [ ] **Step 1: Write test for FileMetadataService**

```java
package com.scmcloud.file.service.metadata;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.mapper.FileMetadataMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileMetadataServiceTest {
    
    @Mock
    private FileMetadataMapper mapper;
    
    @InjectMocks
    private FileMetadataService service;
    
    @Test
    void shouldSaveFileMetadata() {
        // Given
        FileMetadata metadata = new FileMetadata();
        metadata.setId("test-id");
        metadata.setOriginalName("test.txt");
        when(mapper.insert(any())).thenReturn(1);
        
        // When
        FileMetadata result = service.saveMetadata(metadata);
        
        // Then
        assertNotNull(result);
        verify(mapper).insert(metadata);
    }
    
    @Test
    void shouldFindByMd5() {
        // Given
        String md5 = "abc123";
        FileMetadata metadata = new FileMetadata();
        metadata.setMd5(md5);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(metadata);
        
        // When
        Optional<FileMetadata> result = service.findByMd5(md5, 1L);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(md5, result.get().getMd5());
    }
}
```

- [ ] **Step 2: Implement FileMetadataService**

```java
package com.scmcloud.file.service.metadata;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.mapper.FileMetadataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Slf4j
@Service
public class FileMetadataService extends ServiceImpl<FileMetadataMapper, FileMetadata> {
    
    public FileMetadata saveMetadata(FileMetadata metadata) {
        save(metadata);
        return metadata;
    }
    
    public Optional<FileMetadata> findByMd5(String md5, Long tenantId) {
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadata::getMd5, md5)
               .eq(FileMetadata::getTenantId, tenantId)
               .eq(FileMetadata::getDeleted, 0);
        return Optional.ofNullable(getOne(wrapper));
    }
    
    public Optional<FileMetadata> findById(String id, Long tenantId) {
        LambdaQueryWrapper<FileMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileMetadata::getId, id)
               .eq(FileMetadata::getTenantId, tenantId)
               .eq(FileMetadata::getDeleted, 0);
        return Optional.ofNullable(getOne(wrapper));
    }
}
```

- [ ] **Step 3: Run test**

```bash
mvn test -pl scm-file/service -Dtest=FileMetadataServiceTest -f com.scm.parent/pom.xml
```

- [ ] **Step 4: Commit**

```bash
git add scm-file/service/src/main/java/com/scmcloud/file/service/metadata/FileMetadataService.java
git commit -m "feat(file): add FileMetadata service"
```

---

## Task 7: Create API Interfaces

**Files:**
- Create: `scm-file/api/src/main/java/com/scmcloud/file/api/FileQueryApi.java`
- Create: `scm-file/api/src/main/java/com/scmcloud/file/api/FileManageApi.java`
- Create: `scm-file/api/src/main/java/com/scmcloud/file/api/dto/FileMetadataDTO.java`

- [ ] **Step 1: Create FileMetadataDTO**

```java
package com.scmcloud.file.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class FileMetadataDTO implements Serializable {
    private String id;
    private String originalName;
    private String storageKey;
    private String contentType;
    private Long fileSize;
    private String storageEngine;
    private String md5;
    private Integer version;
    private String bizType;
    private String bizId;
    private String status;
    private Long tenantId;
    private Date createTime;
}
```

- [ ] **Step 2: Create FileQueryApi interface (Dubbo RPC)**

```java
package com.scmcloud.file.api;

import com.scmcloud.file.api.dto.FileMetadataDTO;
import java.util.List;

public interface FileQueryApi {
    
    FileMetadataDTO getById(String id, Long tenantId);
    
    FileMetadataDTO getByMd5(String md5, Long tenantId);
    
    List<FileMetadataDTO> getByBizId(String bizType, String bizId, Long tenantId);
    
    String generatePresignedUrl(String fileKey, Long tenantId);
}
```

- [ ] **Step 3: Create FileManageApi interface (Dubbo RPC)**

```java
package com.scmcloud.file.api;

import com.scmcloud.file.api.dto.FileMetadataDTO;

public interface FileManageApi {
    
    void delete(String id, Long tenantId);
    
    void updateBizAssociation(String id, String bizType, String bizId, Long tenantId);
}
```

- [ ] **Step 4: Commit**

```bash
git add scm-file/api/src/main/java/com/scmcloud/file/api/
git commit -m "feat(file): add FileQuery and FileManage API interfaces"
```

---

## Task 8: Implement Upload Service

**Files:**
- Create: `scm-file/service/src/main/java/com/scmcloud/file/service/upload/UploadService.java`
- Create: `scm-file/service/src/main/java/com/scmcloud/file/service/upload/InstantUploadService.java`

- [ ] **Step 1: Write test for InstantUploadService**

```java
package com.scmcloud.file.service.upload;

import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.service.metadata.FileMetadataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstantUploadServiceTest {
    
    @Mock
    private FileMetadataService metadataService;
    
    @InjectMocks
    private InstantUploadService service;
    
    @Test
    void shouldReturnExistingFileWhenMd5Matches() {
        // Given
        String md5 = "abc123";
        Long tenantId = 1L;
        FileMetadata existing = new FileMetadata();
        existing.setId("existing-id");
        existing.setMd5(md5);
        when(metadataService.findByMd5(md5, tenantId)).thenReturn(Optional.of(existing));
        
        // When
        Optional<FileMetadata> result = service.checkExist(md5, tenantId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("existing-id", result.get().getId());
    }
    
    @Test
    void shouldReturnEmptyWhenMd5NotExists() {
        // Given
        String md5 = "not-exists";
        Long tenantId = 1L;
        when(metadataService.findByMd5(md5, tenantId)).thenReturn(Optional.empty());
        
        // When
        Optional<FileMetadata> result = service.checkExist(md5, tenantId);
        
        // Then
        assertFalse(result.isPresent());
    }
}
```

- [ ] **Step 2: Implement InstantUploadService**

```java
package com.scmcloud.file.service.upload;

import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.service.metadata.FileMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstantUploadService {
    
    private final FileMetadataService metadataService;
    
    public Optional<FileMetadata> checkExist(String md5, Long tenantId) {
        return metadataService.findByMd5(md5, tenantId);
    }
}
```

- [ ] **Step 3: Implement UploadService**

```java
package com.scmcloud.file.service.upload;

import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.entity.UploadTask;
import com.scmcloud.file.mapper.UploadTaskMapper;
import com.scmcloud.file.service.metadata.FileMetadataService;
import com.scmcloud.file.service.storage.StorageEngine;
import com.scmcloud.file.service.storage.StorageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {
    
    private final StorageFactory storageFactory;
    private final FileMetadataService metadataService;
    private final InstantUploadService instantUploadService;
    private final UploadTaskMapper uploadTaskMapper;
    
    @Transactional
    public FileMetadata upload(byte[] fileBytes, String fileName, String contentType,
                               String bizType, String bizId, Long tenantId) {
        String md5 = DigestUtils.md5Hex(fileBytes);
        
        return instantUploadService.checkExist(md5, tenantId)
                .orElseGet(() -> doUpload(fileBytes, fileName, contentType, md5, bizType, bizId, tenantId));
    }
    
    private FileMetadata doUpload(byte[] fileBytes, String fileName, String contentType,
                                   String md5, String bizType, String bizId, Long tenantId) {
        StorageEngine engine = storageFactory.getDefaultEngine();
        FileMetadata metadata = engine.upload(fileBytes, fileName, contentType, tenantId);
        metadata.setMd5(md5);
        metadata.setFileSize((long) fileBytes.length);
        metadata.setVersion(1);
        metadata.setBizType(bizType);
        metadata.setBizId(bizId);
        metadata.setCreateBy(tenantId); // TODO: get from TenantContextHolder
        metadataService.saveMetadata(metadata);
        return metadata;
    }
    
    public String initMultipartUpload(String fileName, Long fileSize, Long tenantId) {
        UploadTask task = new UploadTask();
        task.setId(UUID.randomUUID().toString());
        task.setFileName(fileName);
        task.setFileSize(fileSize);
        task.setStatus(UploadTaskStatus.INIT);
        task.setTenantId(tenantId);
        uploadTaskMapper.insert(task);
        return task.getId();
    }
}
```

- [ ] **Step 4: Run tests**

```bash
mvn test -pl scm-file/service -Dtest="InstantUploadServiceTest,UploadServiceTest" -f com.scm.parent/pom.xml
```

- [ ] **Step 5: Commit**

```bash
git add scm-file/service/src/main/java/com/scmcloud/file/service/upload/
git commit -m "feat(file): implement upload service with instant upload"
```

---

## Task 9: Create REST Controller

**Files:**
- Create: `scm-file/service/src/main/java/com/scmcloud/file/controller/FileController.java`

- [ ] **Step 1: Implement FileController**

```java
package com.scmcloud.file.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.file.api.dto.FileMetadataDTO;
import com.scmcloud.file.convert.FileMetadataConvert;
import com.scmcloud.file.entity.FileMetadata;
import com.scmcloud.file.service.upload.InstantUploadService;
import com.scmcloud.file.service.upload.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {
    
    private final UploadService uploadService;
    private final InstantUploadService instantUploadService;
    private final FileMetadataConvert fileMetadataConvert;
    
    @PostMapping("/upload")
    public ApiResponse<FileMetadataDTO> upload(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "bizType", required = false) String bizType,
                                                @RequestParam(value = "bizId", required = false) String bizId) {
        try {
            FileMetadata metadata = uploadService.upload(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    bizType,
                    bizId,
                    1L // TODO: get from TenantContextHolder
            );
            return ApiResponse.success(fileMetadataConvert.toDTO(metadata));
        } catch (Exception e) {
            return ApiResponse.error("Upload failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/check/{md5}")
    public ApiResponse<FileMetadataDTO> checkExist(@PathVariable String md5) {
        return instantUploadService.checkExist(md5, 1L) // TODO: get from TenantContextHolder
                .map(fileMetadataConvert::toDTO)
                .map(ApiResponse::success)
                .orElse(ApiResponse.success(null));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add scm-file/service/src/main/java/com/scmcloud/file/controller/FileController.java
git commit -m "feat(file): add REST controller for file upload"
```

---

## Task 10: Integration Test

**Files:**
- Create: `scm-file/service/src/test/java/com/scmcloud/file/integration/FileUploadIntegrationTest.java`

- [ ] **Step 1: Write integration test**

```java
package com.scmcloud.file.integration;

import com.scmcloud.file.ScmFileApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ScmFileApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileUploadIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldUploadFile() {
        // Given
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("test content".getBytes()) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        });
        body.add("bizType", "product");
        body.add("bizId", "123");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/files/upload", request, String.class);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
    
    @Test
    void shouldCheckFileExistByMd5() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/files/check/{md5}", String.class, "nonexistent");
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
```

- [ ] **Step 2: Run integration test**

```bash
mvn test -pl scm-file/service -Dtest=FileUploadIntegrationTest -f com.scm.parent/pom.xml
```

- [ ] **Step 3: Commit**

```bash
git add scm-file/service/src/test/java/com/scmcloud/file/integration/
git commit -m "test(file): add integration test for file upload"
```

---

## Summary

| Task | Description | Dependencies |
|------|-------------|--------------|
| 1 | Module structure | None |
| 2 | Database schema | Task 1 |
| 3 | Entity, Mapper and Converter | Task 2 |
| 4 | Storage SPI and Enums | Task 1 |
| 5 | MinIO implementation | Task 3, 4 |
| 6 | FileMetadata service | Task 3 |
| 7 | API interfaces (FileQuery, FileManage) | Task 1 |
| 8 | Upload service | Task 4, 5, 6 |
| 9 | REST controller | Task 6, 8 |
| 10 | Integration test | Task 9 |
