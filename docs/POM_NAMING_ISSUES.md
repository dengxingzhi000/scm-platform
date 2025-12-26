# POM文件命名规范分析报告

## 📋 分析日期
2025-12-25

## ✅ 已统一的模块

以下模块的命名规范已经统一且正确:

### 业务服务 (完全符合规范)
所有业务服务的POM命名都遵循了统一规范:

| 服务 | Parent | API模块 | Service模块 |
|------|--------|---------|-------------|
| scm-product | ✅ scm-product | ✅ scm-product-api | ✅ scm-product-service |
| scm-inventory | ✅ scm-inventory | ✅ scm-inventory-api | ✅ scm-inventory-service |
| scm-order | ✅ scm-order | ✅ scm-order-api | ✅ scm-order-service |
| scm-warehouse | ✅ scm-warehouse | ✅ scm-warehouse-api | ✅ scm-warehouse-service |
| scm-logistics | ✅ scm-logistics | ✅ scm-logistics-api | ✅ scm-logistics-service |
| scm-supplier | ✅ scm-supplier | ✅ scm-supplier-api | ✅ scm-supplier-service |
| scm-purchase | ✅ scm-purchase | ✅ scm-purchase-api | ✅ scm-purchase-service |
| scm-finance | ✅ scm-finance | ✅ scm-finance-api | ✅ scm-finance-service |
| scm-approval | ✅ scm-approval | ✅ scm-approval-api | ✅ scm-approval-service |
| scm-audit | ✅ scm-audit | ✅ scm-audit-api | ✅ scm-audit-service |
| scm-notify | ✅ scm-notify | ✅ scm-notify-api | ✅ scm-notify-service |
| scm-tenant | ✅ scm-tenant | ✅ scm-tenant-api | ✅ scm-tenant-service |

**规范:**
- Parent: `groupId=com.frog`, `artifactId=scm-{service}`
- API: `groupId=com.frog`, `artifactId=scm-{service}-api`
- Service: `groupId=com.frog`, `artifactId=scm-{service}-service`

---

## ❌ 需要修复的命名不一致问题

### 1. scm-common 模块 (高优先级)

#### 问题：Parent引用错误
```xml
<!-- 当前 (错误) -->
<parent>
    <groupId>com</groupId>
    <artifactId>NewNearSync</artifactId>
    <version>1.0-SNAPSHOT</version>
    <relativePath>../../pom.xml</relativePath>
</parent>
```

**影响模块:**
- ❌ scm-common/core/pom.xml
- ❌ scm-common/data/pom.xml
- ❌ scm-common/integration/pom.xml
- ❌ scm-common/monitoring/pom.xml
- ❌ scm-common/security-api/pom.xml
- ❌ scm-common/web/pom.xml

**应修改为:**
```xml
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
```

#### 问题：artifactId缺少统一前缀
```xml
<!-- 当前 (不规范) -->
<artifactId>core</artifactId>
<artifactId>data</artifactId>
<artifactId>web</artifactId>
<artifactId>integration</artifactId>
<artifactId>monitoring</artifactId>
<artifactId>security-api</artifactId>
```

**应修改为:**
```xml
<artifactId>scm-common-core</artifactId>
<artifactId>scm-common-data</artifactId>
<artifactId>scm-common-web</artifactId>
<artifactId>scm-common-integration</artifactId>
<artifactId>scm-common-monitoring</artifactId>
<artifactId>scm-common-security-api</artifactId>
```

**注意:** 这个修改会影响所有引用这些模块的地方!

---

### 2. scm-auth 模块

#### 问题：groupId不一致
```xml
<!-- 当前 (不规范) -->
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-platform</artifactId>
</parent>
<groupId>com.frog.auth</groupId>
<artifactId>scm-auth</artifactId>
```

**应修改为:**
```xml
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-platform</artifactId>
</parent>
<!-- 直接继承父POM的groupId，不需要自定义 -->
<artifactId>scm-auth</artifactId>
```

#### 问题：name首字母小写
```xml
<!-- 当前 (不规范) -->
<name>SCM auth</name>
```

**应修改为:**
```xml
<name>SCM Auth Service</name>
```

#### 问题：依赖引用使用旧的artifactId
```xml
<!-- 当前 (错误) -->
<dependency>
    <groupId>com.frog.common</groupId>
    <artifactId>core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**应修改为:**
```xml
<dependency>
    <groupId>com.frog</groupId>
    <artifactId>scm-common-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

### 3. scm-gateway 模块

#### 问题：name首字母小写
```xml
<!-- 当前 (不规范) -->
<name>SCM gateway</name>
```

**应修改为:**
```xml
<name>SCM Gateway</name>
```

#### 问题：依赖引用使用旧的groupId和artifactId
```xml
<!-- 当前 (错误) -->
<dependency>
    <groupId>com.frog.common</groupId>
    <artifactId>web</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**应修改为:**
```xml
<dependency>
    <groupId>com.frog</groupId>
    <artifactId>scm-common-web</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

### 4. scm-system 模块

#### 问题：Parent引用错误
```xml
<!-- 当前 (错误) -->
<parent>
    <groupId>com</groupId>
    <artifactId>NewNearSync</artifactId>
    <version>1.0-SNAPSHOT</version>
</parent>
```

**应修改为:**
```xml
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
```

#### 问题：groupId自定义
```xml
<!-- 当前 (不规范) -->
<groupId>com.frog.system</groupId>
<artifactId>system</artifactId>
```

