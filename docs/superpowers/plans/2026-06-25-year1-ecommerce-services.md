# Year 1 E-Commerce Services Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 7 new microservices (scm-member, scm-promotion, scm-mall, scm-payment, scm-order-center, scm-fulfillment, scm-search) that form the Transaction Layer of the SCM Platform's e-commerce evolution.

**Architecture:** E-Commerce Middle Platform pattern — new services build upon existing SCM core (inventory, warehouse, purchase, supplier, logistics) without modifying them. Services communicate via Dubbo RPC (sync) and Kafka (async events).

**Tech Stack:** Java 21, Spring Boot 4.0.6, Spring Cloud Alibaba 2025, MyBatis-Plus 3.5.15, PostgreSQL, Redis, Elasticsearch, Kafka, Dubbo 3.3.6, Seata 2.0.0, Flyway 10.15.0

**Reference Spec:** `docs/superpowers/specs/2026-06-25-year1-ecommerce-services-design.md`

---

## Implementation Order

```
Phase 1 (Parallel): scm-member + scm-promotion
Phase 2 (Parallel): scm-payment + scm-search
Phase 3: scm-order-center (depends on Phase 1-2)
Phase 4: scm-mall (depends on Phase 1-3)
Phase 5: scm-fulfillment (depends on Phase 3)
```

---

## Phase 1A: scm-member (Member Center)

### Task 1: Scaffold scm-member project structure

**Files:**
- Create: `scm-member/pom.xml`
- Create: `scm-member/api/pom.xml`
- Create: `scm-member/service/pom.xml`
- Create: `scm-member/Dockerfile`

- [ ] **Step 1: Create parent pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.scmcloud</groupId>
        <artifactId>scm-platform</artifactId>
        <version>1.0.0-beta.1</version>
        <relativePath>../com.scm.parent/pom.xml</relativePath>
    </parent>

    <artifactId>scm-member</artifactId>
    <packaging>pom</packaging>
    <name>SCM Member Service</name>
    <description>Member center with user registration, profiles, points, and third-party integration</description>

    <modules>
        <module>api</module>
        <module>service</module>
    </modules>

</project>
```

- [ ] **Step 2: Create api/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.scmcloud</groupId>
        <artifactId>scm-member</artifactId>
        <version>1.0.0-beta.1</version>
    </parent>

    <artifactId>scm-member-api</artifactId>
    <packaging>jar</packaging>
    <name>SCM Member API</name>
    <description>Member service Dubbo RPC API definitions</description>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    </dependencies>

</project>
```

