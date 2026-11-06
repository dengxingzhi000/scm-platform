# minimatch ReDoS 风险接受说明

> 结论：`minimatch` 的 10.x 线没有修复版本，`11.0.1` 是公告覆盖的唯一修复版本。该依赖只存在于 `scm-web` 的开发期 lint 工具链，不进入部署产物，按 `not_used` 接受风险。2026-11-06 04:29，PR #352 将 `scm-web` 移出本仓库后，`scm-web/package-lock.json` 不再存在，告警 #95 自动转为 `fixed`；本说明保留判定依据与复评条件，等价的告警处置记录见 `cognora-aura-web` 的同名文档。

## 1. 公告事实

| 项 | 值 |
|---|---|
| GHSA | [GHSA-g3hv-75wg-m249](https://github.com/advisories/GHSA-g3hv-75wg-m249) |
| CVE | CVE-2026-118764 |
| 标题 | minimatch has potential denial of service via globparts amplification |
| 等级 | medium（公告未给出 CVSS 分值） |
| 发布时间 | 2026-10-30 |
| 受影响版本 | `< 11.0.1` |
| 最低修复版本 | **11.0.1** |
| Dependabot 告警 | #95，manifest `scm-web/package-lock.json`，scope **development**，受影响区间 `>= 7.0.0, < 11.0.1` |

关键判断：公告的修复版本只有 11.0.1，**10.x 线没有任何修复版本**。因此「把嵌套 10.x 升到该公告覆盖的修复版」在不动大版本的前提下无法完成。

## 2. 仓库内的实际暴露

`scm-web/package-lock.json` 中命中：

| 版本 | 位置 |
|---|---|
| 10.2.6 | `node_modules/eslint/node_modules/minimatch` |
| 10.2.6 | `node_modules/@eslint/config-array/node_modules/minimatch` |
| 10.2.6 | `node_modules/eslint-config-next/node_modules/typescript-eslint/node_modules/minimatch` |
| 3.1.5 | 顶层 `node_modules/minimatch`（`glob` 等传递依赖） |

上游声明的区间是 `^10.x`（如 `@eslint/config-array` 的 `^10.2.4`），override 到 11 会越出该区间，属于未经上游验证的强制替换；用户口径也明确不这么做。

> 2026-11-06 更新：上述 manifest 已随 PR #352（`chore: remove the extracted scm-web frontend from the monorepo`）移出本仓库，本仓库当前不再包含该依赖；告警 #95 于同日 04:29 转为 `fixed`。

## 3. 为什么判定为可接受（当前形态）

1. **只在开发链**：四份副本分别由 `eslint`、`@eslint/config-array`、`typescript-eslint` 引入，都是 `devDependencies`；Dependabot 亦标注 `scope: development`。
2. **不进运行期产物**：生产镜像与 `scm-web` 的运行产物不包含 eslint 工具链，该代码不会被部署。
3. **触发条件不可达**：公告描述的是 glob **模式串**被放大（globparts amplification）导致的拒绝服务。本仓库里 minimatch 的输入是 `.eslintrc`/`eslint.config.mjs` 中的静态 glob，不来自外部请求或用户输入。
4. **权限不增益**：能改这些 glob 的人本来就能改 CI 里的任意代码，不构成额外攻击面。

## 4. 处置结果

- **aura-platform**：告警 #95 以 `not_used` 作为处置理由 —— 该依赖仅存在于开发期 lint 工具链、不进入部署产物；受影响输入（glob 模式串）不由外部控制；且 10.x 线无修复版本，强制升级到 11.x 会越出上游声明的依赖区间。2026-11-06 04:29，PR #352 将 `scm-web` 移出 monorepo 后 manifest 消失，该告警自动转为 `fixed`。
- **cognora-aura-web**：同一公告的等价告警 #3 已按同一理由 `not_used` dismiss，风险接受文档随该仓库维护（`docs/security/MINIMATCH_RISK_ACCEPTANCE.md`）。
- 若倾向彻底消除：需要等 `eslint` / `typescript-eslint` 上游升级到 minimatch 11 后随工具链整体升级，而不是单独 override。

## 5. 复评触发条件

- 工具链升级到 ESLint 11 / typescript-eslint 支持 minimatch 11 时；
- 仓库出现「由外部输入构造 glob 模式串」的代码路径时；
- 公告被修订（新增 10.x 修复版本或调整受影响区间）时；
- `scm-web` 被重新并入本仓库（manifest 再次出现）时。

核对命令与日期：

```bash
gh api /repos/dengxingzhi000/cognora-aura-platform/dependabot/alerts \
  --paginate --jq '.[] | select(.dependency.package.name=="minimatch")'
```

核对时间：2026-11-06（告警 #95 已随 `scm-web` 移出转为 `fixed`）。
