# CQRS Performance Monitoring - Quick Start Guide

## Overview

This quick start guide helps developers set up and use CQRS performance monitoring in the SCM Platform.

---

## Prerequisites

- Prometheus server running on `http://localhost:9090`
- Grafana server running on `http://localhost:3000`
- scm-system service running on `http://localhost:8081`

---

## 5-Minute Setup

### 1. Verify Metrics Export

Check that the system service is exposing metrics:

```bash
# Test Prometheus endpoint
curl http://localhost:8081/actuator/prometheus | grep cross_db_query

# Expected output:
# cross_db_query_seconds_count{...} 142.0
# cross_db_query_seconds_sum{...} 7.234
```

If no output, verify `application.yaml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
```

### 2. Configure Prometheus

Add this to `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'scm-system-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']
```

Reload Prometheus:
```bash
# Linux/Mac
kill -HUP $(pgrep prometheus)

# Or restart
docker restart prometheus
```

Verify target is UP:
```
http://localhost:9090/targets
```

### 3. Import Grafana Dashboard

1. Open Grafana: `http://localhost:3000` (admin/admin)
2. Navigate to: **Dashboards** → **Import**
3. Upload file: `docs/development/grafana-cqrs-dashboard.json`
4. Select Prometheus datasource
5. Click **Import**

### 4. Configure Alerts (Optional)

Copy alert rules to Prometheus:
```bash
cp docs/development/prometheus-cqrs-alerts.yml /etc/prometheus/alerts/
```

Update `prometheus.yml`:
```yaml
rule_files:
  - 'alerts/prometheus-cqrs-alerts.yml'
```

Reload Prometheus:
```bash
kill -HUP $(pgrep prometheus)
```

Verify alerts are loaded:
```
http://localhost:9090/alerts
```

---

## Using the Dashboard

### Key Panels

**Service Health Overview** (Top Row):
- Total Requests: Cumulative query count
- Average Latency: Mean response time
- **P95 Latency: SLO target is < 200ms**
- Error Rate: Should be < 0.1%

**Method-Level Performance**:
- Request Rate by Method: Identify hotspots
- Latency Heatmap: Visualize distribution
- Top 10 Slowest Methods: Focus optimization efforts
- Cache Hit Rate: Should be > 80%

**Database Performance**:
- Connection Pool Usage: Should be < 70%
- Connection Acquisition Time: Should be < 50ms
- Read-Write Separation: Verify slave traffic

### Dashboard Variables

Use dropdown filters to focus analysis:

- **CQRS Method**: Select specific methods (e.g., `getUserBasicInfo`)
- **Service Class**: Filter by service (e.g., `UserCrossDatabaseQueryService`)
- **Database Pool**: Filter by database (e.g., `user`, `permission`)

### Time Range Selection

- **Real-time monitoring**: Use "Last 15 minutes" with 30s refresh
- **Incident investigation**: Use custom range (e.g., "Last 6 hours")
- **Performance testing**: Use "Last 1 hour" during load tests

---

## Key Metrics to Watch

### Critical Metrics

| Metric | Query | Threshold |
|--------|-------|-----------|
| **P95 Latency** | `histogram_quantile(0.95, sum(rate(cross_db_query_seconds_bucket[5m])) by (le))` | < 200ms |
| **Error Rate** | `sum(rate(cross_db_query_seconds_count{outcome="ERROR"}[5m])) / sum(rate(cross_db_query_seconds_count[5m]))` | < 0.1% |
| **Cache Hit Rate** | `sum(rate(cache_gets{result="hit"}[5m])) / sum(rate(cache_gets[5m]))` | > 80% |
| **Connection Pool Usage** | `hikaricp_connections_active / hikaricp_connections_max` | < 70% |

### Performance Queries

**Top 5 Slowest Methods**:
```promql
topk(5,
  histogram_quantile(0.95,
    sum(rate(cross_db_query_seconds_bucket[5m])) by (le, method)
  )
)
```