- [ ] **Step 3: Create service/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.scmcloud</groupId>
        <artifactId>scm-member</artifactId>
        <version>1.0.0-beta.1</version>
    </parent>

    <artifactId>scm-member-service</artifactId>
    <packaging>jar</packaging>
    <name>SCM Member Service Implementation</name>
    <description>Member service implementation with registration, profiles, and points</description>

    <dependencies>
        <dependency>
            <groupId>com.scmcloud</groupId>
            <artifactId>scm-member-api</artifactId>
            <version>1.0.0-beta.1</version>
        </dependency>
        <dependency>
            <groupId>com.scmcloud</groupId>
            <artifactId>scm-common-web</artifactId>
            <version>1.0.0-beta.1</version>
        </dependency>
        <dependency>
            <groupId>com.scmcloud</groupId>
            <artifactId>scm-common-data</artifactId>
            <version>1.0.0-beta.1</version>
        </dependency>
        <dependency>
            <groupId>com.scmcloud</groupId>
            <artifactId>scm-auth-api</artifactId>
            <version>1.0.0-beta.1</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-registry-nacos</artifactId>
        </dependency>

        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <skip>false</skip>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Create Dockerfile**

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY service/target/*.jar app.jar
EXPOSE 8302
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 5: Commit**

```bash
git add scm-member/
git commit -m "feat(member): scaffold scm-member project structure"
```

---

### Task 2: Create scm-member database schema

**Files:**
- Create: `scm-member/service/src/main/resources/db/migration/V1__create_member_tables.sql`
- Create: `scm-member/service/src/main/resources/application.yml`

- [ ] **Step 1: Create Flyway migration**

```sql
-- V1__create_member_tables.sql
-- Member core table
CREATE TABLE mem_member (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    member_no VARCHAR(32) NOT NULL UNIQUE,
    nickname VARCHAR(64),
    avatar VARCHAR(256),
    gender SMALLINT DEFAULT 0,
    birthday DATE,
    member_level INT DEFAULT 1,
    points INT DEFAULT 0,
    total_spent DECIMAL(12,2) DEFAULT 0,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Member levels configuration
CREATE TABLE mem_member_level (
    id BIGINT PRIMARY KEY,
    level_code VARCHAR(32) NOT NULL UNIQUE,
    level_name VARCHAR(64) NOT NULL,
    min_points INT NOT NULL,
    discount_rate DECIMAL(3,2) DEFAULT 1.00,
    privileges_json JSONB,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Address book
CREATE TABLE mem_member_address (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    receiver_name VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(32) NOT NULL,
    city VARCHAR(32) NOT NULL,
    district VARCHAR(32) NOT NULL,
    detail_address VARCHAR(256) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Points transaction log
CREATE TABLE mem_member_points_log (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    points INT NOT NULL,
    type VARCHAR(32) NOT NULL,
    source VARCHAR(64),
    order_no VARCHAR(64),
    description VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Third-party bindings
CREATE TABLE mem_third_party_binding (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    open_id VARCHAR(128) NOT NULL,
    union_id VARCHAR(128),
    nickname VARCHAR(64),
    avatar VARCHAR(256),
    binding_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status SMALLINT DEFAULT 1,
    UNIQUE(platform, open_id)
);

-- Member tags
CREATE TABLE mem_member_tag (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    tag_name VARCHAR(64) NOT NULL,
    tag_type VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, tag_name)
);

-- Indexes
CREATE INDEX idx_mem_member_user_id ON mem_member(user_id);
CREATE INDEX idx_mem_address_user_id ON mem_member_address(user_id);
CREATE INDEX idx_mem_points_user_id ON mem_member_points_log(user_id);
CREATE INDEX idx_mem_binding_platform ON mem_third_party_binding(platform, open_id);
CREATE INDEX idx_mem_tag_user ON mem_member_tag(user_id);

-- Seed member levels
INSERT INTO mem_member_level (id, level_code, level_name, min_points, discount_rate, privileges_json) VALUES
(1, 'NORMAL', 'Normal Member', 0, 1.00, '{"freeShipping": false}'),
(2, 'SILVER', 'Silver Member', 1000, 0.98, '{"freeShipping": false, "prioritySupport": true}'),
(3, 'GOLD', 'Gold Member', 5000, 0.95, '{"freeShipping": true, "prioritySupport": true}'),
(4, 'PLATINUM', 'Platinum Member', 20000, 0.90, '{"freeShipping": true, "prioritySupport": true, "exclusiveOffers": true}'),
(5, 'DIAMOND', 'Diamond Member', 50000, 0.85, '{"freeShipping": true, "prioritySupport": true, "exclusiveOffers": true, "personalAdvisor": true}');
```

- [ ] **Step 2: Create application.yml**

```yaml
server:
  port: 8302

spring:
  application:
    name: scm-member

  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:}
        group: DEFAULT_GROUP
      config:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:}
        group: DEFAULT_GROUP
        file-extension: yml

  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/db_user
    username: ${DB_USERNAME:admin}
    password: ${DB_PASSWORD:changeme}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    table: flyway_schema_history

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: assign_id

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

logging:
  level:
    root: INFO
    com.scmcloud.member: DEBUG
```

- [ ] **Step 3: Commit**

```bash
git add scm-member/service/src/main/resources/
git commit -m "feat(member): add database schema and application config"
```

---

### Task 3: Create scm-member domain entities

**Files:**
- Create: `scm-member/service/src/main/java/com/scmcloud/member/domain/entity/Member.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/domain/entity/MemberLevel.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/domain/entity/MemberAddress.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/domain/entity/MemberPointsLog.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/domain/entity/ThirdPartyBinding.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/domain/entity/MemberTag.java`

- [ ] **Step 1: Create Member entity**

```java
package com.scmcloud.member.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mem_member")
public class Member {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("member_no")
    private String memberNo;

    @TableField("nickname")
    private String nickname;

    @TableField("avatar")
    private String avatar;

    @TableField("gender")
    private Integer gender;

    @TableField("birthday")
    private LocalDate birthday;

    @TableField("member_level")
    private Integer memberLevel;

    @TableField("points")
    private Integer points;

    @TableField("total_spent")
    private BigDecimal totalSpent;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: Create MemberLevel entity**

```java
package com.scmcloud.member.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mem_member_level")
public class MemberLevel {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("level_code")
    private String levelCode;

    @TableField("level_name")
    private String levelName;

    @TableField("min_points")
    private Integer minPoints;

    @TableField("discount_rate")
    private BigDecimal discountRate;

    @TableField("privileges_json")
    private String privilegesJson;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: Create MemberAddress entity**

```java
package com.scmcloud.member.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mem_member_address")
public class MemberAddress {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("phone")
    private String phone;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("district")
    private String district;

    @TableField("detail_address")
    private String detailAddress;

    @TableField("is_default")
    private Boolean isDefault;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: Create MemberPointsLog entity**

```java
package com.scmcloud.member.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mem_member_points_log")
public class MemberPointsLog {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("points")
    private Integer points;

    @TableField("type")
    private String type;

    @TableField("source")
    private String source;

    @TableField("order_no")
    private String orderNo;

    @TableField("description")
    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 5: Create ThirdPartyBinding entity**

```java
package com.scmcloud.member.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mem_third_party_binding")
public class ThirdPartyBinding {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("platform")
    private String platform;

    @TableField("open_id")
    private String openId;

    @TableField("union_id")
    private String unionId;

    @TableField("nickname")
    private String nickname;

    @TableField("avatar")
    private String avatar;

    @TableField("binding_time")
    private LocalDateTime bindingTime;

    @TableField("status")
    private Integer status;
}
```

- [ ] **Step 6: Create MemberTag entity**

```java
package com.scmcloud.member.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mem_member_tag")
public class MemberTag {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("tag_name")
    private String tagName;

    @TableField("tag_type")
    private String tagType;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 7: Commit**

```bash
git add scm-member/service/src/main/java/com/scmcloud/member/domain/entity/
git commit -m "feat(member): add domain entities"
```

---

### Task 4: Create scm-member mappers

**Files:**
- Create: `scm-member/service/src/main/java/com/scmcloud/member/mapper/MemberMapper.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/mapper/MemberLevelMapper.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/mapper/MemberAddressMapper.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/mapper/MemberPointsLogMapper.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/mapper/ThirdPartyBindingMapper.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/mapper/MemberTagMapper.java`

- [ ] **Step 1: Create all mapper interfaces**

```java
package com.scmcloud.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.member.domain.entity.Member;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {
}
```

```java
package com.scmcloud.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.member.domain.entity.MemberLevel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberLevelMapper extends BaseMapper<MemberLevel> {
}
```

```java
package com.scmcloud.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.member.domain.entity.MemberAddress;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberAddressMapper extends BaseMapper<MemberAddress> {
}
```

```java
package com.scmcloud.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.member.domain.entity.MemberPointsLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberPointsLogMapper extends BaseMapper<MemberPointsLog> {
}
```

```java
package com.scmcloud.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.member.domain.entity.ThirdPartyBinding;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ThirdPartyBindingMapper extends BaseMapper<ThirdPartyBinding> {
}
```

```java
package com.scmcloud.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.member.domain.entity.MemberTag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberTagMapper extends BaseMapper<MemberTag> {
}
```

- [ ] **Step 2: Commit**

```bash
git add scm-member/service/src/main/java/com/scmcloud/member/mapper/
git commit -m "feat(member): add mapper interfaces"
```

---

### Task 5: Create scm-member service interfaces and implementations

**Files:**
- Create: `scm-member/service/src/main/java/com/scmcloud/member/service/IMemberService.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/service/IMemberAddressService.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/service/IMemberPointsService.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/service/impl/MemberServiceImpl.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/service/impl/MemberAddressServiceImpl.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/service/impl/MemberPointsServiceImpl.java`

- [ ] **Step 1: Create IMemberService interface**

```java
package com.scmcloud.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.member.domain.entity.Member;

public interface IMemberService extends IService<Member> {

    Member register(String userId, String nickname, String avatar);

    Member getByUserId(String userId);

    void updateMemberLevel(String userId, Integer level);

    void addTotalSpent(String userId, java.math.BigDecimal amount);
}
```

- [ ] **Step 2: Create IMemberAddressService interface**

```java
package com.scmcloud.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.member.domain.entity.MemberAddress;

import java.util.List;

public interface IMemberAddressService extends IService<MemberAddress> {

    List<MemberAddress> getByUserId(String userId);

    MemberAddress getDefaultAddress(String userId);

    void setDefault(String userId, Long addressId);
}
```

- [ ] **Step 3: Create IMemberPointsService interface**

```java
package com.scmcloud.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.member.domain.entity.MemberPointsLog;

import java.util.List;

public interface IMemberPointsService extends IService<MemberPointsLog> {

    void addPoints(String userId, Integer points, String source, String orderNo, String description);

    void deductPoints(String userId, Integer points, String source, String orderNo, String description);

    List<MemberPointsLog> getByUserId(String userId);

    int getPointsBalance(String userId);
}
```

- [ ] **Step 4: Create MemberServiceImpl**

```java
package com.scmcloud.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.member.domain.entity.Member;
import com.scmcloud.member.mapper.MemberMapper;
import com.scmcloud.member.service.IMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements IMemberService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Member register(String userId, String nickname, String avatar) {
        log.info("Registering member: userId={}", userId);

        Member existing = getByUserId(userId);
        if (existing != null) {
            log.warn("Member already exists: userId={}", userId);
            return existing;
        }

        Member member = new Member();
        member.setUserId(userId);
        member.setMemberNo("MEM" + System.currentTimeMillis());
        member.setNickname(nickname);
        member.setAvatar(avatar);
        member.setGender(0);
        member.setMemberLevel(1);
        member.setPoints(0);
        member.setTotalSpent(BigDecimal.ZERO);
        member.setStatus(1);
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());

        save(member);
        log.info("Member registered: userId={}, memberNo={}", userId, member.getMemberNo());
        return member;
    }

    @Override
    public Member getByUserId(String userId) {
        return getOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberLevel(String userId, Integer level) {
        Member member = getByUserId(userId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + userId);
        }
        member.setMemberLevel(level);
        member.setUpdatedAt(LocalDateTime.now());
        updateById(member);
        log.info("Member level updated: userId={}, level={}", userId, level);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTotalSpent(String userId, BigDecimal amount) {
        Member member = getByUserId(userId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + userId);
        }
        member.setTotalSpent(member.getTotalSpent().add(amount));
        member.setUpdatedAt(LocalDateTime.now());
        updateById(member);
    }
}
```

- [ ] **Step 5: Create MemberAddressServiceImpl**

```java
package com.scmcloud.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.member.domain.entity.MemberAddress;
import com.scmcloud.member.mapper.MemberAddressMapper;
import com.scmcloud.member.service.IMemberAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class MemberAddressServiceImpl extends ServiceImpl<MemberAddressMapper, MemberAddress> implements IMemberAddressService {

    @Override
    public List<MemberAddress> getByUserId(String userId) {
        return list(new LambdaQueryWrapper<MemberAddress>()
                .eq(MemberAddress::getUserId, userId)
                .orderByDesc(MemberAddress::getIsDefault)
                .orderByDesc(MemberAddress::getCreatedAt));
    }

    @Override
    public MemberAddress getDefaultAddress(String userId) {
        return getOne(new LambdaQueryWrapper<MemberAddress>()
                .eq(MemberAddress::getUserId, userId)
                .eq(MemberAddress::getIsDefault, true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(String userId, Long addressId) {
        // Clear existing default
        List<MemberAddress> addresses = getByUserId(userId);
        for (MemberAddress addr : addresses) {
            if (Boolean.TRUE.equals(addr.getIsDefault())) {
                addr.setIsDefault(false);
                updateById(addr);
            }
        }

        // Set new default
        MemberAddress address = getById(addressId);
        if (address != null && address.getUserId().equals(userId)) {
            address.setIsDefault(true);
            updateById(address);
        }
    }
}
```

- [ ] **Step 6: Create MemberPointsServiceImpl**

```java
package com.scmcloud.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.member.domain.entity.Member;
import com.scmcloud.member.domain.entity.MemberPointsLog;
import com.scmcloud.member.mapper.MemberPointsLogMapper;
import com.scmcloud.member.service.IMemberPointsService;
import com.scmcloud.member.service.IMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class MemberPointsServiceImpl extends ServiceImpl<MemberPointsLogMapper, MemberPointsLog> implements IMemberPointsService {

    private final IMemberService memberService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(String userId, Integer points, String source, String orderNo, String description) {
        log.info("Adding points: userId={}, points={}, source={}", userId, points, source);

        Member member = memberService.getByUserId(userId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + userId);
        }

        // Add points to member
        member.setPoints(member.getPoints() + points);
        member.setUpdatedAt(LocalDateTime.now());
        memberService.updateById(member);

        // Log points transaction
        MemberPointsLog logEntry = new MemberPointsLog();
        logEntry.setUserId(userId);
        logEntry.setPoints(points);
        logEntry.setType("EARN");
        logEntry.setSource(source);
        logEntry.setOrderNo(orderNo);
        logEntry.setDescription(description);
        logEntry.setCreatedAt(LocalDateTime.now());
        save(logEntry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(String userId, Integer points, String source, String orderNo, String description) {
        log.info("Deducting points: userId={}, points={}, source={}", userId, points, source);

        Member member = memberService.getByUserId(userId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + userId);
        }

        if (member.getPoints() < points) {
            throw new IllegalStateException("Insufficient points: available=" + member.getPoints() + ", required=" + points);
        }

        // Deduct points from member
        member.setPoints(member.getPoints() - points);
        member.setUpdatedAt(LocalDateTime.now());
        memberService.updateById(member);

        // Log points transaction
        MemberPointsLog logEntry = new MemberPointsLog();
        logEntry.setUserId(userId);
        logEntry.setPoints(-points);
        logEntry.setType("DEDUCT");
        logEntry.setSource(source);
        logEntry.setOrderNo(orderNo);
        logEntry.setDescription(description);
        logEntry.setCreatedAt(LocalDateTime.now());
        save(logEntry);
    }

    @Override
    public List<MemberPointsLog> getByUserId(String userId) {
        return list(new LambdaQueryWrapper<MemberPointsLog>()
                .eq(MemberPointsLog::getUserId, userId)
                .orderByDesc(MemberPointsLog::getCreatedAt));
    }

    @Override
    public int getPointsBalance(String userId) {
        Member member = memberService.getByUserId(userId);
        return member != null ? member.getPoints() : 0;
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add scm-member/service/src/main/java/com/scmcloud/member/service/
git commit -m "feat(member): add service interfaces and implementations"
```

---

### Task 6: Create scm-member controllers

**Files:**
- Create: `scm-member/service/src/main/java/com/scmcloud/member/controller/MemberController.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/controller/MemberAddressController.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/controller/MemberPointsController.java`

- [ ] **Step 1: Create MemberController**

```java
package com.scmcloud.member.controller;

import com.scmcloud.member.domain.entity.Member;
import com.scmcloud.member.service.IMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final IMemberService memberService;

    @PostMapping("/register")
    public Member register(@RequestBody RegisterRequest request) {
        log.info("[API] Register member: userId={}", request.getUserId());
        return memberService.register(request.getUserId(), request.getNickname(), request.getAvatar());
    }

    @GetMapping("/{userId}")
    public Member getByUserId(@PathVariable String userId) {
        log.info("[API] Get member: userId={}", userId);
        return memberService.getByUserId(userId);
    }

    @PutMapping("/{userId}")
    public boolean update(@PathVariable String userId, @RequestBody Member member) {
        log.info("[API] Update member: userId={}", userId);
        member.setUserId(userId);
        return memberService.updateById(member);
    }

    @PutMapping("/{userId}/level")
    public boolean updateLevel(@PathVariable String userId, @RequestParam Integer level) {
        log.info("[API] Update member level: userId={}, level={}", userId, level);
        memberService.updateMemberLevel(userId, level);
        return true;
    }

    @lombok.Data
    public static class RegisterRequest {
        private String userId;
        private String nickname;
        private String avatar;
    }
}
```

- [ ] **Step 2: Create MemberAddressController**

```java
package com.scmcloud.member.controller;

import com.scmcloud.member.domain.entity.MemberAddress;
import com.scmcloud.member.service.IMemberAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/members/{userId}/addresses")
public class MemberAddressController {

    private final IMemberAddressService addressService;

    @GetMapping
    public List<MemberAddress> list(@PathVariable String userId) {
        log.info("[API] List addresses: userId={}", userId);
        return addressService.getByUserId(userId);
    }

    @PostMapping
    public MemberAddress create(@PathVariable String userId, @RequestBody MemberAddress address) {
        log.info("[API] Create address: userId={}", userId);
        address.setUserId(userId);
        addressService.save(address);
        return address;
    }

    @PutMapping("/{addressId}")
    public boolean update(@PathVariable String userId, @PathVariable Long addressId, @RequestBody MemberAddress address) {
        log.info("[API] Update address: userId={}, addressId={}", userId, addressId);
        address.setId(addressId);
        address.setUserId(userId);
        return addressService.updateById(address);
    }

    @DeleteMapping("/{addressId}")
    public boolean delete(@PathVariable String userId, @PathVariable Long addressId) {
        log.info("[API] Delete address: userId={}, addressId={}", userId, addressId);
        return addressService.removeById(addressId);
    }

    @PutMapping("/{addressId}/default")
    public boolean setDefault(@PathVariable String userId, @PathVariable Long addressId) {
        log.info("[API] Set default address: userId={}, addressId={}", userId, addressId);
        addressService.setDefault(userId, addressId);
        return true;
    }
}
```

- [ ] **Step 3: Create MemberPointsController**

```java
package com.scmcloud.member.controller;

import com.scmcloud.member.domain.entity.MemberPointsLog;
import com.scmcloud.member.service.IMemberPointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/members/{userId}/points")
public class MemberPointsController {

    private final IMemberPointsService pointsService;

    @GetMapping
    public int getBalance(@PathVariable String userId) {
        log.info("[API] Get points balance: userId={}", userId);
        return pointsService.getPointsBalance(userId);
    }

    @GetMapping("/log")
    public List<MemberPointsLog> getLog(@PathVariable String userId) {
        log.info("[API] Get points log: userId={}", userId);
        return pointsService.getByUserId(userId);
    }

    @PostMapping("/add")
    public boolean addPoints(@PathVariable String userId, @RequestBody PointsRequest request) {
        log.info("[API] Add points: userId={}, points={}", userId, request.getPoints());
        pointsService.addPoints(userId, request.getPoints(), request.getSource(), request.getOrderNo(), request.getDescription());
        return true;
    }

    @PostMapping("/deduct")
    public boolean deductPoints(@PathVariable String userId, @RequestBody PointsRequest request) {
        log.info("[API] Deduct points: userId={}, points={}", userId, request.getPoints());
        pointsService.deductPoints(userId, request.getPoints(), request.getSource(), request.getOrderNo(), request.getDescription());
        return true;
    }

    @lombok.Data
    public static class PointsRequest {
        private Integer points;
        private String source;
        private String orderNo;
        private String description;
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add scm-member/service/src/main/java/com/scmcloud/member/controller/
git commit -m "feat(member): add REST controllers"
```

---

### Task 7: Create scm-member Dubbo interface and application class

**Files:**
- Create: `scm-member/api/src/main/java/com/scmcloud/member/api/MemberDubboService.java`
- Create: `scm-member/api/src/main/java/com/scmcloud/member/api/dto/MemberVO.java`
- Create: `scm-member/api/src/main/java/com/scmcloud/member/api/request/RegisterRequest.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/service/dubbo/MemberDubboServiceImpl.java`
- Create: `scm-member/service/src/main/java/com/scmcloud/member/MemberServiceApplication.java`

- [ ] **Step 1: Create MemberDubboService interface**

```java
package com.scmcloud.member.api;

import com.scmcloud.member.api.dto.MemberVO;
import com.scmcloud.member.api.request.RegisterRequest;

public interface MemberDubboService {

    MemberVO getMember(Long userId);

    MemberVO register(RegisterRequest request);

    void updateMemberLevel(Long userId, Integer level);

    void addPoints(Long userId, Integer points, String source);

    void deductPoints(Long userId, Integer points, String source);
}
```

- [ ] **Step 2: Create MemberVO**

```java
package com.scmcloud.member.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MemberVO {

    private Long id;
    private String userId;
    private String memberNo;
    private String nickname;
    private String avatar;
    private Integer gender;
    private LocalDate birthday;
    private Integer memberLevel;
    private Integer points;
    private BigDecimal totalSpent;
    private Integer status;
}
```

- [ ] **Step 3: Create RegisterRequest**

```java
package com.scmcloud.member.api.request;

import lombok.Data;

@Data
public class RegisterRequest {

    private String userId;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
}
```

- [ ] **Step 4: Create MemberDubboServiceImpl**

```java
package com.scmcloud.member.service.dubbo;

import com.scmcloud.member.api.MemberDubboService;
import com.scmcloud.member.api.dto.MemberVO;
import com.scmcloud.member.api.request.RegisterRequest;
import com.scmcloud.member.domain.entity.Member;
import com.scmcloud.member.service.IMemberPointsService;
import com.scmcloud.member.service.IMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

@RequiredArgsConstructor
@Slf4j
@DubboService
public class MemberDubboServiceImpl implements MemberDubboService {

    private final IMemberService memberService;
    private final IMemberPointsService pointsService;

    @Override
    public MemberVO getMember(Long userId) {
        Member member = memberService.getByUserId(String.valueOf(userId));
        if (member == null) {
            return null;
        }
        return convertToVO(member);
    }

    @Override
    public MemberVO register(RegisterRequest request) {
        Member member = memberService.register(request.getUserId(), request.getNickname(), request.getAvatar());
        return convertToVO(member);
    }

    @Override
    public void updateMemberLevel(Long userId, Integer level) {
        memberService.updateMemberLevel(String.valueOf(userId), level);
    }

    @Override
    public void addPoints(Long userId, Integer points, String source) {
        pointsService.addPoints(String.valueOf(userId), points, source, null, null);
    }

    @Override
    public void deductPoints(Long userId, Integer points, String source) {
        pointsService.deductPoints(String.valueOf(userId), points, source, null, null);
    }

    private MemberVO convertToVO(Member member) {
        MemberVO vo = new MemberVO();
        vo.setId(member.getId());
        vo.setUserId(member.getUserId());
        vo.setMemberNo(member.getMemberNo());
        vo.setNickname(member.getNickname());
        vo.setAvatar(member.getAvatar());
        vo.setGender(member.getGender());
        vo.setBirthday(member.getBirthday());
        vo.setMemberLevel(member.getMemberLevel());
        vo.setPoints(member.getPoints());
        vo.setTotalSpent(member.getTotalSpent());
        vo.setStatus(member.getStatus());
        return vo;
    }
}
```

- [ ] **Step 5: Create MemberServiceApplication**

```java
package com.scmcloud.member;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.scmcloud.member", "com.scmcloud.common"})
@EnableDiscoveryClient
@EnableDubbo
@EnableTransactionManagement
@MapperScan("com.scmcloud.member.mapper")
public class MemberServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemberServiceApplication.class, args);
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add scm-member/api/ scm-member/service/src/main/java/com/scmcloud/member/service/dubbo/ scm-member/service/src/main/java/com/scmcloud/member/MemberServiceApplication.java
git commit -m "feat(member): add Dubbo interface and application class"
```

---

### Task 8: Verify scm-member builds successfully

- [ ] **Step 1: Build scm-member**

```bash
mvn clean package -pl scm-member/service -am -f com.scm.parent/pom.xml -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Run scm-member tests**

```bash
mvn test -pl scm-member/service -f com.scm.parent/pom.xml
```

Expected: Tests pass (or no tests yet)

- [ ] **Step 3: Commit any fixes if needed**

```bash
git add -A
git commit -m "fix(member): resolve build issues"
```

---

## Phase 1B: scm-promotion (Promotion Center)

> Note: scm-promotion follows the same pattern as scm-member. Due to the large scope, I'll provide the key files and patterns. The engineer should follow the same structure as scm-member.

### Task 9: Scaffold scm-promotion project structure

**Files:**
- Create: `scm-promotion/pom.xml`
- Create: `scm-promotion/api/pom.xml`
- Create: `scm-promotion/service/pom.xml`
- Create: `scm-promotion/Dockerfile`

- [ ] **Step 1: Create parent pom.xml (same pattern as scm-member, change artifactId to scm-promotion)**

- [ ] **Step 2: Create api/pom.xml (same pattern as scm-member)**

- [ ] **Step 3: Create service/pom.xml with dependencies: scm-promotion-api, scm-common-web, scm-common-data, scm-member-api, scm-product-api, scm-decision-engine-api**

- [ ] **Step 4: Create Dockerfile (port 8303)**

- [ ] **Step 5: Commit**

```bash
git add scm-promotion/
git commit -m "feat(promotion): scaffold scm-promotion project structure"
```

---

### Task 10: Create scm-promotion database schema

**Files:**
- Create: `scm-promotion/service/src/main/resources/db/migration/V1__create_promotion_tables.sql`
- Create: `scm-promotion/service/src/main/resources/application.yml`

- [ ] **Step 1: Create Flyway migration with all tables from spec (pro_activity, pro_coupon_template, pro_coupon, pro_flash_sale, pro_group_buy, pro_group_buy_member, pro_bundle, pro_bundle_item, pro_tiered_pricing, pro_recommendation_log, pro_ab_test, pro_ab_test_assignment)**

- [ ] **Step 2: Create application.yml (port 8303, database db_promotion)**

- [ ] **Step 3: Commit**

---

### Task 11-17: Create scm-promotion entities, mappers, services, controllers, Dubbo interface

> Follow the same pattern as scm-member Tasks 3-7. Create:
> - Entities: Activity, CouponTemplate, Coupon, FlashSale, GroupBuy, Bundle, TieredPricing, RecommendationLog, AbTest
> - Mappers: One per entity
> - Services: ICouponService, IFlashSaleService, IGroupBuyService, IPromotionCalcService, IRecommendationService
> - Controllers: CouponController, FlashSaleController, GroupBuyController, RecommendationController
> - Dubbo: PromotionDubboService interface and implementation

- [ ] **Step 1-17: Create all files following scm-member patterns**

- [ ] **Step 18: Verify build**

```bash
mvn clean package -pl scm-promotion/service -am -f com.scm.parent/pom.xml -DskipTests
```

---

## Phase 2A: scm-payment (Payment Center)

### Task 18: Scaffold scm-payment project structure

**Files:**
- Create: `scm-payment/pom.xml`
- Create: `scm-payment/api/pom.xml`
- Create: `scm-payment/service/pom.xml`
- Create: `scm-payment/Dockerfile`

- [ ] **Step 1-4: Follow scm-member pattern, port 8304, dependencies: scm-order-api, scm-common-web**

- [ ] **Step 5: Commit**

---

### Task 19: Create scm-payment database schema

- [ ] **Step 1: Create migration with tables: pay_payment_order, pay_channel_config, pay_refund, pay_refund_item, pay_reconciliation, pay_reconciliation_detail, pay_payment_log**

- [ ] **Step 2: Create application.yml (port 8304, database db_finance)**

- [ ] **Step 3: Commit**

---

### Task 20-25: Create scm-payment entities, mappers, services, controllers, Dubbo interface

> Create:
> - Entities: PaymentOrder, ChannelConfig, Refund, RefundItem, Reconciliation, ReconciliationDetail, PaymentLog
> - Mappers: One per entity
> - Services: IPaymentOrderService, IRefundService, IReconciliationService, IPaymentChannelService
> - Controllers: PaymentController, RefundController, ReconciliationController
> - Dubbo: PaymentDubboService interface and implementation
> - Special: AlipayChannelAdapter, WechatChannelAdapter, StripeChannelAdapter, PayPalChannelAdapter

- [ ] **Step 1-25: Create all files**

- [ ] **Step 26: Verify build**

```bash
mvn clean package -pl scm-payment/service -am -f com.scm.parent/pom.xml -DskipTests
```

---

## Phase 2B: scm-search (Search Center)

### Task 26: Scaffold scm-search project structure

- [ ] **Step 1-4: Follow scm-member pattern, port 8307, dependencies: scm-product-api, scm-member-api, spring-boot-starter-data-elasticsearch**

- [ ] **Step 5: Commit**

---

### Task 27: Create scm-search Elasticsearch index configuration

**Files:**
- Create: `scm-search/service/src/main/resources/es/product-index.json`
- Create: `scm-search/service/src/main/java/com/scmcloud/search/config/ElasticsearchConfig.java`

- [ ] **Step 1: Create product index mapping**

- [ ] **Step 2: Create Elasticsearch configuration**

- [ ] **Step 3: Commit**

---

### Task 28-32: Create scm-search services and controllers

> Create:
> - Document: ProductDocument
> - Repository: ProductDocumentRepository
> - Services: IProductIndexService, IProductSearchService, ISearchSuggestionService
> - Controllers: SearchController, IndexController
> - Dubbo: SearchDubboService interface and implementation

- [ ] **Step 1-32: Create all files**

- [ ] **Step 33: Verify build**

```bash
mvn clean package -pl scm-search/service -am -f com.scm.parent/pom.xml -DskipTests
```

---

## Phase 3: scm-order-center (Unified Order Center)

### Task 33: Scaffold scm-order-center project structure

- [ ] **Step 1-4: Follow scm-member pattern, port 8305, dependencies: scm-order-api, scm-inventory-api, scm-promotion-api, scm-payment-api, scm-fulfillment-api**

- [ ] **Step 5: Commit**

---

### Task 34: Create scm-order-center database schema

- [ ] **Step 1: Create migration with tables: oc_order (partitioned), oc_order_item, oc_order_payment, oc_order_logistics, oc_order_state_history, oc_order_event, oc_order_split**

- [ ] **Step 2: Create application.yml (port 8305, database db_order)**

- [ ] **Step 3: Commit**

---

### Task 35-42: Create scm-order-center entities, mappers, services, controllers, state machine, saga

> Create:
> - Entities: OcOrder, OcOrderItem, OcOrderPayment, OcOrderLogistics, OcOrderStateHistory, OcOrderEvent, OcOrderSplit
> - State Machine: OrderStateMachineConfig, OrderStateMachineEngine
> - Saga: CreateOrderSaga, CreateOrderSagaHandler
> - Services: IOrderCenterService, IOrderEventService
> - Controllers: OrderCenterController
> - Dubbo: OrderCenterDubboService interface and implementation

- [ ] **Step 1-42: Create all files**

- [ ] **Step 43: Verify build**

```bash
mvn clean package -pl scm-order-center/service -am -f com.scm.parent/pom.xml -DskipTests
```

---

## Phase 4: scm-mall (E-Commerce Platform)

### Task 43: Scaffold scm-mall project structure

- [ ] **Step 1-4: Follow scm-member pattern, port 8301, dependencies: scm-product-api, scm-member-api, scm-promotion-api, scm-order-center-api, scm-search-api**

- [ ] **Step 5: Commit**

---

### Task 44: Create scm-mall database schema

- [ ] **Step 1: Create migration with tables: mall_cart, mall_seller, mall_seller_shop, mall_product_display, mall_review, mall_review_reply, mall_browsing_history, mall_favorite**

- [ ] **Step 2: Create application.yml (port 8301, database db_order)**

- [ ] **Step 3: Commit**

---

### Task 45-52: Create scm-mall entities, mappers, services, controllers, checkout flow

> Create:
> - Entities: Cart, Seller, SellerShop, ProductDisplay, Review, ReviewReply, BrowsingHistory, Favorite
> - Services: ICartService, ICheckoutService, IReviewService, IFavoriteService, ISellerService
> - Controllers: CartController, CheckoutController, ReviewController, FavoriteController, SellerController
> - Dubbo: MallDubboService interface and implementation
> - Special: CheckoutFlowService (orchestrates checkout across services)

- [ ] **Step 1-52: Create all files**

- [ ] **Step 53: Verify build**

```bash
mvn clean package -pl scm-mall/service -am -f com.scm.parent/pom.xml -DskipTests
```

---

## Phase 5: scm-fulfillment (Fulfillment Center)

### Task 53: Scaffold scm-fulfillment project structure

- [ ] **Step 1-4: Follow scm-member pattern, port 8306, dependencies: scm-order-api, scm-warehouse-api, scm-inventory-api, scm-logistics-api**

- [ ] **Step 5: Commit**

---

### Task 54: Create scm-fulfillment database schema

- [ ] **Step 1: Create migration with tables: ful_fulfillment_order, ful_fulfillment_item, ful_3pl_config, ful_3pl_order, ful_tracking, ful_dropship_config, ful_dropship_order, ful_routing_rule**

- [ ] **Step 2: Create application.yml (port 8306, database db_warehouse)**

- [ ] **Step 3: Commit**

---

### Task 55-62: Create scm-fulfillment entities, mappers, services, controllers, routing, 3PL integration

> Create:
> - Entities: FulfillmentOrder, FulfillmentItem, ThirdPartyConfig, ThirdPartyOrder, Tracking, DropshipConfig, DropshipOrder, RoutingRule
> - Services: IFulfillmentOrderService, ITrackingService, IThirdPartyService, IWarehouseRoutingService, IDropshipService
> - Controllers: FulfillmentController, TrackingController, ThirdPartyController
> - Dubbo: FulfillmentDubboService interface and implementation
> - Special: WarehouseRoutingEngine, ThirdPartyAdapter (SF, JD, ZTO adapters)

- [ ] **Step 1-62: Create all files**

- [ ] **Step 63: Verify build**

```bash
mvn clean package -pl scm-fulfillment/service -am -f com.scm.parent/pom.xml -DskipTests
```

---

## Final Verification

### Task 63: Update parent POM and verify full build

**Files:**
- Modify: `com.scm.parent/pom.xml`

- [ ] **Step 1: Add new modules to parent POM**

Add to `<modules>` section:
```xml
<!-- E-Commerce Layer -->
<module>../scm-member</module>
<module>../scm-promotion</module>
<module>../scm-mall</module>
<module>../scm-payment</module>
<module>../scm-order-center</module>
<module>../scm-fulfillment</module>
<module>../scm-search</module>
```

- [ ] **Step 2: Full build**

```bash
mvn clean install -DskipTests -f com.scm.parent/pom.xml
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Run all tests**

```bash
mvn test -f com.scm.parent/pom.xml
```

Expected: All tests pass

- [ ] **Step 4: Commit**

```bash
git add com.scm.parent/pom.xml
git commit -m "feat: add Year 1 e-commerce services to parent POM"
```

---

### Task 64: Create database initialization scripts

**Files:**
- Create: `scripts/db/microservices/028_db_member.sql`
- Create: `scripts/db/microservices/029_db_promotion.sql`
- Create: `scripts/db/microservices/030_db_payment.sql`

- [ ] **Step 1: Create db_member script (CREATE DATABASE + grant permissions)**

- [ ] **Step 2: Create db_promotion script**

- [ ] **Step 3: Create db_payment script (if not using existing db_finance)**

- [ ] **Step 4: Commit**

```bash
git add scripts/db/microservices/
git commit -m "feat: add database initialization scripts for Year 1 services"
```

---

### Task 65: Update docker-compose.yml

**Files:**
- Modify: `docker-compose.yml`

- [ ] **Step 1: Add new service definitions for scm-member, scm-promotion, scm-mall, scm-payment, scm-order-center, scm-fulfillment, scm-search**

- [ ] **Step 2: Commit**

```bash
git add docker-compose.yml
git commit -m "feat: add Year 1 services to docker-compose"
```

---

## Summary

| Phase | Services | Tasks | Key Deliverables |
|-------|----------|-------|------------------|
| 1A | scm-member | 1-8 | Member registration, profiles, points, WeChat/Alipay |
| 1B | scm-promotion | 9-17 | Coupons, flash sales, group buy, AI recommendations |
| 2A | scm-payment | 18-25 | Alipay, WeChat Pay, Stripe, PayPal, reconciliation |
| 2B | scm-search | 26-32 | Elasticsearch, NLP search, image search, voice search |
| 3 | scm-order-center | 33-42 | Unified orders, state machine, saga, event sourcing |
| 4 | scm-mall | 43-52 | B2C frontend, marketplace, cart, checkout, reviews |
| 5 | scm-fulfillment | 53-62 | Fulfillment orchestration, 3PL, dropshipping, routing |
| Final | Integration | 63-65 | Parent POM, DB scripts, docker-compose |

**Total Tasks:** 65  
**Estimated Effort:** 24 weeks with 6 engineers
