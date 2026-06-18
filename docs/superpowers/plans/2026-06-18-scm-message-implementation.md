# scm-message Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build unified event bus service with Kafka-based domain event publishing, outbox pattern for transactional events, and dead letter queue for failed events.

**Architecture:** New Spring Boot module using Kafka for event streaming, outbox pattern for reliable event delivery, PostgreSQL for event outbox table.

**Tech Stack:** Spring Boot 4.x, Apache Kafka, MyBatis-Plus, Dubbo 3.x

---

## File Structure

```
scm-message/
├── pom.xml
├── api/
│   ├── pom.xml
│   └── src/main/java/com/scmcloud/message/api/
│       ├── EventPublisherApi.java
│       ├── EventConsumerApi.java
│       ├── dto/
│       │   ├── DomainEventDTO.java
│       │   └── EventOutboxDTO.java
│       └── enums/
│           └── OutboxStatus.java
├── service/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/scmcloud/message/
│       │   │   ├── ScmMessageApplication.java
│       │   │   ├── config/
│       │   │   │   ├── KafkaConfig.java
│       │   │   │   └── ProducerConfig.java
│       │   │   ├── producer/
│       │   │   │   ├── KafkaEventProducer.java
│       │   │   │   └── OutboxService.java
│       │   │   ├── consumer/
│       │   │   │   ├── ConsumerRegistry.java
│       │   │   │   └── EventConsumer.java
│       │   │   ├── deadletter/
│       │   │   │   ├── DlqService.java
│       │   │   │   └── DlqRetryScheduler.java
│       │   │   ├── entity/
│       │   │   │   └── EventOutbox.java
│       │   │   ├── event/
│       │   │   │   ├── DomainEvent.java
│       │   │   │   ├── OrderCreatedEvent.java
│       │   │   │   ├── InventoryChangedEvent.java
│       │   │   │   └── PriceChangedEvent.java
│       │   │   └── mapper/
│       │   │       └── EventOutboxMapper.java
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       │           └── V1_0_0__create_message_tables.sql
│       └── test/
│           └── java/com/scmcloud/message/
│               ├── producer/
│               │   └── OutboxServiceTest.java
│               └── consumer/
│                   └── ConsumerRegistryTest.java
```

---

## Task 1: Create Module Structure

**Files:**
- Create: `scm-message/pom.xml`
- Create: `scm-message/api/pom.xml`
- Create: `scm-message/service/pom.xml`
- Modify: `com.scm.parent/pom.xml`

- [ ] **Step 1: Create parent POM for scm-message**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.scmcloud</groupId>
    <artifactId>scm-message</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>SCM Message Service</name>
    <description>Unified event bus service</description>

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
        <artifactId>scm-message</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>scm-message-api</artifactId>
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
        <artifactId>scm-message</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>scm-message-service</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.scmcloud</groupId>
            <artifactId>scm-message-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.scmcloud</groupId>
            <artifactId>scm-common-data</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Add scm-message to parent POM**

Add to `com.scm.parent/pom.xml` in `<modules>`:
```xml
<module>../scm-message</module>
```

- [ ] **Step 5: Verify build**

```bash
mvn clean install -f scm-message/pom.xml -DskipTests
```

- [ ] **Step 6: Commit**

```bash
git add scm-message/ com.scm.parent/pom.xml
git commit -m "feat(message): create scm-message module structure"
```

---

## Task 2: Create Database Schema

**Files:**
- Create: `scm-message/service/src/main/resources/db/migration/V1_0_0__create_message_tables.sql`

- [ ] **Step 1: Write migration SQL**

