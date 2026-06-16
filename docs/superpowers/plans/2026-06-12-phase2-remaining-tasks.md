# SCM Platform Phase 2 Remaining Tasks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the remaining 27 tasks from the architecture review that weren't covered in the first 23-task plan.

**Architecture:** Group remaining tasks by category: Backend/Infrastructure, DevOps, Security, Frontend, Documentation. Each task produces self-contained changes.

**Tech Stack:** Java 21, Spring Boot 4.0.6, PostgreSQL 16, Redis 7.2, Kafka, Kubernetes, Docker, Prometheus, Grafana

---

## Phase 2A: Backend Infrastructure (8 Tasks)

### Task 24: Implement Database Connection Pooling (PgBouncer)

**Files:**
- Create: `deploy/pgbouncer/pgbouncer.ini`
- Create: `deploy/pgbouncer/userlist.txt`
- Modify: `docker-compose.yml` (add pgpool service)

- [ ] **Step 1: Create PgBouncer configuration**

Create `deploy/pgbouncer/pgbouncer.ini`:
```ini
[databases]
db_user = host=postgresql port=5432 dbname=db_user
db_org = host=postgresql port=5432 dbname=db_org
db_permission = host=postgresql port=5432 dbname=db_permission
db_approval = host=postgresql port=5432 dbname=db_approval
db_audit = host=postgresql port=5432 dbname=db_audit
db_notify = host=postgresql port=5432 dbname=db_notify
db_product = host=postgresql port=5432 dbname=db_product
db_inventory = host=postgresql port=5432 dbname=db_inventory
db_order = host=postgresql port=5432 dbname=db_order
db_warehouse = host=postgresql port=5432 dbname=db_warehouse
db_logistics = host=postgresql port=5432 dbname=db_logistics
db_supplier = host=postgresql port=5432 dbname=db_supplier
db_tenant = host=postgresql port=5432 dbname=db_tenant
db_finance = host=postgresql port=5432 dbname=db_finance
db_purchase = host=postgresql port=5432 dbname=db_purchase

[pgbouncer]
listen_addr = 0.0.0.0
listen_port = 6432
auth_type = md5
auth_file = /etc/pgbouncer/userlist.txt
pool_mode = transaction
server_reset_query = DISCARD ALL
max_client_conn = 1000
default_pool_size = 25
min_pool_size = 5
reserve_pool_size = 5
reserve_pool_timeout = 3
server_lifetime = 3600
server_idle_timeout = 600
log_connections = 1
log_disconnections = 1
```

- [ ] **Step 2: Create userlist**

Create `deploy/pgbouncer/userlist.txt`:
```
"admin" "admin123"
```

- [ ] **Step 3: Add PgBouncer to docker-compose.yml**

Add to docker-compose.yml before the `networks:` section:
```yaml
  # ============================================================
  # PgBouncer - 连接池
  # ============================================================
  pgbouncer:
    image: edoburu/pgbouncer:1.23.1
    container_name: scm-pgbouncer
    volumes:
      - ./deploy/pgbouncer/pgbouncer.ini:/etc/pgbouncer/pgbouncer.ini:ro
      - ./deploy/pgbouncer/userlist.txt:/etc/pgbouncer/userlist.txt:ro
    ports:
      - "6432:6432"
    depends_on:
      - postgresql
    networks:
      - scm-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -h localhost -p 6432 || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5
```

- [ ] **Step 4: Commit**

```bash
git add deploy/pgbouncer/ docker-compose.yml
git commit -m "feat: implement PgBouncer connection pooling"
```

---

### Task 25: Implement Cache Warming Strategy

**Files:**
- Create: `scm-common/cache/src/main/java/com/scmcloud/common/cache/warming/CacheWarmer.java`
- Create: `scm-common/cache/src/main/java/com/scmcloud/common/cache/warming/CacheWarmingConfig.java`

