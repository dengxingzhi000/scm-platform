# 告警链路：Prometheus → Alertmanager

本文件说明审计与平台告警如何真正外发、如何配置通知通道，以及如何验证规则确实触发。

## 1. 组件与文件

```text
业务服务 /actuator/prometheus
        │  (scrape，见 config/prometheus/prometheus.yml)
        ▼
   Prometheus  ── 每 15s 求值 config/prometheus/alert-rules.yml
        │  (alerting.alertmanagers → alertmanager:9093)
        ▼
   Alertmanager ── 分组 / 去重 / 抑制 / 路由 → webhook（或 null）
```

| 文件 | 作用 |
|---|---|
| `config/prometheus/prometheus.yml` | 抓取目标、`rule_files`、`alerting.alertmanagers` |
| `config/prometheus/alert-rules.yml` | 告警规则（含 `security-audit-alerts` 组） |
| `config/alertmanager/alertmanager.yml` | 配置了 webhook 时使用（`${ALERTMANAGER_WEBHOOK_URL}`） |
| `config/alertmanager/alertmanager-null.yml` | 未配置 webhook 时使用，`receiver: null`，不外发 |

审计组现有 5 条规则（指标名是 Micrometer 计数器转成下划线后的名字）：

| 告警 | 条件 | 级别 |
|---|---|---|
| `AuditDeliveryFailed` | 5 分钟内 `security_audit_delivery_failed_total` 增量 > 0 | warning |
| `AuditDeliveryFailedHigh` | 1 小时增量 > 10 | critical |
| `AuditWriteRejected` | 5 分钟内 `security_audit_write_rejected_total` 增量 > 0 | warning |
| `AuditWriteRejectedHigh` | 1 小时增量 > 10 | critical |
| `AuditWriteRejectedSustained` | 每个滚动 15 分钟窗口都有被拒，且持续 15 分钟 | warning |

`AuditWriteRejectedSustained` 的 `for: 15m` + `increase(...[15m]) > 0` 组合用于区分
「持续被拒」和瞬时抖动：只要每个窗口都有新的拒绝，才会在 15 分钟后进入 firing。

## 2. 通知通道：默认不外发

`docker-compose.yml` 的 alertmanager 服务根据 `ALERTMANAGER_WEBHOOK_URL` 是否为空
选择配置文件：

- **空（默认）**：使用 `alertmanager-null.yml`，`receiver: null`，不向任何外部系统发送；
  规则照常求值，告警在 Prometheus `/alerts` 和 Alertmanager UI (`http://localhost:9093`)
  以及 API 中可见，日志不会报错。
- **非空**：使用 `alertmanager.yml`，`--config.expand-env` 把地址展开进
  `webhook_configs.url`，按下方路由推送。

不在仓库里硬编码邮箱、企业微信或任何第三方凭据；需要时只配一个接收 webhook。

### 2.1 配置 webhook

1. 在与 `docker-compose.yml` 同目录的 `.env` 里写（compose 会自动加载该文件）：

   ```bash
   ALERTMANAGER_WEBHOOK_URL=https://your-receiver.example.com/hooks/alerts
   ```

   仓库的 `.env.example` 已列出该变量，值为空。`.env` 不提交。

2. 应用配置（只重建 alertmanager，不动其它容器）：

   ```bash
   docker compose up -d alertmanager
   ```

3. 确认生效：

   ```bash
   docker compose logs --tail=20 alertmanager   # 无 "unsupported scheme" / "missing URL"
   curl -s http://localhost:9093/-/ready
   ```

Alertmanager 以标准 webhook JSON（`version: "4"`）POST，包含 `status`
（`firing` / `resolved`）、`groupLabels`、`commonLabels`、`commonAnnotations` 和
`alerts[]`；`send_resolved: true` 表示恢复时也会推送一次。接收端按
HTTP 2xx 视为成功，非 2xx 会触发 Alertmanager 重试。

## 3. 路由、去重与抑制

`alertmanager.yml` 与 `alertmanager-null.yml` 使用同一套规则：

- `group_by: [alertname, team]`：同一 `alertname` + 同一 `team` 的告警合并为一条通知，
  天然完成「同一告警组去重」；
- critical 路由 `group_wait: 10s` / `repeat_interval: 1h`，warning 为 30s / 4h；
- 抑制规则：
  - `severity="critical"` 抑制同 `alertname`、同 `team` 的 `severity="warning"`；
  - `AuditDeliveryFailedHigh` / `AuditWriteRejectedHigh` 额外按 `team` 抑制
    `AuditDeliveryFailed` / `AuditWriteRejected`（两者的 alertname 不同，靠第一条抑制不到）。