**Request Rate by Service Class**:
```promql
sum(rate(cross_db_query_seconds_count[1m])) by (class)
```

**Database Query Time (MyBatis)**:
```promql
histogram_quantile(0.95,
  sum(rate(mybatis_query_seconds_bucket[5m])) by (le, mapper)
)
```

**Read-Write Separation Ratio**:
```promql
sum(rate(cross_db_query_seconds_count{datasource_type="slave"}[5m]))
/
sum(rate(cross_db_query_seconds_count[5m]))
* 100
```

---

## Common Troubleshooting

### Issue: No Metrics in Grafana

**Diagnosis**:
```bash
# Check service is running
curl http://localhost:8081/actuator/health

# Check Prometheus endpoint
curl http://localhost:8081/actuator/prometheus

# Check Prometheus is scraping
curl http://localhost:9090/api/v1/targets
```

**Solution**:
1. Verify `management.endpoints.web.exposure.include` in `application.yaml`
2. Check Prometheus `scrape_configs` target URL
3. Verify firewall rules allow Prometheus → Service connection

### Issue: High P95 Latency

**Diagnosis**:
```promql
# Identify slowest method
topk(1, cross_db_query_seconds_max)

# Check database query time
histogram_quantile(0.95,
  sum(rate(mybatis_query_seconds_bucket[5m])) by (le, mapper)
)

# Check connection pool
hikaricp_connections_active / hikaricp_connections_max
```

**Solution**:
1. Check database indexes with `EXPLAIN ANALYZE`
2. Review N+1 query patterns
3. Increase connection pool size if usage > 85%
4. Enable query result caching

### Issue: Low Cache Hit Rate

**Diagnosis**:
```promql
# Hit rate by cache
sum(rate(cache_gets{result="hit"}[10m])) by (cache)
/
sum(rate(cache_gets[10m])) by (cache)

# Evictions
rate(cache_evictions[5m])
```

**Solution**:
1. Increase cache TTL in `application.yaml`
2. Increase cache size (Caffeine `maximumSize`)
3. Add tenant isolation to cache keys
4. Check Redis connectivity and memory

### Issue: Connection Pool Exhaustion

**Diagnosis**:
```promql
# Pool usage
hikaricp_connections_active{pool="user"} / hikaricp_connections_max{pool="user"}

# Pending threads
hikaricp_connections_pending{pool="user"}

# Acquisition time
histogram_quantile(0.95,
  sum(rate(hikaricp_connections_acquire_seconds_bucket[5m])) by (le, pool)
)
```

**Solution**:
```yaml
# Increase pool size in application.yaml
spring:
  datasource:
    dynamic:
      hikari:
        maximum-pool-size: 30  # Increase from 20
        leak-detection-threshold: 30000  # Enable leak detection
```

### Issue: Read-Write Separation Not Working

**Diagnosis**:
```bash
# Check logs for routing decisions
tail -f logs/system-service.log | grep "Routing to"

# Check slave traffic
curl -s 'http://localhost:9090/api/v1/query?query=sum(rate(cross_db_query_seconds_count{datasource_type="slave"}[5m]))'
```

**Solution**:
1. Verify `@Slave` annotation on query methods
2. Remove `@Transactional` from read-only methods
3. Check `spring.datasource.rw.enabled=true`
4. Verify slave database connectivity

---

## Load Testing

### JMeter Test Plan

Create `cqrs_load_test.jmx`:

```xml
<ThreadGroup guiclass="ThreadGroupGui" testname="CQRS Load Test">
  <stringProp name="ThreadGroup.num_threads">100</stringProp>
  <stringProp name="ThreadGroup.ramp_time">30</stringProp>
  <stringProp name="ThreadGroup.duration">300</stringProp>
</ThreadGroup>
```

Run test:
```bash
jmeter -n -t cqrs_load_test.jmx \
  -Jhost=localhost \
  -Jport=8081 \
  -l results.jtl \
  -e -o report/
```

### Load Test Scenarios

