#!/usr/bin/env node
/**
 * 抓取运行中后端服务的 OpenAPI (Swagger) 文档。
 *
 * 用法:
 *   node scripts/api/fetch-openapi.mjs                       # 抓取默认主机
 *   node scripts/api/fetch-openapi.mjs 192.168.0.125         # 指定主机
 *   node scripts/api/fetch-openapi.mjs 192.168.0.125 --out docs/api
 *
 * 说明:
 *   - 服务把 OpenAPI 文档发布在 /api/public/v3/api-docs (见
 *     scm-common/web OpenApiPropertiesEnvironmentPostProcessor)。
 *   - 请求必须带 X-Tenant-Id 头: TenantFilter 在 scm.tenant.required=true 时
 *     缺少租户上下文会抛 TenantParseException, 表现为 401/500。
 *   - Swagger UI 默认关闭 (springdoc.swagger-ui.enabled=false), 所以只有 JSON 文档。
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const DEFAULT_HOST = '192.168.0.125';
const TENANT_ID = '11111111-1111-1111-1111-111111111111';

/**
 * The deployed services still publish the security scheme as "Bearer Authentication".
 * The OpenAPI spec restricts `components.securitySchemes` keys to `^[a-zA-Z0-9.\-_]+$`,
 * and openapi-generator refuses such a document. Newer builds of OpenApiConfig use
 * "BearerAuthentication"; this normalises the legacy name so the fetched spec is
 * consumable either way.
 */
const LEGACY_SECURITY_SCHEME = 'Bearer Authentication';
const SECURITY_SCHEME = 'BearerAuthentication';

/**
 * The controllers declare Chinese `@Tag` values ("订单管理", "用户管理", …).
 * openapi-generator derives the generated API class name from the tag and cannot
 * sanitise a CJK string into an ASCII identifier — it collapses every operation
 * of a service into a single `DefaultApi` class with `getById1`/`getById2` style
 * names. Mapping the tags to English restores one API class per functional area.
 */
const TAG_ALIASES = {
  // scm-auth
  '用户认证': 'Auth',
  'WebAuthn 认证': 'WebAuthnAuthentication',
  'WebAuthn 注册': 'WebAuthnRegistration',
  'WebAuthn 凭证': 'WebAuthnCredential',
  'OAuth2 登出': 'OAuth2Logout',
  // scm-product
  'SPU 管理': 'Spu',
  'SKU 管理': 'Sku',
  '商品分类': 'Category',
  '品牌管理': 'Brand',
  '属性模板': 'AttributeTemplate',
  '商品搜索': 'ProductSearch',
  // scm-inventory
  '库存管理': 'Inventory',
  '库存预占': 'InventoryReservation',
  // scm-order
  '订单管理': 'Order',
  '订单明细': 'OrderItem',
  '订单支付': 'Payment',
  '订单退款': 'Refund',
  '订单状态历史': 'OrderStatusHistory',
  // scm-system
  '用户管理': 'User',
  '角色管理': 'Role',
  '部门管理': 'Dept',
  '权限管理': 'Permission',
  '数据字典': 'Dict',
  '状态字典': 'StatusDict',
  '决策规则': 'DecisionRule',
  '决策权重': 'DecisionWeight',
};

const SERVICES = [
  { name: 'scm-system', port: 8081 },
  { name: 'scm-auth', port: 8106 },
  { name: 'scm-product', port: 8201 },
  { name: 'scm-inventory', port: 8202 },
  { name: 'scm-order', port: 8203 },
];

// The repo root is the parent of `scripts/`, so the default output lands in
// `<repo>/docs/api` no matter which directory the script is invoked from.
const REPO_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');

const args = process.argv.slice(2);
const host = args.find((a) => !a.startsWith('--')) ?? DEFAULT_HOST;
const outIndex = args.indexOf('--out');
const outDir = path.resolve(
  REPO_ROOT,
  outIndex >= 0 ? args[outIndex + 1] : 'docs/api'
);
fs.mkdirSync(outDir, { recursive: true });

let failed = 0;
const index = [];

