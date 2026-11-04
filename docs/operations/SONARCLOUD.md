# SonarCloud 接入（路线 A：保留历史 projectKey）

## 1. 项目标识

| 项 | 值 | 是否可变 |
|---|---|---|
| `sonar.projectKey` | `scm-platform` | **不可变**（项目创建后不能改） |
| `sonar.projectName` | `Cognora Aura Platform` | 可变，只是展示名 |
| `sonar.organization` | 取自仓库变量 `vars.SONAR_ORG` | 可变 |
| `sonar.host.url` | `https://sonarcloud.io` | 可变 |

仓库改名（`scm-platform` → `cognora-aura-platform`）只改展示名，**不动 projectKey**。当前 workflow 里的三个 `-Dsonar.*` 就是这个口径，仓库改名时不需要重新导入项目。

## 2. 为什么 projectKey 不能改

SonarCloud 里项目分析历史、issue 状态（已修复/误报/接受）、豁免、质量门结果、PR 装饰、徽章链接，全部以 projectKey 为主键。改 key 在 SonarCloud 里没有 API，等价于**新建一个项目**。

若未来确实要换 key（路线 B），代价是：

1. 历史分析全部丢失，新项目首扫时所有存量代码都算 New Code，质量门可能一次性爆出大量问题；
2. Quality Gate、自定义规则、issue 的人工处置（误报/接受/分配）都要在新项目重建；
3. PR 装饰、徽章、外部看板里的 dashboard 链接要全部替换，旧链接失效；
4. 旧项目需要 archive，否则继续占用组织的项目额度。

因此：**改展示名可以，改 key 需要单独立项**。

## 3. 质量门：只看 New Code

质量门配置在 SonarCloud 项目侧（Project Settings → Quality Gate），不写进 workflow：

- 选 `Sonar way` 或自建门限，所有条件的度量范围设为 **New Code**；
- 目的是让存量债务（历史代码里的问题）不参与 PR 判定，避免每个 PR 都被存量债务直接判红；
- workflow 里**故意不加** `sonar.qualitygate.wait=true`：CI 只负责上传分析，门禁结论在 SonarCloud 侧展示，不阻塞构建。

> 如果以后要让门禁阻塞 PR，再单独加 `-Dsonar.qualitygate.wait=true`，并同步调整分支保护。

## 4. 一次性配置（精确到点击）

前置：SonarCloud 组织里必须已存在 key 为 `scm-platform` 的项目。若还没有，先在 SonarCloud 里导入本仓库（`+` → Analyze new project → GitHub），**导入时把 project key 填成 `scm-platform`**（首次分析前确定，之后不可改）。

### 4.1 SonarCloud 侧：生成分析 token

1. 登录 SonarCloud，右上角头像 → **My Account**；
2. 左侧 **Security** → **Tokens** → **Generate Tokens**；
3. Name 填 `github-actions-scm-platform`；
4. Type 选 **Project analysis token**，Project 选 `scm-platform`（最小权限：只能做分析，不能改项目设置、不能管理成员）；
5. 点 **Generate**，**立即复制** token（页面关闭后不再显示）。

### 4.2 GitHub 侧：加 Secret 与 Variable

仓库 → **Settings** → **Secrets and variables** → **Actions**：

| Tab | 操作 | Name | Value |
|---|---|---|---|
| **Secrets** | New repository secret | `SONAR_TOKEN` | 4.1 生成的 token（前后不要带空格/换行） |
| **Variables** | New repository variable | `SONAR_ORG` | SonarCloud 组织 key（组织页地址 `https://sonarcloud.io/organizations/<key>` 里的 `<key>`；也可在 My Account → Organizations 里看） |

两点必须注意：

- 变量名大小写完全一致（`SONAR_ORG`、`SONAR_TOKEN`），workflow 里就是这么引用的；
- `SONAR_ORG` 是 **Variable** 不是 Secret（它出现在日志里也无害），`SONAR_TOKEN` 必须是 **Secret**。

### 4.3 可选：PR 装饰

在 SonarCloud 组织里安装 **SonarCloud GitHub App**，PR 才会出现 Sonar 评论与检查项；没装也能跑分析，只是 PR 上看不到汇总。

## 5. 配完后的验证判据

1. 随便触发一次 PR（或重跑 PR 的 **Code Quality Check** job）；
2. Actions 页面里定位 job **`Code Quality Check`**（不是 `Build and Test`），进入后看 step **`SonarCloud Scan`**：
   - 配置前：该 step 显示 **skipped**（`if: vars.SONAR_ORG != ''` 不成立），job 整体仍然是绿的；
   - 配置后：该 step **不再是 skipped**，有实际耗时（分钟级），日志里出现：
     - `ANALYSIS SUCCESSFUL`
     - `https://sonarcloud.io/dashboard?id=scm-platform`
3. 打开 `https://sonarcloud.io/dashboard?id=scm-platform`，Activity 里有本次 commit SHA 的分析记录；
4. `Code Quality Check` 这个 **job 名**一直在必检列表里、结论为 success；它不会因为变量缺失而变成红或消失。

## 6. 当前状态

- workflow 已按路线 A 固定 `projectKey` / 展示名 / 组织变量，并保留 `if: vars.SONAR_ORG != ''` 守卫；
- `SONAR_TOKEN` 与 `SONAR_ORG` **尚未添加**（值不在仓库里，需要仓库管理员手动操作）；
- 在变量补齐之前，**不要**手工触发空扫描——守卫会让它直接跳过，没必要制造一次没有意义（且 token 缺失时必然失败）的运行。