**Scenario 1: User Permission Check** (High Frequency)
- Endpoint: `GET /api/v1/users/{userId}/permissions`
- Target: 500 QPS
- Expected P95: < 100ms

**Scenario 2: Menu Tree Load** (Medium Frequency)
- Endpoint: `GET /api/v1/users/{userId}/menu-tree`
- Target: 200 QPS
- Expected P95: < 250ms

**Scenario 3: Department Hierarchy** (Low Frequency)
- Endpoint: `GET /api/v1/depts/{deptId}/accessible`
- Target: 50 QPS
- Expected P95: < 300ms

### Monitoring During Load Test

Watch these metrics in Grafana:

1. **P95 Latency** - Should stay below thresholds
2. **Error Rate** - Should remain < 0.1%
3. **Connection Pool Usage** - Should not exceed 85%
4. **Cache Hit Rate** - Should improve over time (warmup)
5. **JVM Heap Usage** - Should stabilize after warmup

---

## Best Practices

### 1. Annotation Usage

Always use `@Timed` with method tag:
```java
@Timed(value = "cross_db_query", extraTags = {"method", "getUserBasicInfo"})
public SysUser getUserBasicInfo(UUID userId) {
    // Implementation
}
```

Use `@Slave` for read operations:
```java
@Slave
@Timed(value = "cross_db_query", extraTags = {"method", "findUserRoles"})
public List<Map<String, Object>> findUserRoles(UUID userId) {
    // Read from slave
}
```

Use `@Master` for write operations:
```java
@Master(reason = "Write operation must use master database")
@Transactional(rollbackFor = Exception.class)
public int batchInsertUserRoles(UUID userId, List<UUID> roleIds) {
    // Write to master
}
```

### 2. Cache Strategy

Implement cache warming:
```java
@Scheduled(cron = "0 0 * * * *")  // Hourly
public void warmCache() {
    List<UUID> activeUserIds = getActiveUserIds();
    activeUserIds.forEach(this::getUserBasicInfo);  // Populates cache
}
```

Use appropriate TTL:
```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=10000,expireAfterWrite=30m  # L1 cache
    redis:
      time-to-live: 3600000  # L2 cache: 1 hour
```

### 3. Performance Testing

Test before production:
1. Run load test with expected QPS
2. Verify P95 latency < 200ms
3. Check error rate < 0.1%
4. Monitor for 30+ minutes to detect memory leaks

### 4. Monitoring Dashboard Usage

- **Daily**: Check service health overview
- **Weekly**: Review top slowest methods, plan optimizations
- **Monthly**: Analyze trends, adjust thresholds
- **During deployments**: Watch for latency spikes or error increases

---

## Alert Channels

Configure AlertManager to send alerts to:

**Slack** (Warnings):
```yaml
receivers:
  - name: 'slack-notifications'
    slack_configs:
      - channel: '#scm-alerts'
        api_url: 'https://hooks.slack.com/services/YOUR_WEBHOOK'
```

**PagerDuty** (Critical):
```yaml
receivers:
  - name: 'pagerduty-critical'
    pagerduty_configs:
      - service_key: 'YOUR_PAGERDUTY_KEY'
```

**Email** (All):
```yaml
receivers:
  - name: 'email-alerts'
    email_configs:
      - to: 'ops@cognora-aura-platform.com'
        from: 'alerts@cognora-aura-platform.com'
```

---

## Next Steps

1. **Read Full Guide**: [CQRS_PERFORMANCE_MONITORING.md](CQRS_PERFORMANCE_MONITORING.md)
2. **Configure Alerts**: Set up AlertManager with your channels
3. **Baseline Testing**: Run load tests to establish performance baselines
4. **Custom Dashboards**: Create team-specific views in Grafana
5. **Automated Reports**: Set up weekly performance summary emails

---

## Support

- Documentation: [docs/development/](.)
- Runbooks: `https://docs.cognora-aura-platform.com/runbooks/`
- Team: Platform Team
- Slack: `#scm-monitoring`

---

**Last Updated**: 2025-01-16