# Helm Charts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a minimal Helm chart for deploying the SCM Platform to Kubernetes, including namespace, configmap, and secrets.

**Architecture:** Replace existing Helm chart files with a simplified version that focuses on core Kubernetes resources. The chart will contain Chart.yaml, values.yaml, and templates for namespace, configmap, and secrets.

**Tech Stack:** Helm 3, Kubernetes manifests, YAML.

---

### Task 1: Create Chart.yaml

**Files:**
- Create: `deploy/helm/scm-platform/Chart.yaml`

- [ ] **Step 1: Write Chart.yaml**

```yaml
apiVersion: v2
name: scm-platform
description: SCM Platform Helm Chart
type: application
version: 0.1.0
appVersion: "1.0.0"
```

- [ ] **Step 2: Verify Chart.yaml exists**

Run: `Test-Path -Path "deploy/helm/scm-platform/Chart.yaml"`
Expected: `True`

- [ ] **Step 3: Commit**

```bash
git add deploy/helm/scm-platform/Chart.yaml
git commit -m "feat: create Chart.yaml for Helm chart"
```

### Task 2: Create values.yaml

**Files:**
- Create: `deploy/helm/scm-platform/values.yaml`

- [ ] **Step 1: Write values.yaml**

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

- [ ] **Step 2: Verify values.yaml exists**

Run: `Test-Path -Path "deploy/helm/scm-platform/values.yaml"`
Expected: `True`

- [ ] **Step 3: Commit**

```bash
git add deploy/helm/scm-platform/values.yaml
git commit -m "feat: create values.yaml for Helm chart"
```

### Task 3: Create templates

**Files:**
- Create: `deploy/helm/scm-platform/templates/namespace.yaml`
- Create: `deploy/helm/scm-platform/templates/configmap.yaml`
- Create: `deploy/helm/scm-platform/templates/secrets.yaml`

- [ ] **Step 1: Create templates directory**

Run: `New-Item -ItemType Directory -Path "deploy/helm/scm-platform/templates" -Force`

- [ ] **Step 2: Write namespace.yaml**

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: {{ .Values.namespace }}
```

- [ ] **Step 3: Write configmap.yaml**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: scm-config
  namespace: {{ .Values.namespace }}
data:
  NACOS_SERVER: {{ .Values.env.nacosServer }}
  DB_HOST: {{ .Values.env.dbHost }}
  DB_PORT: {{ .Values.env.dbPort }}
  REDIS_HOST: {{ .Values.env.redisHost }}
  REDIS_PORT: {{ .Values.env.redisPort }}
```

- [ ] **Step 4: Write secrets.yaml**

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: scm-secrets
  namespace: {{ .Values.namespace }}
type: Opaque
data:
  DB_USERNAME: YWRtaW4=  # base64 of 'admin'
  DB_PASSWORD: YWRtaW4xMjM=  # base64 of 'admin123'
  REDIS_PASSWORD: ""  # empty for dev
```

- [ ] **Step 5: Verify templates exist**

Run: `Get-ChildItem -Path "deploy/helm/scm-platform/templates" -Filter *.yaml | Measure-Object | Select-Object -ExpandProperty Count`
Expected: `3`

- [ ] **Step 6: Commit**

```bash
git add deploy/helm/scm-platform/templates/
git commit -m "feat: add Helm templates for namespace, configmap, secrets"
```

### Task 4: Final commit

- [ ] **Step 1: Stage all changes**

```bash
git add deploy/helm/
```

- [ ] **Step 2: Commit with final message**

```bash
git commit -m "feat: implement Helm charts for Kubernetes deployment"
```

---

**Self-Review:**
1. Spec coverage: All required files created.
2. Placeholder scan: No placeholders; all content provided.
3. Type consistency: Consistent naming and structure.

**Execution Handoff:**
Plan complete and saved to `docs/superpowers/plans/2026-06-12-helm-charts.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?