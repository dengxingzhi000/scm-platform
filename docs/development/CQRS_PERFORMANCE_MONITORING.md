# CQRS Performance Monitoring Guide

## Table of Contents
1. [Overview](#overview)
2. [Monitoring Architecture](#monitoring-architecture)
3. [Micrometer Metrics](#micrometer-metrics)
4. [Prometheus Integration](#prometheus-integration)
5. [Grafana Dashboards](#grafana-dashboards)
6. [Performance KPIs & Thresholds](#performance-kpis--thresholds)
7. [Alert Configuration](#alert-configuration)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Best Practices](#best-practices)

---

## Overview

The SCM Platform's **CQRS (Command Query Responsibility Segregation)** pattern separates read and write operations across multiple databases (`db_user`, `db_org`, `db_permission`, `db_approval`, `db_audit`, `db_notify`). This guide provides comprehensive monitoring configuration for tracking the performance of CQRS services.

### CQRS Services Overview

**Query Services** (Read Operations - Execute on Slave DBs):
- `UserCrossDatabaseQueryService` - 16 methods
- `RoleCrossDatabaseQueryService` - 5 methods
- `DeptCrossDatabaseQueryService` - 5 methods
- `PermissionCrossDatabaseQueryService` - 1 method

**Command Services** (Write Operations - Execute on Master DB):
- `UserRoleCrossDatabaseCommandService` - 5 methods
- `DeptRoleCrossDatabaseCommandService` - 1 method

### Key Monitoring Objectives

1. **Cross-Database Query Performance** - Track latency for multi-database aggregations
2. **Read-Write Separation Effectiveness** - Monitor master vs. slave routing
3. **Cache Hit Rates** - Measure Caffeine (L1) and Redis (L2) effectiveness
4. **Database Connection Pool Health** - Track HikariCP metrics per datasource
5. **Service-Level Objectives (SLOs)** - Ensure 95th percentile latency < 200ms

---

## Monitoring Architecture

### Technology Stack

```
┌─────────────────────────────────────────────────────────────┐
│                  CQRS Service Methods                       │
│  @Timed(value="cross_db_query", extraTags={"method", ...}) │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Micrometer Metrics Registry                    │
│  - Timer Metrics (latency, throughput)                      │
│  - Counter Metrics (invocations)                            │
│  - Gauge Metrics (active operations)                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│             Prometheus Scrape Endpoint                      │
│  http://localhost:8081/actuator/prometheus                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                 Prometheus TSDB                             │
│  - Stores time-series metrics                               │
│  - Retention: 15 days (configurable)                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Grafana Dashboards                             │
│  - Real-time visualization                                  │
│  - Alerting via Prometheus AlertManager                     │
└─────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Role | Configuration |
|-----------|------|---------------|
| **@Timed Annotation** | Instruments method execution time | `extraTags = {"method", "methodName"}` |
| **Micrometer** | Metrics collection & abstraction | Auto-configured via Spring Boot Actuator |
| **Prometheus** | Time-series database | Scrapes `/actuator/prometheus` every 15s |
| **Grafana** | Visualization & alerting | Connects to Prometheus as datasource |
| **OpenTelemetry** | Distributed tracing | Traces cross-service calls (optional) |

---

## Micrometer Metrics

### @Timed Annotation Details

All CQRS query services use the `@Timed` annotation for automatic metric collection:

```java
@Timed(value = "cross_db_query", extraTags = {"method", "getUserBasicInfo"})
public SysUser getUserBasicInfo(UUID userId) {
    // Method implementation
}
```

**Metric Naming Convention**: `cross_db_query`

**Tags** (Dimensions):
- `method` - Specific method name (e.g., `getUserBasicInfo`, `findUserRolesWithNames`)
- `class` - Service class name (auto-added by Micrometer)
- `exception` - Exception type if method fails (auto-added)
- `outcome` - `SUCCESS` or `ERROR` (auto-added)

### Generated Metrics

For each `@Timed` method, Micrometer generates:

| Metric Name | Type | Description | Unit |
|-------------|------|-------------|------|
| `cross_db_query_seconds_count` | Counter | Total invocations | count |
| `cross_db_query_seconds_sum` | Counter | Total execution time | seconds |
| `cross_db_query_seconds_max` | Gauge | Maximum execution time | seconds |
| `cross_db_query_seconds_bucket` | Histogram | Latency distribution | seconds |

**Example Prometheus Query**:
```promql
# P95 latency for getUserBasicInfo
histogram_quantile(0.95,
  sum(rate(cross_db_query_seconds_bucket{method="getUserBasicInfo"}[5m])) by (le)
)

# Request rate (QPS)
rate(cross_db_query_seconds_count{method="getUserBasicInfo"}[1m])

# Error rate
rate(cross_db_query_seconds_count{method="getUserBasicInfo",outcome="ERROR"}[5m])
/
rate(cross_db_query_seconds_count{method="getUserBasicInfo"}[5m])
```

### Additional Metrics

Beyond `@Timed`, the system exposes:

**HikariCP Connection Pool Metrics** (per datasource):
```promql
# Active connections
hikaricp_connections_active{pool="user"}
hikaricp_connections_active{pool="permission"}

# Connection wait time
hikaricp_connections_acquire_seconds{pool="user"}

# Pool usage
hikaricp_connections_usage{pool="user"}
```

**Cache Metrics** (Caffeine L1 + Redis L2):
```promql
# Cache hit rate
cache_gets{cache="userRoles",result="hit"}
/
cache_gets{cache="userRoles"}

# Cache evictions
cache_evictions{cache="userRoles"}
```

**Database Query Metrics** (MyBatis-Plus):
```promql
# Database query execution time
mybatis_query_seconds{mapper="SysUserMapper"}
```

---

## Prometheus Integration

### Configuration in scm-system/service

**File**: `scm-system/service/src/main/resources/application.yaml`

Add the following to enable Prometheus metrics export:

```yaml
# Management endpoints configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
      base-path: /actuator

  # Micrometer metrics configuration
  metrics:
    tags:
      application: ${spring.application.name}
      service: system-service
      environment: ${ENVIRONMENT:dev}

    # Enable percentile histograms for accurate P95/P99 calculations
    distribution:
      percentiles-histogram:
        cross_db_query: true
        http.server.requests: true

      # Service Level Objectives (SLO) boundaries
      slo:
        cross_db_query: 50ms,100ms,200ms,500ms,1s,2s
        http.server.requests: 100ms,200ms,500ms,1s,2s,5s

      # Minimum expected value (for histogram optimization)
      minimum-expected-value:
        cross_db_query: 1ms

      # Maximum expected value (for histogram optimization)
      maximum-expected-value:
        cross_db_query: 5s

    # Enable JVM metrics
    enable:
      jvm: true
      process: true
      system: true
      tomcat: true
      hikaricp: true
      logback: true

    export:
      prometheus:
        enabled: true
        step: 15s  # Scrape interval (matches Prometheus config)
        descriptions: true

  # Distributed tracing with OpenTelemetry (optional)
  tracing:
    enabled: ${TRACING_ENABLED:false}
    sampling:
      probability: 0.1  # 10% sampling rate
    propagation:
      type: w3c  # W3C Trace Context standard

# Logging configuration for monitoring
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  level:
    # Enable debug logging for read-write routing (to verify @Slave/@Master)
    com.frog.common.data.rw: INFO
    com.frog.system.service.query: DEBUG
    com.frog.system.service.command: DEBUG

    # HikariCP connection pool logging
    com.zaxxer.hikari: INFO

    # MyBatis SQL logging (disable in production)
    com.frog.system.mapper: ${SQL_LOG_LEVEL:INFO}
```

### Prometheus Server Configuration

**File**: `prometheus.yml` (on Prometheus server)

```yaml
global:
  scrape_interval: 15s      # Scrape interval
  evaluation_interval: 15s  # Rule evaluation interval
  external_labels:
    cluster: 'cognora-aura-platform'
    region: 'cn-east-1'

# Scrape configuration for SCM services
scrape_configs:
  # System service (scm-system)
  - job_name: 'scm-system-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']
        labels:
          service: 'system-service'
          environment: 'production'

    # Scrape timeout (must be shorter than scrape_interval)
    scrape_timeout: 10s

    # Relabeling (add custom labels)
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'system-service-1'

  # Additional services can be added here
  - job_name: 'scm-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8761']

# Alerting configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['localhost:9093']

# Load alert rules
rule_files:
  - 'alerts/cqrs_performance_alerts.yml'
```

### Verifying Metrics Export

1. **Check Actuator endpoint**:
```bash
curl http://localhost:8081/actuator/prometheus | grep cross_db_query
```

Expected output:
```
# HELP cross_db_query_seconds_max
# TYPE cross_db_query_seconds_max gauge
cross_db_query_seconds_max{class="UserCrossDatabaseQueryService",method="getUserBasicInfo",} 0.0523
# HELP cross_db_query_seconds
# TYPE cross_db_query_seconds histogram
cross_db_query_seconds_bucket{class="UserCrossDatabaseQueryService",method="getUserBasicInfo",le="0.05",} 142.0
cross_db_query_seconds_bucket{class="UserCrossDatabaseQueryService",method="getUserBasicInfo",le="0.1",} 198.0
...
```

2. **Check Prometheus targets**:
```
http://prometheus-server:9090/targets
```
Verify `scm-system-service` shows `UP` status.

3. **Query metrics in Prometheus**:
```
http://prometheus-server:9090/graph
```
Run query: `cross_db_query_seconds_count`

---

## Grafana Dashboards

### Installation & Setup

1. **Add Prometheus Datasource**:
   - Navigate to: Configuration → Data Sources → Add data source
   - Select: Prometheus
   - URL: `http://prometheus-server:9090`
   - Click: Save & Test

2. **Import CQRS Dashboard**:
   - Navigate to: Dashboards → Import
   - Upload: `docs/development/grafana-cqrs-dashboard.json`
   - Select Prometheus datasource
   - Click: Import

### Dashboard Panels Overview

The CQRS Performance Dashboard includes:

#### 1. Service Health Overview (Row 1)
- **Total Requests** - Total cross-DB query invocations
- **Average Latency** - Mean response time across all methods
- **P95 Latency** - 95th percentile latency (SLO target: < 200ms)
- **Error Rate** - Percentage of failed requests

#### 2. Method-Level Performance (Row 2)
- **Request Rate by Method** - QPS per CQRS method
- **Latency Heatmap** - Distribution of response times
- **Top 10 Slowest Methods** - Ranked by P95 latency
- **Cache Hit Rate** - L1 (Caffeine) and L2 (Redis) effectiveness

#### 3. Database Performance (Row 3)
- **Connection Pool Usage** - Active/idle connections per datasource
- **Connection Acquisition Time** - Wait time for DB connections
- **Database Query Time** - MyBatis query execution time
- **Read-Write Separation** - Master vs. Slave request distribution

#### 4. Error Analysis (Row 4)
- **Error Count by Method** - Total errors per CQRS method
- **Error Rate Trend** - Error percentage over time
- **Exception Types** - Breakdown of exception classes
- **Slow Query Log** - Queries exceeding 500ms threshold

#### 5. Resource Utilization (Row 5)
- **JVM Heap Usage** - Memory consumption
- **CPU Usage** - System-level CPU utilization
- **Garbage Collection** - GC pause times
- **Thread Pool** - Active thread count

---

## Performance KPIs & Thresholds

### Service-Level Objectives (SLOs)

| KPI | Target | Warning | Critical | Measurement |
|-----|--------|---------|----------|-------------|
| **P95 Latency** | < 200ms | 200-500ms | > 500ms | 95th percentile response time |
| **P99 Latency** | < 500ms | 500ms-1s | > 1s | 99th percentile response time |
| **Request Rate** | N/A | N/A | < 10 QPS (baseline check) | Requests per second |
| **Error Rate** | < 0.1% | 0.1-1% | > 1% | Failed requests / Total requests |
| **Cache Hit Rate** | > 80% | 60-80% | < 60% | Cache hits / Total cache access |
| **Connection Pool Usage** | < 70% | 70-85% | > 85% | Active connections / Max pool size |
| **Connection Wait Time** | < 50ms | 50-100ms | > 100ms | Time to acquire DB connection |
| **Database Query Time** | < 100ms | 100-200ms | > 200ms | MyBatis query execution time |

### Method-Specific Thresholds

**User Query Service**:
| Method | P95 Target | Reason |
|--------|------------|--------|
| `getUserBasicInfo` | < 50ms | Single-row query on indexed `id` column |
| `getUserBasicInfoBatch` | < 150ms | Batch query (up to 100 users) |
| `findUserRolesWithNames` | < 100ms | Join across `sys_user_role` + `sys_role` |
| `findPermissionCodesByUserId` | < 200ms | Multi-level join (user → role → permission) |
| `countUsersByDeptIds` | < 200ms | Aggregation query across multiple departments |

**Role Query Service**:
| Method | P95 Target | Reason |
|--------|------------|--------|
| `findAccessibleDeptIds` | < 250ms | Recursive CTE query for department hierarchy |
| `findExpiringRolesWithUserInfo` | < 300ms | Complex join with date filtering |

**Dept Query Service**:
| Method | P95 Target | Reason |
|--------|------------|--------|
| `selectDeptTree` | < 200ms | Hierarchical query with leader info |
| `hasAccessToDept` | < 100ms | Simple recursive check |

**Permission Query Service**:
| Method | P95 Target | Reason |
|--------|------------|--------|
| `findMenuTreeByUserId` | < 250ms | Complex join (user → role → permission + menu tree) |

**Command Services** (Write Operations):
| Method | P95 Target | Reason |
|--------|------------|--------|
| `batchInsertUserRoles` | < 100ms | Batch insert (up to 20 roles) |
| `deleteUserRoles` | < 50ms | Indexed delete on `user_id` |

### Baseline Performance Expectations

**Baseline Test Scenario**:
- 1000 users, 50 roles, 200 permissions
- 10 departments with 3-level hierarchy
- Average user has 3 roles
- Cache warm (80% hit rate)

**Expected Throughput**:
- Query operations: 1000-2000 QPS (limited by DB read capacity)
- Command operations: 200-500 TPS (limited by DB write capacity)

---

## Alert Configuration

### Prometheus Alert Rules

**File**: `prometheus/alerts/cqrs_performance_alerts.yml`

```yaml
groups:
  - name: cqrs_performance_alerts
    interval: 30s
    rules:
      # High P95 Latency Alert
      - alert: HighCQRSLatency
        expr: |
          histogram_quantile(0.95,
            sum(rate(cross_db_query_seconds_bucket[5m])) by (le, method)
          ) > 0.5
        for: 5m
        labels:
          severity: warning
          service: system-service
        annotations:
          summary: "High CQRS query latency detected"
          description: "Method {{ $labels.method }} P95 latency is {{ $value }}s (threshold: 500ms)"

      # Critical P95 Latency Alert
      - alert: CriticalCQRSLatency
        expr: |
          histogram_quantile(0.95,
            sum(rate(cross_db_query_seconds_bucket[5m])) by (le, method)
          ) > 1
        for: 2m
        labels:
          severity: critical
          service: system-service
        annotations:
          summary: "CRITICAL: CQRS query latency exceeds 1s"
          description: "Method {{ $labels.method }} P95 latency is {{ $value }}s"

      # High Error Rate Alert
      - alert: HighCQRSErrorRate
        expr: |
          (
            sum(rate(cross_db_query_seconds_count{outcome="ERROR"}[5m])) by (method)
            /
            sum(rate(cross_db_query_seconds_count[5m])) by (method)
          ) > 0.01
        for: 5m
        labels:
          severity: warning
          service: system-service
        annotations:
          summary: "High CQRS error rate detected"
          description: "Method {{ $labels.method }} error rate is {{ $value | humanizePercentage }}"

      # Low Cache Hit Rate Alert
      - alert: LowCacheHitRate
        expr: |
          (
            sum(rate(cache_gets{cache=~"userRoles|userPermissionCodes",result="hit"}[10m]))
            /
            sum(rate(cache_gets{cache=~"userRoles|userPermissionCodes"}[10m]))
          ) < 0.6
        for: 10m
        labels:
          severity: warning
          service: system-service
        annotations:
          summary: "Low cache hit rate detected"
          description: "Cache {{ $labels.cache }} hit rate is {{ $value | humanizePercentage }}"

      # High Connection Pool Usage Alert
      - alert: HighConnectionPoolUsage
        expr: |
          (
            hikaricp_connections_active{pool=~"user|permission|org"}
            /
            hikaricp_connections_max{pool=~"user|permission|org"}
          ) > 0.85
        for: 5m
        labels:
          severity: warning
          service: system-service
        annotations:
          summary: "High database connection pool usage"
          description: "Pool {{ $labels.pool }} usage is {{ $value | humanizePercentage }}"

      # Connection Acquisition Timeout Alert
      - alert: SlowConnectionAcquisition
        expr: |
          histogram_quantile(0.95,
            sum(rate(hikaricp_connections_acquire_seconds_bucket[5m])) by (le, pool)
          ) > 0.1
        for: 5m
        labels:
          severity: warning
          service: system-service
        annotations:
          summary: "Slow database connection acquisition"
          description: "Pool {{ $labels.pool }} P95 acquisition time is {{ $value }}s"

      # Read-Write Separation Failure (No Slave Traffic)
      - alert: NoSlaveTraffic
        expr: |
          sum(rate(cross_db_query_seconds_count{datasource_type="slave"}[10m])) == 0
          and
          sum(rate(cross_db_query_seconds_count[10m])) > 10
        for: 10m
        labels:
          severity: critical
          service: system-service
        annotations:
          summary: "Read-write separation failure: No slave traffic detected"
          description: "All read queries are routing to master database"
```

### AlertManager Configuration

**File**: `alertmanager/alertmanager.yml`

```yaml
global:
  resolve_timeout: 5m
  slack_api_url: 'https://hooks.slack.com/services/YOUR_WEBHOOK_URL'

route:
  group_by: ['alertname', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'slack-notifications'

  routes:
    # Critical alerts go to PagerDuty
    - match:
        severity: critical
      receiver: 'pagerduty-critical'
      continue: true

    # Warning alerts go to Slack
    - match:
        severity: warning
      receiver: 'slack-notifications'

receivers:
  - name: 'slack-notifications'
    slack_configs:
      - channel: '#scm-alerts'
        title: 'SCM Platform Alert'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

  - name: 'pagerduty-critical'
    pagerduty_configs:
      - service_key: 'YOUR_PAGERDUTY_SERVICE_KEY'
```

---

## Troubleshooting Guide

### Common Performance Issues

#### 1. High Latency on Cross-Database Queries

**Symptoms**:
- P95 latency > 500ms for methods like `findMenuTreeByUserId`
- Slow response times in user permission checks

**Diagnosis**:
```promql
# Identify slowest methods
topk(10,
  histogram_quantile(0.95,
    sum(rate(cross_db_query_seconds_bucket[5m])) by (le, method)
  )
)

# Check database query time
histogram_quantile(0.95,
  sum(rate(mybatis_query_seconds_bucket{mapper=~"SysUser.*"}[5m])) by (le, mapper)
)
```

**Root Causes**:
- **Missing database indexes** - Check `EXPLAIN ANALYZE` for sequential scans
- **N+1 query problem** - Method making multiple round-trips to DB
- **Large result sets** - Fetching too many rows without pagination

**Solutions**:
```sql
-- Add missing indexes (example)
CREATE INDEX idx_user_role_user_id ON sys_user_role(user_id) WHERE deleted = 0;
CREATE INDEX idx_role_permission_role_id ON sys_role_permission(role_id);

-- Optimize recursive CTE queries
CREATE INDEX idx_dept_parent_id ON sys_dept(parent_id);
```

Enable batch fetching:
```java
@BatchSize(size = 100)  // Add to entity relationship
private List<SysRole> roles;
```

#### 2. Low Cache Hit Rate

**Symptoms**:
- Cache hit rate < 60% for `userRoles`, `userPermissionCodes`
- High database load despite caching enabled

**Diagnosis**:
```promql
# Cache hit rate by cache name
sum(rate(cache_gets{result="hit"}[10m])) by (cache)
/
sum(rate(cache_gets[10m])) by (cache)

# Cache evictions
rate(cache_evictions[5m])
```

**Root Causes**:
- **Cache TTL too short** - Data evicted before reuse
- **Cache key collision** - Multiple tenants sharing same key
- **Cache size too small** - Frequent evictions due to memory pressure

**Solutions**:
```yaml
# Increase cache TTL (application.yaml)
spring:
  cache:
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=30m  # Increase from 5m to 30m
    redis:
      time-to-live: 3600000  # 1 hour (increase from 30m)
```

Add tenant isolation to cache keys:
```java
@Cacheable(value = "userRoles", key = "#tenantId + ':' + #userId")
public List<Map<String, Object>> findUserRolesWithNames(String tenantId, UUID userId) {
    // ...
}
```

#### 3. Database Connection Pool Exhaustion

**Symptoms**:
- `hikaricp_connections_active` / `hikaricp_connections_max` > 0.9
- `HikariPool-1 - Connection is not available` errors in logs

**Diagnosis**:
```promql
# Connection pool usage
hikaricp_connections_active{pool="user"}
/
hikaricp_connections_max{pool="user"}

# Connection wait time
histogram_quantile(0.95,
  sum(rate(hikaricp_connections_acquire_seconds_bucket[5m])) by (le, pool)
)

# Pending thread count
hikaricp_connections_pending{pool="user"}
```

**Root Causes**:
- **Connection leaks** - Connections not properly closed
- **Slow queries holding connections** - Long-running transactions
- **Pool size too small** - Insufficient connections for load

**Solutions**:
```yaml
# Increase pool size (application.yaml)
spring:
  datasource:
    dynamic:
      hikari:
        maximum-pool-size: 30  # Increase from 20
        minimum-idle: 10       # Increase from 5
        connection-timeout: 10000  # 10s timeout
        leak-detection-threshold: 30000  # 30s leak detection
```

Enable connection leak detection logging:
```yaml
logging:
  level:
    com.zaxxer.hikari: DEBUG
```

#### 4. Read-Write Separation Not Working

**Symptoms**:
- All queries routing to master database
- Slave databases show zero traffic
- No `@Slave` annotation effectiveness

**Diagnosis**:
```bash
# Check application logs for routing decisions
tail -f logs/system-service.log | grep "Routing to"
```

Expected output:
```
2025-01-16 10:23:45 DEBUG [ReadWriteRoutingDataSource] Routing to SLAVE for read operation
```

**Root Causes**:
- **@Slave annotation missing** - Query methods not annotated
- **Transaction context active** - @Transactional forces master routing
- **Read-write separation disabled** - `spring.datasource.rw.enabled=false`

**Solutions**:
```java
// Ensure @Slave annotation is present
@Slave
public SysUser getUserBasicInfo(UUID userId) {
    return userMapper.selectById(userId);
}

// Remove unnecessary @Transactional from read-only methods
// @Transactional  <-- REMOVE THIS
@Slave
public List<Map<String, Object>> findUserRolesWithNames(UUID userId) {
    return userRoleMapper.findUserRolesWithNames(userId);
}
```

Verify configuration:
```yaml
spring:
  datasource:
    rw:
      enabled: true  # MUST be true
```

#### 5. High Error Rate

**Symptoms**:
- Error rate > 1% for CQRS methods
- `NullPointerException`, `SQLException` in logs

**Diagnosis**:
```promql
# Error count by method
sum(rate(cross_db_query_seconds_count{outcome="ERROR"}[5m])) by (method, exception)

# Error rate percentage
(
  sum(rate(cross_db_query_seconds_count{outcome="ERROR"}[5m]))
  /
  sum(rate(cross_db_query_seconds_count[5m]))
) * 100
```

**Root Causes**:
- **Null parameter handling** - Methods not validating input
- **Database connection failures** - Network issues or DB downtime
- **Data inconsistency** - Foreign key violations in cross-DB queries

**Solutions**:
Add input validation:
```java
@Slave
@Timed(value = "cross_db_query", extraTags = {"method", "getUserBasicInfo"})
public SysUser getUserBasicInfo(UUID userId) {
    if (userId == null) {
        log.warn("getUserBasicInfo called with null userId");
        return null;  // Graceful degradation
    }
    return userMapper.selectById(userId);
}
```

Add circuit breaker for database calls:
```java
@CircuitBreaker(name = "userDatabase", fallbackMethod = "getUserBasicInfoFallback")
public SysUser getUserBasicInfo(UUID userId) {
    return userMapper.selectById(userId);
}

public SysUser getUserBasicInfoFallback(UUID userId, Exception e) {
    log.error("Failed to get user basic info, returning cached data", e);
    return cacheService.getCachedUser(userId);
}
```

---

## Best Practices

### 1. Annotation Guidelines

Use `@Timed` on all public CQRS service methods:
```java
@Timed(value = "cross_db_query", extraTags = {"method", "methodName"})
public ReturnType methodName(Parameters...) {
    // Implementation
}
```

Use `@Slave` for all read operations:
```java
@Slave
@Timed(value = "cross_db_query", extraTags = {"method", "getUserBasicInfo"})
public SysUser getUserBasicInfo(UUID userId) {
    // Read from slave DB
}
```

Use `@Master` for all write operations:
```java
@Master(reason = "Write operation must use master database")
@Transactional(rollbackFor = Exception.class)
public int batchInsertUserRoles(UUID userId, List<UUID> roleIds, UUID createBy) {
    // Write to master DB
}
```

### 2. Performance Testing

Run regular load tests to validate SLOs:

```bash
# JMeter test plan for CQRS queries
jmeter -n -t cqrs_load_test.jmx \
  -Jusers=100 \
  -Jrampup=30 \
  -Jduration=300 \
  -l results.jtl

# Analyze P95 latency
awk '{sum+=$2; count++; times[count]=$2} END {
  asort(times);
  print "P95:", times[int(count*0.95)]
}' results.jtl
```

### 3. Database Optimization

Regularly review slow query logs:
```sql
-- PostgreSQL: Enable slow query logging
ALTER SYSTEM SET log_min_duration_statement = 500;  -- Log queries > 500ms
SELECT pg_reload_conf();

-- View slow queries
SELECT
  mean_exec_time,
  calls,
  query
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;
```

### 4. Cache Strategy

Implement cache warming for frequently accessed data:
```java
@Scheduled(cron = "0 0 * * * *")  // Hourly
public void warmUserCache() {
    List<UUID> activeUserIds = userMapper.selectActiveUserIds();
    activeUserIds.forEach(userId -> {
        getUserBasicInfo(userId);  // Populates cache
        findUserRolesWithNames(userId);
    });
}
```

### 5. Monitoring Dashboard Maintenance

- **Review dashboard weekly** - Ensure panels reflect current metrics
- **Update thresholds quarterly** - Adjust based on growth patterns
- **Archive historical data** - Export Prometheus data to long-term storage (e.g., Thanos)

---

## Appendix

### A. Metric Labels Reference

| Label | Description | Example Values |
|-------|-------------|----------------|
| `method` | CQRS method name | `getUserBasicInfo`, `findUserRolesWithNames` |
| `class` | Service class name | `UserCrossDatabaseQueryService` |
| `outcome` | Operation result | `SUCCESS`, `ERROR` |
| `exception` | Exception type (if failed) | `NullPointerException`, `SQLException` |
| `pool` | Database connection pool | `user`, `permission`, `org` |
| `cache` | Cache name | `userRoles`, `userPermissionCodes` |
| `datasource_type` | Datasource routing | `master`, `slave` |

### B. Prometheus Query Examples

**Top 10 Slowest Methods (P95)**:
```promql
topk(10,
  histogram_quantile(0.95,
    sum(rate(cross_db_query_seconds_bucket[5m])) by (le, method)
  )
)
```

**Request Rate by Service Class**:
```promql
sum(rate(cross_db_query_seconds_count[1m])) by (class)
```

**Error Rate Percentage**:
```promql
(
  sum(rate(cross_db_query_seconds_count{outcome="ERROR"}[5m]))
  /
  sum(rate(cross_db_query_seconds_count[5m]))
) * 100
```

**Database Connection Pool Health**:
```promql
# Active connections
hikaricp_connections_active{pool="user"}

# Idle connections
hikaricp_connections_idle{pool="user"}

# Pool usage percentage
(hikaricp_connections_active / hikaricp_connections_max) * 100
```

### C. Useful Grafana Variables

Add these to dashboard for dynamic filtering:

```json
{
  "name": "method",
  "label": "CQRS Method",
  "query": "label_values(cross_db_query_seconds_count, method)",
  "multi": true,
  "includeAll": true
}
```

```json
{
  "name": "pool",
  "label": "Database Pool",
  "query": "label_values(hikaricp_connections_active, pool)",
  "multi": true,
  "includeAll": true
}
```

### D. Related Documentation

- [Development Standards](DEVELOPMENT_STANDARDS.md) - Code quality guidelines
- [Multi-Tenant Design](../multi-tenant/PERMISSION_MULTI_TENANT_DESIGN.md) - Multi-database architecture
- [Read-Write Separation Guide](../guides/READ_WRITE_SEPARATION_GUIDE.md) - @Slave/@Master usage
- [CLAUDE.md](../../CLAUDE.md) - Project overview and commands

---

**Document Version**: 1.0.0
**Last Updated**: 2025-01-16
**Maintained By**: Platform Team