```sql
-- Event outbox table for transactional event publishing
CREATE TABLE sys_event_outbox (
    id              VARCHAR(36) PRIMARY KEY,
    event_type      VARCHAR(100) NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(36) NOT NULL,
    payload         TEXT NOT NULL,
    retry_count     INTEGER DEFAULT 0,
    max_retries     INTEGER DEFAULT 3,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message   TEXT,
    tenant_id       BIGINT NOT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMP,
    next_retry_at   TIMESTAMP
);

CREATE INDEX idx_outbox_status ON sys_event_outbox(status);
CREATE INDEX idx_outbox_next_retry ON sys_event_outbox(next_retry_at);
CREATE INDEX idx_outbox_aggregate ON sys_event_outbox(aggregate_type, aggregate_id);

-- Dead letter queue table
CREATE TABLE sys_event_dlq (
    id              VARCHAR(36) PRIMARY KEY,
    original_event_id VARCHAR(36) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(36) NOT NULL,
    payload         TEXT NOT NULL,
    error_message   TEXT,
    error_stack     TEXT,
    retry_count     INTEGER DEFAULT 0,
    tenant_id       BIGINT NOT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved        BOOLEAN DEFAULT FALSE,
    resolved_at     TIMESTAMP
);

CREATE INDEX idx_dlq_event_type ON sys_event_dlq(event_type);
CREATE INDEX idx_dlq_resolved ON sys_event_dlq(resolved);
```

- [ ] **Step 2: Commit**

```bash
git add scm-message/service/src/main/resources/db/migration/
git commit -m "feat(message): add database schema for event outbox and DLQ"
```

---

## Task 3: Create Domain Event Interface and Implementations

**Files:**
- Create: `scm-message/api/src/main/java/com/scmcloud/message/api/dto/DomainEventDTO.java`
- Create: `scm-message/service/src/main/java/com/scmcloud/message/event/DomainEvent.java`
- Create: `scm-message/service/src/main/java/com/scmcloud/message/event/OrderCreatedEvent.java`
- Create: `scm-message/service/src/main/java/com/scmcloud/message/event/InventoryChangedEvent.java`
- Create: `scm-message/service/src/main/java/com/scmcloud/message/event/PriceChangedEvent.java`

- [ ] **Step 1: Create DomainEventDTO**

```java
package com.scmcloud.message.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
public class DomainEventDTO implements Serializable {
    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private Long tenantId;
    private Date timestamp;
    private Map<String, Object> payload;
}
```

- [ ] **Step 2: Create DomainEvent interface**

```java
package com.scmcloud.message.event;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public interface DomainEvent {
    String getEventId();
    String getEventType();
    String getAggregateType();
    String getAggregateId();
    Long getTenantId();
    Date getTimestamp();
    Map<String, Object> getPayload();
    
    default String generateEventId() {
        return UUID.randomUUID().toString();
    }
}
```

- [ ] **Step 3: Create OrderCreatedEvent**

```java
package com.scmcloud.message.event;

import lombok.Builder;
import lombok.Data;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class OrderCreatedEvent implements DomainEvent {
    private String eventId;
    private String orderId;
    private String orderNo;
    private Long tenantId;
    private Date timestamp;
    
    @Override
    public String getEventType() {
        return "ORDER_CREATED";
    }
    
    @Override
    public String getAggregateType() {
        return "Order";
    }
    
    @Override
    public String getAggregateId() {
        return orderId;
    }
    
    @Override
    public Map<String, Object> getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("orderNo", orderNo);
        return payload;
    }
    
    public static OrderCreatedEvent of(String orderId, String orderNo, Long tenantId) {
        return OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .orderNo(orderNo)
                .tenantId(tenantId)
                .timestamp(new Date())
                .build();
    }
}
```

- [ ] **Step 4: Create InventoryChangedEvent**