for (const svc of SERVICES) {
  const url = `http://${host}:${svc.port}/api/public/v3/api-docs`;
  const target = path.join(outDir, `openapi-${svc.name}.json`);
  try {
    const res = await fetch(url, { headers: { 'X-Tenant-Id': TENANT_ID } });
    if (!res.ok) {
      failed++;
      console.error(`FAIL ${svc.name} :${svc.port} -> HTTP ${res.status}`);
      continue;
    }
    const text = await res.text();
    const doc = JSON.parse(text); // 校验并失败即退出
    normaliseSecurityScheme(doc);
    aliasTags(doc);
    fs.writeFileSync(target, JSON.stringify(doc, null, 2), 'utf8');
    const pathCount = Object.keys(doc.paths ?? {}).length;
    const schemaCount = Object.keys(doc.components?.schemas ?? {}).length;
    console.log(
      `OK   ${svc.name} :${svc.port} -> ${pathCount} paths, ${schemaCount} schemas -> ${target}`
    );
    index.push({
      service: svc.name,
      port: svc.port,
      title: doc.info?.title,
      paths: pathCount,
      schemas: schemaCount,
      doc,
    });
  } catch (err) {
    failed++;
    console.error(`FAIL ${svc.name} :${svc.port} -> ${err.message}`);
  }
}

if (index.length > 0) {
  writeSummaries(index, outDir);
}

process.exit(failed === 0 ? 0 : 1);

/**
 * Rename the legacy "Bearer Authentication" security scheme to the spec-compliant
 * "BearerAuthentication", keeping every reference in sync.
 */
function normaliseSecurityScheme(doc) {
  const schemes = doc.components?.securitySchemes;
  if (!schemes || !(LEGACY_SECURITY_SCHEME in schemes)) return;

  schemes[SECURITY_SCHEME] = schemes[LEGACY_SECURITY_SCHEME];
  delete schemes[LEGACY_SECURITY_SCHEME];

  const requirements = doc.security ?? [];
  for (const requirement of requirements) {
    if (LEGACY_SECURITY_SCHEME in requirement) {
      requirement[SECURITY_SCHEME] = requirement[LEGACY_SECURITY_SCHEME];
      delete requirement[LEGACY_SECURITY_SCHEME];
    }
  }
}

/**
 * Replace the Chinese controller tags with their English aliases so
 * openapi-generator emits one API class per functional area.
 */
function aliasTags(doc) {
  for (const item of Object.values(doc.paths ?? {})) {
    for (const op of Object.values(item)) {
      if (!Array.isArray(op.tags)) continue;
      op.tags = op.tags.map((tag) => TAG_ALIASES[tag] ?? tag);
    }
  }
}