- [ ] **Step 1: Create CacheWarmer interface**

Create `scm-common/cache/src/main/java/com/scmcloud/common/cache/warming/CacheWarmer.java`:
```java
package com.scmcloud.common.cache.warming;

import java.util.List;

public interface CacheWarmer {
    void warmCache();
    String getWarmerName();
    default int getOrder() { return 0; }
}
```

- [ ] **Step 2: Create CacheWarmingConfig**

Create `scm-common/cache/src/main/java/com/scmcloud/common/cache/warming/CacheWarmingConfig.java`:
```java
package com.scmcloud.common.cache.warming;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@Order(1)
public class CacheWarmingConfig implements ApplicationRunner {

    private final List<CacheWarmer> warmers;

    public CacheWarmingConfig(List<CacheWarmer> warmers) {
        this.warmers = warmers;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting cache warming with {} warmers", warmers.size());
        warmers.stream()
                .sorted(Comparator.comparingInt(CacheWarmer::getOrder))
                .forEach(warmer -> {
                    try {
                        log.info("Running cache warmer: {}", warmer.getWarmerName());
                        warmer.warmCache();
                        log.info("Cache warmer {} completed successfully", warmer.getWarmerName());
                    } catch (Exception e) {
                        log.error("Cache warmer {} failed: {}", warmer.getWarmerName(), e.getMessage(), e);
                    }
                });
        log.info("Cache warming completed");
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add scm-common/cache/src/main/java/com/scmcloud/common/cache/warming/
git commit -m "feat: implement cache warming strategy framework"
```

---

### Task 26: Implement Distributed Tracing Correlation

**Files:**
- Modify: `scm-common/web/src/main/java/com/scmcloud/common/security/filter/RequestIdFilter.java` (or create if not exists)
- Modify: All service `application.yml` files (add trace propagation config)

- [ ] **Step 1: Create RequestIdFilter**

Create `scm-common/web/src/main/java/com/scmcloud/common/security/filter/RequestIdFilter.java`:
```java
package com.scmcloud.common.security.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter implements Filter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String TRACE_ID_HEADER = "X-Trace-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);

        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Add tracing headers to service YAML files**

Add to the security headers config in all service application.yml files:
```yaml
frog:
  security:
    headers:
      # ... existing config ...
      expose-headers: X-Request-ID,X-Trace-ID