```java
package com.scmcloud.message.event;

import lombok.Builder;
import lombok.Data;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class InventoryChangedEvent implements DomainEvent {
    private String eventId;
    private String skuId;
    private String warehouseId;
    private Integer quantityChange;
    private Integer newQuantity;
    private String changeType;
    private Long tenantId;
    private Date timestamp;
    
    @Override
    public String getEventType() {
        return "INVENTORY_CHANGED";
    }
    
    @Override
    public String getAggregateType() {
        return "Inventory";
    }
    
    @Override
    public String getAggregateId() {
        return skuId + ":" + warehouseId;
    }
    
    @Override
    public Map<String, Object> getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("skuId", skuId);
        payload.put("warehouseId", warehouseId);
        payload.put("quantityChange", quantityChange);
        payload.put("newQuantity", newQuantity);
        payload.put("changeType", changeType);
        return payload;
    }
    
    public static InventoryChangedEvent of(String skuId, String warehouseId, 
                                            Integer quantityChange, Integer newQuantity,
                                            String changeType, Long tenantId) {
        return InventoryChangedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .skuId(skuId)
                .warehouseId(warehouseId)
                .quantityChange(quantityChange)
                .newQuantity(newQuantity)
                .changeType(changeType)
                .tenantId(tenantId)
                .timestamp(new Date())
                .build();
    }
}
```

- [ ] **Step 5: Create PriceChangedEvent**

```java
package com.scmcloud.message.event;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class PriceChangedEvent implements DomainEvent {
    private String eventId;
    private String productId;
    private String skuId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String priceListId;
    private Long tenantId;
    private Date timestamp;
    
    @Override
    public String getEventType() {
        return "PRICE_CHANGED";
    }
    
    @Override
    public String getAggregateType() {
        return "Price";
    }
    
    @Override
    public String getAggregateId() {
        return productId + ":" + skuId;
    }
    
    @Override
    public Map<String, Object> getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", productId);
        payload.put("skuId", skuId);
        payload.put("oldPrice", oldPrice);
        payload.put("newPrice", newPrice);
        payload.put("priceListId", priceListId);
        return payload;
    }
    
    public static PriceChangedEvent of(String productId, String skuId, 
                                        BigDecimal oldPrice, BigDecimal newPrice,
                                        String priceListId, Long tenantId) {
        return PriceChangedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .productId(productId)
                .skuId(skuId)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .priceListId(priceListId)
                .tenantId(tenantId)
                .timestamp(new Date())
                .build();
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add scm-message/api/src/main/java/com/scmcloud/message/api/dto/
git add scm-message/service/src/main/java/com/scmcloud/message/event/
git commit -m "feat(message): add domain event interface and implementations"
```

---

## Task 4: Create Outbox Entity and Mapper

**Files:**
- Create: `scm-message/service/src/main/java/com/scmcloud/message/entity/EventOutbox.java`
- Create: `scm-message/service/src/main/java/com/scmcloud/message/mapper/EventOutboxMapper.java`
- Create: `scm-message/api/src/main/java/com/scmcloud/message/api/enums/OutboxStatus.java`

- [ ] **Step 1: Create OutboxStatus enum**

```java
package com.scmcloud.message.api.enums;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    RETRYING
}
```

- [ ] **Step 2: Create EventOutbox entity**

```java
package com.scmcloud.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_event_outbox")
public class EventOutbox {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String payload;
    private Integer retryCount;
    private Integer maxRetries;
    private String status;
    private String errorMessage;
    private Long tenantId;
    private Date createTime;
    private Date publishedAt;
    private Date nextRetryAt;
}
```

- [ ] **Step 3: Create EventOutboxMapper**

```java
package com.scmcloud.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.message.entity.EventOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.Date;
import java.util.List;

@Mapper
public interface EventOutboxMapper extends BaseMapper<EventOutbox> {
    
    @Select("SELECT * FROM sys_event_outbox WHERE status = 'PENDING' AND next_retry_at <= #{now} LIMIT #{limit}")
    List<EventOutbox> findPendingEvents(@Param("now") Date now, @Param("limit") int limit);
    
    @Select("SELECT * FROM sys_event_outbox WHERE status = 'RETRYING' AND next_retry_at <= #{now} LIMIT #{limit}")
    List<EventOutbox> findRetryingEvents(@Param("now") Date now, @Param("limit") int limit);
}
```

- [ ] **Step 4: Commit**

```bash
git add scm-message/api/src/main/java/com/scmcloud/message/api/enums/
git add scm-message/service/src/main/java/com/scmcloud/message/entity/
git add scm-message/service/src/main/java/com/scmcloud/message/mapper/
git commit -m "feat(message): add outbox entity and mapper"
```

