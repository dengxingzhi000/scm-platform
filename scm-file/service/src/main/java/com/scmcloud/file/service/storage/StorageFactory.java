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