**应修改为:**
```xml
<!-- 继承父POM的groupId -->
<artifactId>scm-system</artifactId>
```

#### 问题：scm-system/api/pom.xml artifactId不规范
```xml
<!-- 当前 (不规范) -->
<groupId>com.frog.system</groupId>
<artifactId>api</artifactId>
<name>api</name>
```

**应修改为:**
```xml
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-system</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
<artifactId>scm-system-api</artifactId>
<name>SCM System API</name>
```

---

### 5. scm-common/web/securityCore 模块

#### 问题：artifactId不一致
```xml
<!-- 当前 (不规范) -->
<groupId>com.frog.common.web</groupId>
<artifactId>scm-securityCore</artifactId>
<name>securityCore</name>
```

**应修改为:**
```xml
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
<artifactId>scm-common-security-core</artifactId>
<name>SCM Common Security Core</name>
```

---

## 📊 统计摘要

### 符合规范的模块数量
- ✅ **完全符合**: 36个 (12个业务服务 × 3个pom)
- ❌ **需要修复**: 13个

### 主要问题类型
1. **Parent引用错误**: 7个文件 (scm-common子模块 + scm-system)
2. **GroupId不统一**: 10个文件
3. **ArtifactId缺少前缀**: 7个文件 (scm-common子模块)
4. **Name命名不规范**: 9个文件
5. **版本号不统一**: 所有scm-common相关引用 (1.0-SNAPSHOT vs 1.0.0-SNAPSHOT)

---

## 🎯 统一的命名规范 (建议采用)

### 1. 根POM
```xml
<groupId>com.frog</groupId>
<artifactId>scm-platform</artifactId>
<version>1.0.0-SNAPSHOT</version>
<name>SCM Platform</name>
```

### 2. 公共模块 (scm-common)
```xml
<!-- Parent -->
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
<artifactId>scm-common</artifactId>
<packaging>pom</packaging>
<name>SCM Common</name>

<!-- 子模块 -->
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
<artifactId>scm-common-{module}</artifactId>
<packaging>jar</packaging>
<name>SCM Common {Module}</name>
```

### 3. 基础设施服务 (Gateway, Auth)
```xml
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
<artifactId>scm-{service}</artifactId>
<packaging>jar</packaging>
<name>SCM {Service}</name>
```

### 4. 业务服务 (Product, Order等)
```xml
<!-- Parent POM -->
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
<artifactId>scm-{service}</artifactId>
<packaging>pom</packaging>
<name>SCM {Service} Service</name>

<!-- API模块 -->
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-{service}</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
<artifactId>scm-{service}-api</artifactId>
<packaging>jar</packaging>
<name>SCM {Service} API</name>

<!-- Service模块 -->
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-{service}</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
<artifactId>scm-{service}-service</artifactId>
<packaging>jar</packaging>
<name>SCM {Service} Service Implementation</name>
```

### 5. 版本号规范
- 统一使用: `1.0.0-SNAPSHOT`
- 不使用: `1.0-SNAPSHOT`

---

## 🔧 修复建议优先级

### 高优先级 (会导致编译失败)
1. ✅ scm-common子模块的parent引用
2. ✅ scm-system的parent引用
3. ✅ 所有依赖中的artifactId引用 (core -> scm-common-core)

### 中优先级 (影响一致性)
1. scm-auth的groupId
2. scm-system的artifactId
3. scm-common子模块的artifactId前缀

### 低优先级 (仅影响可读性)
1. name字段的大小写统一
2. 版本号格式统一

---

## 📝 修复步骤建议

### 第一步: 修复scm-common模块 (最重要)

1. 创建scm-common/pom.xml作为parent
2. 更新所有scm-common子模块的parent引用
3. 更新所有子模块的artifactId加上scm-common-前缀
4. 批量更新所有依赖引用

### 第二步: 修复scm-system和scm-auth

1. 修改scm-system的parent和artifactId
2. 修改scm-auth的groupId
3. 更新相关依赖引用

### 第三步: 统一name和version格式

1. 批量修改name首字母大写
2. 统一版本号为1.0.0-SNAPSHOT

---

## ⚠️ 注意事项

1. **批量修改影响**: 修改artifactId会影响所有引用这些模块的dependency声明
2. **IDE刷新**: 修改后需要在IDE中刷新Maven项目
3. **构建测试**: 每次修改后都应该执行 `mvn clean install` 确保构建成功
4. **Git提交**: 建议每完成一个模块的修改就提交一次，便于回滚

---

## 📌 示例：修复scm-common-core

### Before:
```xml
<!-- scm-common/core/pom.xml -->
<parent>
    <groupId>com</groupId>
    <artifactId>NewNearSync</artifactId>
    <version>1.0-SNAPSHOT</version>
    <relativePath>../../pom.xml</relativePath>
</parent>
<groupId>com.frog.common</groupId>
<artifactId>core</artifactId>
<name>core</name>
```

### After:
```xml
<!-- scm-common/core/pom.xml -->
<parent>
    <groupId>com.frog</groupId>
    <artifactId>scm-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
<artifactId>scm-common-core</artifactId>
<name>SCM Common Core</name>
```

### 同时需要修改所有引用:
```xml
<!-- Before -->
<dependency>
    <groupId>com.frog.common</groupId>
    <artifactId>core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- After -->
<dependency>
    <groupId>com.frog</groupId>
    <artifactId>scm-common-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