---

## Task 5: Create Outbox Service

**Files:**
- Create: `scm-message/service/src/main/java/com/scmcloud/message/producer/OutboxService.java`

- [ ] **Step 1: Write test for OutboxService**

```java
package com.scmcloud.message.producer;

import com.scmcloud.message.entity.EventOutbox;
import com.scmcloud.message.event.DomainEvent;
import com.scmcloud.message.event.OrderCreatedEvent;
import com.scmcloud.message.mapper.EventOutboxMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {
    
    @Mock
    private EventOutboxMapper mapper;
    
    @InjectMocks
    private OutboxService outboxService;
    
    @Test
    void shouldSaveEventToOutbox() {
        // Given
        DomainEvent event = OrderCreatedEvent.of("order-1", "ORD001", 1L);
        when(mapper.insert(any())).thenReturn(1);
        
        // When
        EventOutbox result = outboxService.save(event);
        
        // Then
        assertNotNull(result);
        assertEquals("ORDER_CREATED", result.getEventType());
        assertEquals("PENDING", result.getStatus());
        verify(mapper).insert(any());
    }
    
    @Test
    void shouldMarkAsPublished() {
        // Given
        String eventId = "event-1";
        EventOutbox outbox = new EventOutbox();
        outbox.setId(eventId);
        outbox.setStatus("PENDING");
        when(mapper.selectById(eventId)).thenReturn(outbox);
        when(mapper.updateById(any())).thenReturn(1);
        
        // When
        outboxService.markAsPublished(eventId);
        
        // Then
        assertEquals("PUBLISHED", outbox.getStatus());
        assertNotNull(outbox.getPublishedAt());
        verify(mapper).updateById(outbox);
    }
}
```

- [ ] **Step 2: Implement OutboxService**

```java
package com.scmcloud.message.producer;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.message.entity.EventOutbox;
import com.scmcloud.message.event.DomainEvent;
import com.scmcloud.message.mapper.EventOutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class OutboxService extends ServiceImpl<EventOutboxMapper, EventOutbox> {
    
    private final ObjectMapper objectMapper;
    
    public OutboxService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public EventOutbox save(DomainEvent event) {
        try {
            EventOutbox outbox = new EventOutbox();
            outbox.setId(event.getEventId());
            outbox.setEventType(event.getEventType());
            outbox.setAggregateType(event.getAggregateType());
            outbox.setAggregateId(event.getAggregateId());
            outbox.setPayload(objectMapper.writeValueAsString(event.getPayload()));
            outbox.setRetryCount(0);
            outbox.setMaxRetries(3);
            outbox.setStatus("PENDING");
            outbox.setTenantId(event.getTenantId());
            outbox.setCreateTime(new Date());
            outbox.setNextRetryAt(new Date());
            
            save(outbox);
            return outbox;
        } catch (Exception e) {
            log.error("Failed to save event to outbox", e);
            throw new RuntimeException("Failed to save event", e);
        }
    }
    
    public void markAsPublished(String eventId) {
        EventOutbox outbox = getById(eventId);
        if (outbox != null) {
            outbox.setStatus("PUBLISHED");
            outbox.setPublishedAt(new Date());
            updateById(outbox);
        }
    }
    
    public void markAsFailed(String eventId, String errorMessage) {
        EventOutbox outbox = getById(eventId);
        if (outbox != null) {
            outbox.setRetryCount(outbox.getRetryCount() + 1);
            outbox.setErrorMessage(errorMessage);
            
            if (outbox.getRetryCount() >= outbox.getMaxRetries()) {
                outbox.setStatus("FAILED");
            } else {
                outbox.setStatus("RETRYING");
                // Exponential backoff: 1min, 5min, 15min
                long[] backoff = {60000, 300000, 900000};
                long delay = backoff[Math.min(outbox.getRetryCount(), backoff.length - 1)];
                outbox.setNextRetryAt(new Date(System.currentTimeMillis() + delay));
            }
            
            updateById(outbox);
        }
    }
    
    public List<EventOutbox> findPendingEvents(int limit) {
        return baseMapper.findPendingEvents(new Date(), limit);
    }
    
    public List<EventOutbox> findRetryingEvents(int limit) {
        return baseMapper.findRetryingEvents(new Date(), limit);
    }
}
```