## 4. 验证规则真的触发

### 4.1 直接对 Alertmanager 注入告警（最快，验证路由/抑制/外发）

不依赖任何业务数据，适合在配好 webhook 后确认通道打通：

```bash
# critical：可以观察 10s 后是否到达 webhook
curl -s -XPOST http://localhost:9093/api/v2/alerts \
  -H 'Content-Type: application/json' \
  -d '[{
    "labels": {"alertname": "AuditWriteRejectedHigh", "severity": "critical", "team": "audit"},
    "annotations": {"summary": "手工验证：critical"}
  }]'

# 同组 warning：应被上面的 critical 抑制，不出现在发出的通知里
curl -s -XPOST http://localhost:9093/api/v2/alerts \
  -H 'Content-Type: application/json' \
  -d '[{
    "labels": {"alertname": "AuditWriteRejected", "severity": "warning", "team": "audit"},
    "annotations": {"summary": "手工验证：warning"}
  }]'

# 查看当前告警及抑制状态（inhibitedBy 非空即被抑制）
curl -s http://localhost:9093/api/v2/alerts | jq '.[] | {labels, status}'
```

- 未配 webhook 时：两条告警都能在 `/api/v2/alerts` 查到，但不会有任何外发，符合预期；
- 配了 webhook 时：critical 在 `group_wait: 10s` 后发出，warning 的
  `status.inhibitedBy` 指向 critical；
- 验证结束可删除注入的告警（按 label 过滤，不传时它们会自行过期）：

  ```bash
  curl -s -XDELETE http://localhost:9093/api/v2/alerts \
    -H 'Content-Type: application/json' \
    -d '[{"name": "alertname", "value": "AuditWriteRejectedHigh"}]'
  ```

### 4.2 走真实指标：让审计写入被拒

`security_audit_write_rejected_total` 在 scm-audit 上，由
`AuditWriteRequestGuard` 对未签名写入计数。向审计写入口发未签名请求即可制造拒绝：

```bash
# 期望每次都返回 403
for i in $(seq 1 5); do
  curl -s -o /dev/null -w '%{http_code}\n' -XPOST http://localhost:8210/sys-audit-log \
    -H 'X-Tenant-Id: 019a0aee-0000-7000-8000-000000000002' \
    -H 'Content-Type: application/json' \
    -d '{"operationType":"MANUAL_ALERT_TEST"}'
done

# 指标是否增长
curl -s http://localhost:8210/actuator/prometheus | grep security_audit_write_rejected_total

# Prometheus 中是否可见
curl -s 'http://localhost:9090/api/v1/query?query=security_audit_write_rejected_total' \
  | jq '.data.result'
```

规则状态（`state` 为 `pending` / `firing`）：

```bash
curl -s http://localhost:9090/api/v1/rules \
  | jq '.data.groups[] | select(.name=="security-audit-alerts") | .rules[] | {name, state, health}'
```

说明：

- 第一次拒绝后约一个求值周期（15s），`AuditWriteRejected`（5 分钟窗口）即进入 firing；
- `AuditWriteRejectedSustained` 要求连续 15 分钟每个窗口都有拒绝，手工验证时需持续
  （例如每分钟发一次）至少 16 分钟，否则会停留在 pending 并退出；只想验证规则语法与
  路由，用 4.1 的注入方式即可；
- 修改 `alert-rules.yml` 后无需重启 Prometheus（compose 已开 `--web.enable-lifecycle`）：

  ```bash
  curl -s -XPOST http://localhost:9090/-/reload
  ```

## 5. 未配置通道时的预期行为

- alertmanager 容器正常启动，使用 `alertmanager-null.yml`，不报错；
- Prometheus 仍把 firing/resolved 状态推给 Alertmanager，`/api/v2/alerts`、UI 与
  `ALERTS` 指标都正常，只是没有外部通知；
- 因此「规则有效」与「有人收到通知」是两件独立的事：只验证 4.2 的规则状态即可确认
  告警链路前半段，验证 webhook 需再配 4.1；
- 若日志出现 `unsupported scheme ""` 或 `missing URL`，说明
  `ALERTMANAGER_WEBHOOK_URL` 被设置成了无效值（例如只写了 `https://`），修好后
  `docker compose up -d alertmanager` 重建。

## 6. 生效方式

本轮改动只落在文件里，**没有重启任何正在运行的容器**。下次执行
`docker compose up -d`（或单独 `docker compose up -d alertmanager`）时生效；
`config/prometheus/*` 的变更需要 Prometheus 重载或重建后生效。
