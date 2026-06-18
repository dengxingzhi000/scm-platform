# Storage SPI and Enums Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the storage abstraction layer with SPI pattern for the scm-file module, enabling multiple storage backends (MinIO, OSS, S3).

**Architecture:** Implement a factory pattern with StorageEngine SPI interface. Each storage backend implements the interface and declares its supported StorageType. The StorageFactory manages engine registration and retrieval.

**Tech Stack:** Java, Spring Boot, Lombok, JUnit 5, Mockito

---

## File Structure

- `scm-file/api/src/main/java/com/scmcloud/file/api/enums/StorageType.java` - Storage type enum
- `scm-file/service/src/main/java/com/scmcloud/file/service/storage/StorageEngine.java` - Storage SPI interface
- `scm-file/service/src/main/java/com/scmcloud/file/service/storage/StorageFactory.java` - Factory for managing engines
- `scm-file/service/src/test/java/com/scmcloud/file/service/storage/StorageFactoryTest.java` - Unit tests

---

### Task 1: Create StorageType Enum

**Files:**
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

- [ ] **Step 2: Verify file created**

Run: `ls scm-file/api/src/main/java/com/scmcloud/file/api/enums/`
Expected: StorageType.java exists

---

### Task 2: Create StorageEngine Interface

**Files:**
- Create: `scm-file/service/src/main/java/com/scmcloud/file/service/storage/StorageEngine.java`

- [ ] **Step 1: Create StorageEngine interface**

```java
package com.scmcloud.file.service.storage;

import com.scmcloud.file.api.enums.StorageType;
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

- [ ] **Step 2: Verify file created**

Run: `ls scm-file/service/src/main/java/com/scmcloud/file/service/storage/`
Expected: StorageEngine.java exists

---

### Task 3: Create StorageFactory

**Files:**
- Create: `scm-file/service/src/main/java/com/scmcloud/file/service/storage/StorageFactory.java`

- [ ] **Step 1: Create StorageFactory class**

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

- [ ] **Step 2: Verify file created**

Run: `ls scm-file/service/src/main/java/com/scmcloud/file/service/storage/`
Expected: StorageFactory.java exists

---

### Task 4: Write and Run StorageFactory Test

**Files:**
- Create: `scm-file/service/src/test/java/com/scmcloud/file/service/storage/StorageFactoryTest.java`

- [ ] **Step 1: Create test file**

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

- [ ] **Step 2: Run test**

Run: `mvn test -pl scm-file/service -Dtest=StorageFactoryTest -f com.scm.parent/pom.xml`
Expected: Tests pass

- [ ] **Step 3: Commit**

```bash
git add scm-file/service/src/main/java/com/scmcloud/file/service/storage/
git add scm-file/api/src/main/java/com/scmcloud/file/api/enums/StorageType.java
git commit -m "feat(file): add storage SPI interface and factory"
```

---

## Verification

After completing all tasks:
1. Verify all files exist in correct locations
2. Run tests to ensure they pass
3. Check that StorageEngine interface uses byte[] for uploads (not InputStream)
4. Verify StorageFactory uses EnumMap for type-safe storage
5. Confirm test coverage for both successful and error cases