- [ ] **Step 3: Run test**

```bash
mvn test -pl scm-message/service -Dtest=OutboxServiceTest -f com.scm.parent/pom.xml
```

- [ ] **Step 4: Commit**

```bash
git add scm-message/service/src/main/java/com/scmcloud/message/producer/OutboxService.java
git commit -m "feat(message): implement outbox service"
```

---

## Task 6: Create Kafka Event Producer

**Files:**
- Create: `scm-message/service/src/main/java/com/scmcloud/message/producer/KafkaEventProducer.java`
- Create: `scm-message/service/src/main/java/com/scmcloud/message/config/KafkaConfig.java`

- [ ] **Step 1: Create KafkaConfig**

```java
package com.scmcloud.message.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

- [ ] **Step 2: Create KafkaEventProducer**

```java
package com.scmcloud.message.producer;

import com.scmcloud.message.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String TOPIC_PREFIX = "scm.";
    
    public CompletableFuture<SendResult<String, String>> send(DomainEvent event) {
        String topic = TOPIC_PREFIX + event.getAggregateType().toLowerCase() + ".events";
        String key = event.getAggregateId();
        
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(topic, key, payload);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Event sent successfully: topic={}, key={}, eventType={}", 
                            topic, key, event.getEventType());
                } else {
                    log.error("Failed to send event: topic={}, key={}, eventType={}", 
                            topic, key, event.getEventType(), ex);
                }
            });
            
            return future;
        } catch (Exception e) {
            log.error("Failed to serialize event", e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add scm-message/service/src/main/java/com/scmcloud/message/producer/KafkaEventProducer.java
git add scm-message/service/src/main/java/com/scmcloud/message/config/KafkaConfig.java
git commit -m "feat(message): add Kafka event producer"
```

---

## Task 7: Create Outbox Publisher Scheduler

**Files:**
- Create: `scm-message/service/src/main/java/com/scmcloud/message/producer/OutboxPublisher.java`

- [ ] **Step 1: Implement OutboxPublisher**

```java
package com.scmcloud.message.producer;

import com.scmcloud.message.entity.EventOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    
    private final OutboxService outboxService;
    private final KafkaEventProducer kafkaEventProducer;
    
    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    public void publishPendingEvents() {
        List<EventOutbox> pendingEvents = outboxService.findPendingEvents(100);
        
        for (EventOutbox event : pendingEvents) {
            publishEvent(event);
        }
    }
    
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void publishRetryingEvents() {
        List<EventOutbox> retryingEvents = outboxService.findRetryingEvents(50);
        
        for (EventOutbox event : retryingEvents) {
            publishEvent(event);
        }
    }
    
    private void publishEvent(EventOutbox event) {
        try {
            // Convert outbox to domain event
            DomainEvent domainEvent = convertToDomainEvent(event);
            
            // Send to Kafka
            kafkaEventProducer.send(domainEvent)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            outboxService.markAsPublished(event.getId());
                        } else {
                            outboxService.markAsFailed(event.getId(), ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getId(), e);
            outboxService.markAsFailed(event.getId(), e.getMessage());
        }
    }
    
    private DomainEvent convertToDomainEvent(EventOutbox outbox) {
        // Simple conversion - in production, use event registry
        return new DomainEvent() {
            @Override
            public String getEventId() { return outbox.getId(); }
            @Override
            public String getEventType() { return outbox.getEventType(); }
            @Override
            public String getAggregateType() { return outbox.getAggregateType(); }
            @Override
            public String getAggregateId() { return outbox.getAggregateId(); }
            @Override
            public Long getTenantId() { return outbox.getTenantId(); }
            @Override
            public java.util.Date getTimestamp() { return outbox.getCreateTime(); }
            @Override
            public java.util.Map<String, Object> getPayload() { return java.util.Collections.emptyMap(); }
        };
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add scm-message/service/src/main/java/com/scmcloud/message/producer/OutboxPublisher.java
git commit -m "feat(message): add outbox publisher scheduler"
```

---

## Task 8: Create API Interface

**Files:**
- Create: `scm-message/api/src/main/java/com/scmcloud/message/api/EventPublisherApi.java`

- [ ] **Step 1: Create EventPublisherApi**

```java
package com.scmcloud.message.api;

import com.scmcloud.message.api.dto.DomainEventDTO;

public interface EventPublisherApi {
    
    /**
     * Publish event to outbox (transactional)
     */
    void publish(DomainEventDTO event);
    
    /**
     * Publish event directly to Kafka (non-transactional)
     */
    void publishDirect(DomainEventDTO event);
}
```

- [ ] **Step 2: Commit**

```bash
git add scm-message/api/src/main/java/com/scmcloud/message/api/EventPublisherApi.java
git commit -m "feat(message): add EventPublisher API interface"
```

---

## Task 9: Create Application Entry Point

**Files:**
- Create: `scm-message/service/src/main/java/com/scmcloud/message/ScmMessageApplication.java`
- Create: `scm-message/service/src/main/resources/application.yml`

- [ ] **Step 1: Create ScmMessageApplication**

```java
package com.scmcloud.message;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScmMessageApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ScmMessageApplication.class, args);
    }
}
```

- [ ] **Step 2: Create application.yml**

```yaml
server:
  port: 8209