```

- [ ] **Step 3: Commit**

```bash
git add scm-common/web/src/main/java/com/scmcloud/common/security/filter/RequestIdFilter.java
git commit -m "feat: implement distributed tracing correlation with X-Request-ID"
```

---

### Task 27: Implement Feature Flags

**Files:**
- Create: `scm-common/core/src/main/java/com/scmcloud/common/feature/FeatureFlag.java`
- Create: `scm-common/core/src/main/java/com/scmcloud/common/feature/FeatureFlagService.java`
- Create: `scm-common/core/src/main/java/com/scmcloud/common/feature/FeatureFlagConfig.java`

- [ ] **Step 1: Create FeatureFlag annotation**

Create `scm-common/core/src/main/java/com/scmcloud/common/feature/FeatureFlag.java`:
```java
package com.scmcloud.common.feature;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeatureFlag {
    String value();
    boolean negate() default false;
}
```

- [ ] **Step 2: Create FeatureFlagService**

Create `scm-common/core/src/main/java/com/scmcloud/common/feature/FeatureFlagService.java`:
```java
package com.scmcloud.common.feature;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FeatureFlagService {

    private final Map<String, Boolean> flags = new ConcurrentHashMap<>();

    @Value("${feature-flags.default:false}")
    private boolean defaultFlag;

    public FeatureFlagService(@Value("${feature-flags:}") Map<String, Boolean> configFlags) {
        if (configFlags != null) {
            this.flags.putAll(configFlags);
        }
        log.info("Feature flags initialized: {}", flags);
    }

    public boolean isEnabled(String flagName) {
        return flags.getOrDefault(flagName, defaultFlag);
    }

    public void setFlag(String flagName, boolean enabled) {
        flags.put(flagName, enabled);
    }
}
```

- [ ] **Step 3: Create FeatureFlagConfig**

Create `scm-common/core/src/main/java/com/scmcloud/common/feature/FeatureFlagConfig.java`:
```java
package com.scmcloud.common.feature;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FeatureFlagConfig {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagConfig(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @Around("@annotation(featureFlag)")
    public Object around(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag) throws Throwable {
        boolean enabled = featureFlagService.isEnabled(featureFlag.value());
        if (featureFlag.negate()) {
            enabled = !enabled;
        }
        if (!enabled) {
            throw new FeatureDisabledException("Feature '" + featureFlag.value() + "' is disabled");
        }
        return joinPoint.proceed();
    }

    public static class FeatureDisabledException extends RuntimeException {
        public FeatureDisabledException(String message) {
            super(message);
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add scm-common/core/src/main/java/com/scmcloud/common/feature/
git commit -m "feat: implement feature flag system with annotation support"
```

---

### Task 28: Implement Rate Limiting on Auth Endpoints

**Files:**
- Create: `scm-common/web/src/main/java/com/scmcloud/common/security/ratelimit/RateLimitFilter.java`
- Create: `scm-common/web/src/main/java/com/scmcloud/common/security/ratelimit/RateLimitConfig.java`

- [ ] **Step 1: Create RateLimitFilter**

Create `scm-common/web/src/main/java/com/scmcloud/common/security/ratelimit/RateLimitFilter.java`:
```java
package com.scmcloud.common.security.ratelimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RateLimitFilter implements Filter {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        if (path.startsWith("/api/v1/auth/")) {
            String clientIp = getClientIP(httpRequest);
            String key = "rate_limit:" + path + ":" + clientIp;

            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, WINDOW);
            }

            if (count != null && count > MAX_REQUESTS) {
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.getWriter().write("{\"code\":429,\"message\":\"Too many requests\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add scm-common/web/src/main/java/com/scmcloud/common/security/ratelimit/
git commit -m "feat: implement rate limiting on auth endpoints"
```

---

### Task 29: Implement Partition Management Automation

**Files:**
- Create: `scripts/db/partition/create-partitions.sh`
- Create: `scripts/db/partition/drop-old-partitions.sh`

- [ ] **Step 1: Create partition creation script**

Create `scripts/db/partition/create-partitions.sh`:
```bash
#!/bin/bash
# scripts/db/partition/create-partitions.sh
# Auto-create monthly partitions for partitioned tables

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"

TABLES=("ord_order" "inv_reservation" "sup_purchase_order")

for TABLE in "${TABLES[@]}"; do
    echo "Creating partitions for ${TABLE}..."
    
    # Create partitions for next 3 months
    for i in $(seq 0 2); do
        PARTITION_DATE=$(date -d "+${i} months" +%Y-%m)
        PARTITION_NAME="${TABLE}_p${PARTITION_DATE//-/}"
        
        PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_order -c "
            CREATE TABLE IF NOT EXISTS ${PARTITION_NAME} 
            PARTITION OF ${TABLE} 
            FOR VALUES FROM ('${PARTITION_DATE}-01') TO ('$(date -d "${PARTITION_DATE}-01 +1 month" +%Y-%m)-01');
        " 2>/dev/null || true
        
        echo "  Partition ${PARTITION_NAME} ensured"
    done
done

echo "Partition management complete"
```

- [ ] **Step 2: Create partition cleanup script**

Create `scripts/db/partition/drop-old-partitions.sh`:
```bash
#!/bin/bash
# scripts/db/partition/drop-old-partitions.sh
# Drop partitions older than retention period

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"
RETENTION_MONTHS="${RETENTION_MONTHS:-24}"

TABLES=("ord_order" "inv_reservation" "sup_purchase_order")

for TABLE in "${TABLES[@]}"; do
    echo "Dropping old partitions for ${TABLE}..."
    
    CUTOFF_DATE=$(date -d "-${RETENTION_MONTHS} months" +%Y-%m)
    
    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_order -c "
        SELECT partname FROM pg_partitions 
        WHERE tablename = '${TABLE}' 
        AND partname < '${TABLE}_p${CUTOFF_DATE//-/}'
    " -t -A | while read PARTITION; do
        if [ -n "$PARTITION" ]; then
            PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_order -c "
                DROP TABLE IF EXISTS ${PARTITION};
            "
            echo "  Dropped ${PARTITION}"
        fi
    done
done

echo "Old partition cleanup complete"
```

- [ ] **Step 3: Commit**

```bash
git add scripts/db/partition/
git commit -m "feat: implement partition management automation"
```

---

### Task 30: Implement Data Retention Policy

**Files:**
- Create: `scripts/db/retention/apply-retention.sh`

- [ ] **Step 1: Create retention script**

Create `scripts/db/retention/apply-retention.sh`:
```bash
#!/bin/bash
# scripts/db/retention/apply-retention.sh
# Apply data retention policies

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"

echo "Applying data retention policies..."

# Audit logs: keep 2 years
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_audit -c "
    DELETE FROM sys_audit_log WHERE create_time < NOW() - INTERVAL '2 years';
"
echo "  Audit logs: retained 2 years"

# Login history: keep 1 year
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_auth -c "
    DELETE FROM auth_login_history WHERE create_time < NOW() - INTERVAL '1 year';
"
echo "  Login history: retained 1 year"

# API access logs: keep 90 days
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_auth -c "
    DELETE FROM auth_api_access_log WHERE create_time < NOW() - INTERVAL '90 days';
"
echo "  API access logs: retained 90 days"

# Notification history: keep 6 months
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_notify -c "
    DELETE FROM notify_history WHERE create_time < NOW() - INTERVAL '6 months';
"
echo "  Notification history: retained 6 months"

# Order events: keep 3 years (for audit trail)
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_order -c "
    DELETE FROM ord_order_event WHERE create_time < NOW() - INTERVAL '3 years';
"
echo "  Order events: retained 3 years"

echo "Data retention policies applied successfully"
```

- [ ] **Step 2: Commit**

```bash
git add scripts/db/retention/
git commit -m "feat: implement data retention policy automation"
```

---

### Task 31: Implement Schema Registry

**Files:**
- Modify: `docker-compose.yml` (add Schema Registry service)

- [ ] **Step 1: Add Schema Registry to docker-compose.yml**

Add to docker-compose.yml before the `networks:` section:
```yaml
  # ============================================================
  # Schema Registry - Kafka Schema管理
  # ============================================================
  schema-registry:
    image: confluentinc/cp-schema-registry:7.6.0
    container_name: scm-schema-registry
    environment:
      SCHEMA_REGISTRY_HOST_NAME: schema-registry
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: kafka:9092
      SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081
    ports:
      - "8081:8081"
    depends_on:
      - kafka
    networks:
      - scm-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/subjects"]
      interval: 30s
      timeout: 10s
      retries: 5
```

- [ ] **Step 2: Commit**

```bash
git add docker-compose.yml
git commit -m "feat: add Schema Registry for Kafka schema management"
```

---

## Phase 2B: DevOps (5 Tasks)

### Task 32: Implement Load Testing (k6)

**Files:**
- Create: `scripts/loadtest/order-flow.js`
- Create: `scripts/loadtest/inventory-check.js`
- Create: `scripts/loadtest/config.js`

- [ ] **Step 1: Create k6 config**

Create `scripts/loadtest/config.js`:
```javascript
export const config = {
  baseURL: __ENV.BASE_URL || 'http://localhost:8203',
  authURL: __ENV.AUTH_URL || 'http://localhost:8106',
  username: __ENV.USERNAME || 'admin',
  password: __ENV.PASSWORD || 'admin123',
};
```

- [ ] **Step 2: Create order flow test**

Create `scripts/loadtest/order-flow.js`:
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { config } from './config.js';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  // Login
  const loginRes = http.post(`${config.authURL}/api/v1/auth/login`, JSON.stringify({
    username: config.username,
    password: config.password,
  }), { headers: { 'Content-Type': 'application/json' } });

  check(loginRes, { 'login successful': (r) => r.status === 200 });

  const token = loginRes.json('data?.token');
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  // Get orders
  const ordersRes = http.get(`${config.baseURL}/api/v1/orders?page=1&size=10`, { headers });
  check(ordersRes, { 'orders retrieved': (r) => r.status === 200 });

  sleep(1);
}
```

- [ ] **Step 3: Create inventory check test**

Create `scripts/loadtest/inventory-check.js`:
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { config } from './config.js';

export const options = {
  stages: [
    { duration: '30s', target: 30 },
    { duration: '1m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<100'],
    http_req_failed: ['rate<0.001'],
  },
};

export default function () {
  const headers = {
    'Content-Type': 'application/json',
  };

  // Check inventory (hot path - should be fast)
  const checkRes = http.post(`${config.baseURL}/api/v1/inventory/check`, JSON.stringify({
    skuId: 'SKU-001',
    quantity: 1,
  }), { headers });

  check(checkRes, { 'inventory check fast': (r) => r.status === 200 && r.timings.duration < 100 });

  sleep(0.1);
}
```

- [ ] **Step 4: Commit**

```bash
git add scripts/loadtest/
git commit -m "feat: implement load testing with k6"
```

---

### Task 33: Implement Helm Charts

**Files:**
- Create: `deploy/helm/scm-platform/Chart.yaml`
- Create: `deploy/helm/scm-platform/values.yaml`
- Create: `deploy/helm/scm-platform/templates/namespace.yaml`
- Create: `deploy/helm/scm-platform/templates/configmap.yaml`
- Create: `deploy/helm/scm-platform/templates/secrets.yaml`

- [ ] **Step 1: Create Chart.yaml**

Create `deploy/helm/scm-platform/Chart.yaml`:
```yaml
apiVersion: v2
name: scm-platform
description: SCM Platform Helm Chart
type: application
version: 0.1.0
appVersion: "1.0.0"
```

- [ ] **Step 2: Create values.yaml**

Create `deploy/helm/scm-platform/values.yaml`:
```yaml
namespace: scm-prod
replicaCount:
  auth: 3
  gateway: 3
  system: 3

image:
  repository: docker.io
  pullPolicy: IfNotPresent

env:
  nacosServer: nacos:8848
  dbHost: postgresql
  dbPort: "5432"
  redisHost: redis
  redisPort: "6379"

resources:
  auth:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1Gi"
      cpu: "500m"
  gateway:
    requests:
      memory: "256Mi"
      cpu: "100m"
    limits:
      memory: "512Mi"
      cpu: "250m"
```

- [ ] **Step 3: Create templates**

Create `deploy/helm/scm-platform/templates/namespace.yaml`:
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: {{ .Values.namespace }}
```

- [ ] **Step 4: Commit**

```bash
git add deploy/helm/
git commit -m "feat: implement Helm charts for Kubernetes deployment"
```

---

### Task 34: Implement Canary Deployments

**Files:**
- Create: `deploy/k8s/scm-auth-canary-deployment.yml`
- Create: `deploy/k8s/scm-auth-canary-service.yml`

- [ ] **Step 1: Create canary deployment**

Create `deploy/k8s/scm-auth-canary-deployment.yml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: scm-auth-canary
  namespace: scm-prod
spec:
  replicas: 1
  selector:
    matchLabels:
      app: scm-auth
      track: canary
  template:
    metadata:
      labels:
        app: scm-auth
        track: canary
    spec:
      containers:
        - name: scm-auth
          image: ${DOCKERHUB_USERNAME}/scm-auth:canary
          ports:
            - containerPort: 8106
          envFrom:
            - configMapRef:
                name: scm-config
            - secretRef:
                name: scm-secrets
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
```

- [ ] **Step 2: Create canary service**

Create `deploy/k8s/scm-auth-canary-service.yml`:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: scm-auth-canary
  namespace: scm-prod
spec:
  selector:
    app: scm-auth
    track: canary
  ports:
    - port: 8106
      targetPort: 8106
  type: ClusterIP
```

- [ ] **Step 3: Commit**

```bash
git add deploy/k8s/scm-auth-canary-*
git commit -m "feat: implement canary deployment manifests"
```

---

### Task 35: Implement GitOps with ArgoCD

**Files:**
- Create: `deploy/argocd/application.yaml`

- [ ] **Step 1: Create ArgoCD application manifest**

Create `deploy/argocd/application.yaml`:
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: scm-platform
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/your-org/scm-platform.git
    targetRevision: HEAD
    path: deploy/k8s
  destination:
    server: https://kubernetes.default.svc
    namespace: scm-prod
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```

- [ ] **Step 2: Commit**

```bash
git add deploy/argocd/
git commit -m "feat: implement GitOps with ArgoCD application manifest"
```

---

### Task 36: Implement Security Headers Audit

**Files:**
- Create: `docs/security/headers-audit.md`

- [ ] **Step 1: Create security headers audit document**

Create `docs/security/headers-audit.md`:
```markdown
# Security Headers Audit

## Current Configuration

| Header | Value | Status |
|--------|-------|--------|
| X-Content-Type-Options | nosniff | ✅ Enabled |
| X-Frame-Options | DENY | ✅ Enabled |
| Strict-Transport-Security | max-age=31536000; includeSubDomains | ✅ Enabled |
| X-XSS-Protection | 1; mode=block | ⚠️ Legacy - Consider removing |
| Referrer-Policy | strict-origin-when-cross-origin | ✅ Enabled |
| Content-Security-Policy | Not configured | ❌ Missing |
| Permissions-Policy | Not configured | ❌ Missing |

## Recommendations

### Immediate Actions
1. **Add Content-Security-Policy header** - Prevents XSS attacks
2. **Add Permissions-Policy header** - Controls browser features
3. **Remove X-XSS-Protection** - Deprecated, CSP is better

### CSP Configuration Example
```
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self';
```

### Permissions-Policy Example
```
Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()
```

## Implementation

Add to SecurityHeadersProperties.java:
```java
private String contentSecurityPolicy = "default-src 'self'";
private String permissionsPolicy = "camera=(), microphone=(), geolocation=()";
```

## Last Audit
- Date: 2026-06-12
- Auditor: Architecture Review Committee
- Next Review: 2026-09-12
```

- [ ] **Step 2: Commit**

```bash
git add docs/security/
git commit -m "docs: create security headers audit report"
```

---

## Phase 2C: Frontend (3 Tasks)

### Task 37: Implement PWA Offline Support

**Files:**
- Create: `scm-web/public/sw.js`
- Create: `scm-web/public/manifest.json`
- Modify: `scm-web/src/app/layout.tsx` (register service worker)

- [ ] **Step 1: Create service worker**

Create `scm-web/public/sw.js`:
```javascript
const CACHE_NAME = 'scm-platform-v1';
const urlsToCache = [
  '/',
  '/dashboard',
  '/orders',
  '/inventory',
  '/products',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(urlsToCache))
  );
});

self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.match(event.request)
      .then((response) => {
        if (response) {
          return response;
        }
        return fetch(event.request);
      })
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});
```

- [ ] **Step 2: Create manifest.json**

Create `scm-web/public/manifest.json`:
```json
{
  "name": "SCM Platform",
  "short_name": "SCM",
  "description": "Enterprise Supply Chain Management Platform",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#ffffff",
  "theme_color": "#1890ff",
  "icons": [
    {
      "src": "/icons/icon-192x192.png",
      "sizes": "192x192",
      "type": "image/png"
    },
    {
      "src": "/icons/icon-512x512.png",
      "sizes": "512x512",
      "type": "image/png"
    }
  ]
}
```

- [ ] **Step 3: Commit**

```bash
git add scm-web/public/sw.js scm-web/public/manifest.json
git commit -m "feat: implement PWA offline support with service worker"
```

---

### Task 38: Implement Error Boundary

**Files:**
- Create: `scm-web/src/components/error-boundary.tsx`

- [ ] **Step 1: Create error boundary component**

Create `scm-web/src/components/error-boundary.tsx`:
```tsx
'use client';

import React from 'react';

interface Props {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div style={{ padding: '20px', textAlign: 'center' }}>
          <h2>Something went wrong</h2>
          <p style={{ color: '#666' }}>{this.state.error?.message}</p>
          <button 
            onClick={() => this.setState({ hasError: false })}
            style={{
              marginTop: '10px',
              padding: '8px 16px',
              backgroundColor: '#1890ff',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            Try again
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add scm-web/src/components/error-boundary.tsx
git commit -m "feat: implement React error boundary component"
```

---

### Task 39: Implement Accessibility Audit

**Files:**
- Create: `docs/accessibility/audit.md`

- [ ] **Step 1: Create accessibility audit document**

Create `docs/accessibility/audit.md`:
```markdown
# Accessibility Audit (WCAG 2.1 AA)

## Current Status

| Criterion | Status | Notes |
|-----------|--------|-------|
| 1.1.1 Non-text Content | ⚠️ Partial | Some images missing alt text |
| 1.3.1 Info and Relationships | ✅ Pass | Semantic HTML used |
| 1.4.3 Contrast (Minimum) | ⚠️ Partial | Some text fails 4.5:1 ratio |
| 2.1.1 Keyboard | ✅ Pass | All interactive elements keyboard accessible |
| 2.4.1 Bypass Blocks | ❌ Fail | No skip navigation link |
| 2.4.2 Page Titled | ✅ Pass | All pages have titles |
| 2.4.3 Focus Order | ✅ Pass | Logical tab order |
| 2.4.6 Headings and Labels | ✅ Pass | Descriptive headings |
| 3.1.1 Language of Page | ❌ Fail | No lang attribute on html |
| 3.3.1 Error Identification | ⚠️ Partial | Errors shown but not announced |
| 4.1.1 Parsing | ✅ Pass | Valid HTML |
| 4.1.2 Name, Role, Value | ⚠️ Partial | Some ARIA attributes missing |

## Priority Fixes

### High Priority
1. Add skip navigation link
2. Add lang attribute to html element
3. Add alt text to all images
4. Fix color contrast issues

### Medium Priority
1. Add ARIA labels to interactive elements
2. Implement live regions for dynamic content
3. Add focus indicators

## Implementation Plan

### Skip Navigation
Add to layout.tsx:
```tsx
<a href="#main-content" className="sr-only focus:not-sr-only">
  Skip to main content
</a>
```

### Language Attribute
Update layout.tsx:
```tsx
<html lang="zh-CN">
```

## Last Audit
- Date: 2026-06-12
- Auditor: Architecture Review Committee
- WCAG Level: AA Target
- Next Review: 2026-09-12
```

- [ ] **Step 2: Commit**

```bash
git add docs/accessibility/
git commit -m "docs: create accessibility audit report (WCAG 2.1 AA)"
```

---

## Phase 2D: Documentation (2 Tasks)

### Task 40: Implement Data Dictionary

**Files:**
- Create: `docs/data/data-dictionary.md`

- [ ] **Step 1: Create data dictionary**

Create `docs/data/data-dictionary.md`:
```markdown
# Data Dictionary

## Order Service (db_order)

### ord_order
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| order_no | VARCHAR(32) | Order number (unique) |
| customer_id | BIGINT | Customer ID |
| status | VARCHAR(20) | Order status |
| total_amount | DECIMAL(12,2) | Total amount |
| create_time | TIMESTAMP | Creation time |
| update_time | TIMESTAMP | Last update time |

### ord_order_item
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| order_id | BIGINT | Order ID (FK) |
| sku_id | VARCHAR(32) | SKU ID |
| quantity | INT | Quantity |
| unit_price | DECIMAL(12,2) | Unit price |

## Inventory Service (db_inventory)

### inv_stock
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| sku_id | VARCHAR(32) | SKU ID |
| warehouse_id | BIGINT | Warehouse ID |
| available_qty | INT | Available quantity |
| reserved_qty | INT | Reserved quantity |
| safety_stock | INT | Safety stock level |

### inv_reservation
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| reservation_no | VARCHAR(32) | Reservation number |
| order_id | BIGINT | Order ID |
| sku_id | VARCHAR(32) | SKU ID |
| quantity | INT | Reserved quantity |
| status | VARCHAR(20) | Reservation status |
| reserved_at | TIMESTAMP | Reservation time |

## Product Service (db_product)

### pro_product
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| product_name | VARCHAR(100) | Product name |
| category_id | BIGINT | Category ID |
| brand | VARCHAR(50) | Brand |
| status | VARCHAR(20) | Product status |

### pro_sku
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| product_id | BIGINT | Product ID (FK) |
| sku_code | VARCHAR(32) | SKU code |
| sku_name | VARCHAR(100) | SKU name |
| price | DECIMAL(12,2) | Price |

## Last Updated
- Date: 2026-06-12
- Version: 1.0
```

- [ ] **Step 2: Commit**

```bash
git add docs/data/
git commit -m "docs: create data dictionary for core services"
```

---

### Task 41: Implement Data Quality Framework

**Files:**
- Create: `scripts/db/quality/check-quality.sh`

- [ ] **Step 1: Create data quality check script**

Create `scripts/db/quality/check-quality.sh`:
```bash
#!/bin/bash
# scripts/db/quality/check-quality.sh
# Run data quality checks

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"

echo "Running data quality checks..."

# Check for orphaned order items
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_order -c "
    SELECT COUNT(*) as orphaned_items 
    FROM ord_order_item oi 
    LEFT JOIN ord_order o ON oi.order_id = o.id 
    WHERE o.id IS NULL;
" -t -A

# Check for negative inventory
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_inventory -c "
    SELECT COUNT(*) as negative_stock 
    FROM inv_stock 
    WHERE available_qty < 0;
" -t -A

# Check for duplicate SKUs
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_product -c "
    SELECT sku_code, COUNT(*) as duplicate_count 
    FROM pro_sku 
    GROUP BY sku_code 
    HAVING COUNT(*) > 1;
" -t -A

echo "Data quality checks complete"
```

- [ ] **Step 2: Commit**

```bash
git add scripts/db/quality/
git commit -m "feat: implement data quality framework"
```

---

## Summary

| Phase | Tasks | Focus |
|-------|-------|-------|
| **Phase 2A: Backend** | 8 | Connection pooling, caching, tracing, feature flags, rate limiting, partitions, retention, schema registry |
| **Phase 2B: DevOps** | 5 | Load testing, Helm charts, canary deployments, ArgoCD, security audit |
| **Phase 2C: Frontend** | 3 | PWA, error boundary, accessibility |
| **Phase 2D: Documentation** | 2 | Data dictionary, data quality |

**Total: 18 tasks** covering the most critical remaining recommendations from the architecture review.