/** Write a human-readable Markdown digest per service plus an index. */
function writeSummaries(entries, dir) {
  for (const entry of entries) {
    const { doc, service } = entry;
    const lines = [`# ${service} — OpenAPI 文档摘要`, ''];
    lines.push(`- 来源: \`http://${host}:${entry.port}/api/public/v3/api-docs\``);
    lines.push(`- 标题: ${doc.info?.title} (${doc.info?.version})`);
    lines.push('- 认证: Bearer JWT (Authorization header)');
    lines.push('');

    const byTag = new Map();
    for (const [p, item] of Object.entries(doc.paths ?? {})) {
      for (const [method, op] of Object.entries(item)) {
        if (!['get', 'post', 'put', 'delete', 'patch'].includes(method)) continue;
        const tag = (op.tags ?? ['ungrouped'])[0];
        if (!byTag.has(tag)) byTag.set(tag, []);
        byTag.get(tag).push({ p, method, op });
      }
    }

    for (const [tag, ops] of byTag) {
      lines.push(`## ${tag}`, '');
      for (const { p, method, op } of ops) {
        const params = (op.parameters ?? []).map((x) => {
          const t = x.schema?.type ?? (x.schema?.format ? x.schema.format : '?');
          return `\`${x.name}\`(${x.in}${x.required ? ',必填' : ''}: ${t})`;
        });
        const bodySchema = op.requestBody?.content?.['application/json']?.schema?.['$ref']?.split('/').pop();
        const respSchema = op.responses?.['200']?.content?.['*/*']?.schema?.['$ref']?.split('/').pop();
        lines.push(`### ${method.toUpperCase()} ${p}`);
        lines.push(`- 说明: ${op.summary ?? ''}`);
        if (op.description && op.description !== op.summary) {
          lines.push(`- 描述: ${op.description}`);
        }
        if (params.length) lines.push(`- 参数: ${params.join(' ')}`);
        if (bodySchema) lines.push(`- 请求体: \`${bodySchema}\``);
        lines.push(`- 响应: \`${respSchema ?? Object.keys(op.responses ?? {}).join(',')}\``);
        lines.push('');
      }
    }

    lines.push('## Schemas', '');
    for (const [name, schema] of Object.entries(doc.components?.schemas ?? {})) {
      const props = schema.properties
        ? Object.entries(schema.properties)
            .map(([k, v]) => {
              const t =
                v.$ref?.split('/').pop() ||
                (v.type === 'array'
                  ? `${v.items?.$ref?.split('/').pop() ?? v.items?.type ?? 'any'}[]`
                  : v.type ?? 'any');
              return `\`${k}\`: ${t}`;
            })
            .join(', ')
        : '';
      lines.push(`- **${name}** — ${props}`);
    }

    fs.writeFileSync(path.join(dir, `${service}.md`), lines.join('\n'), 'utf8');
  }

  const indexLines = [
    '# 后端接口文档 (OpenAPI)',
    '',
    '由运行中的服务实时抓取。',
    '',
    '抓取方式：请求 `/api/public/v3/api-docs` 并附带 `X-Tenant-Id` 头',
    '（多租户过滤器要求租户上下文，否则返回 401/500）。',
    '',
    '```bash',
    'node scripts/api/fetch-openapi.mjs <host> --out docs/api',
    '```',
    '',
    '| 服务 | 端口 | 路径数 | Schema 数 | 摘要 | 原始 JSON |',
    '|------|------|--------|-----------|------|-----------|',
    ...entries.map(
      (i) =>
        `| ${i.service} | ${i.port} | ${i.paths} | ${i.schemas} | ` +
        `[${i.service}.md](${i.service}.md) | ` +
        `[openapi-${i.service}.json](openapi-${i.service}.json) |`
    ),
    '',
    '## 网关路由',
    '',
    '网关 (`:8761`) 只为 `auth` / `system` / `common` 配置了显式路由 (`StripPrefix=1`)，',
    '其余服务通过 Nacos 服务发现定位器以 `/scm-<service>/**` 访问：',
    '',
    '```',
    '/auth/api/auth/**            -> scm-auth      (:8106)',
    '/system/api/system/**        -> scm-system   (:8081)',
    '/scm-product/api/v1/**       -> scm-product  (:8201)',
    '/scm-inventory/api/v1/**     -> scm-inventory(:8202)',
    '/scm-order/api/v1/**         -> scm-order    (:8203)',
    '```',
    '',
    '## 生成 TypeScript 客户端',
    '',
    '`scm-web/openapitools.json` 读取本目录的 JSON，生成 `src/lib/api/generated/`：',
    '',
    '```bash',
    'cd scm-web && npm run generate:api',
    '```',
    '',
    '生成结果按功能域拆分为独立 API 类（`OrderApi`、`UserApi`、`SpuApi` …），',
    '代码只读，请勿手工修改——重新执行上面的命令即可刷新。',
    '',
    '## 已知问题（抓取脚本已做归一化）',
    '',
    '部署中的服务有两处不符合 OpenAPI 规范，`fetch-openapi.mjs` 在写盘前修正：',
    '',
    '| 问题 | 原因 | 归一化 |',
    '|------|------|--------|',
    '| `components.securitySchemes` 的键为 `"Bearer Authentication"` | 键名必须匹配 `^[a-zA-Z0-9.\\-_]+$`，openapi-generator 直接拒绝该文档 | 改为 `BearerAuthentication` |',
    '| 每个操作的 `tags` 是中文（"订单管理"、"用户管理" …） | 生成器用 tag 推导 API 类名，无法把 CJK 字符串转成 ASCII 标识符，于是整个服务的操作被合并进一个 `DefaultApi`，方法名退化成 `getById1`/`getById2` | 映射为英文别名（`Order`、`User` …） |',
    '',
    '`scm-common/web` 的 `OpenApiConfig` 已把 security scheme 名改成',
    '`BearerAuthentication`，重新部署后不再需要脚本兜底。',
    '',
  ];
  fs.writeFileSync(path.join(dir, 'README.md'), indexLines.join('\n'), 'utf8');
}