spring:
  application:
    name: scm-message
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/db_system
    username: ${DB_USERNAME:admin}
    password: ${DB_PASSWORD:admin123}
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    com.scmcloud.message: DEBUG
```

- [ ] **Step 3: Commit**

```bash
git add scm-message/service/src/main/java/com/scmcloud/message/ScmMessageApplication.java
git add scm-message/service/src/main/resources/application.yml
git commit -m "feat(message): add application entry point and config"
```

---

## Task 10: Integration Test

**Files:**
- Create: `scm-message/service/src/test/java/com/scmcloud/message/integration/MessageIntegrationTest.java`

- [ ] **Step 1: Write integration test**

```java
package com.scmcloud.message.integration;

import com.scmcloud.message.ScmMessageApplication;
import com.scmcloud.message.event.DomainEvent;
import com.scmcloud.message.event.OrderCreatedEvent;
import com.scmcloud.message.producer.OutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ScmMessageApplication.class)
class MessageIntegrationTest {
    
    @Autowired
    private OutboxService outboxService;
    
    @Test
    void shouldSaveEventToOutbox() {
        // Given
        DomainEvent event = OrderCreatedEvent.of("order-1", "ORD001", 1L);
        
        // When
        var result = outboxService.save(event);
        
        // Then
        assertNotNull(result);
        assertEquals("ORDER_CREATED", result.getEventType());
        assertEquals("PENDING", result.getStatus());
    }
}
```

- [ ] **Step 2: Run integration test**

```bash
mvn test -pl scm-message/service -Dtest=MessageIntegrationTest -f com.scm.parent/pom.xml
```

- [ ] **Step 3: Commit**

```bash
git add scm-message/service/src/test/java/com/scmcloud/message/integration/
git commit -m "test(message): add integration test for message service"
```

---

## Summary

| Task | Description | Dependencies |
|------|-------------|--------------|
| 1 | Module structure | None |
| 2 | Database schema | Task 1 |
| 3 | Domain events | Task 1 |
| 4 | Outbox entity/mapper | Task 2 |
| 5 | Outbox service | Task 4 |
| 6 | Kafka producer | Task 3 |
| 7 | Outbox publisher | Task 5, 6 |
| 8 | API interface | Task 1 |
| 9 | Application entry | Task 7 |
| 10 | Integration test | Task 9 |
