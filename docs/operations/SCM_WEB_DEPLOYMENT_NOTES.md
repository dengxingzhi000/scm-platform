# scm-web 前端测试环境部署说明

适用主机：`192.168.0.121`（root）。部署日期：2026-11-03。
本轮未停/未重启任何后端容器（31 个 core/infra 容器保持原状）。

## 1. 当前状态

| 项目 | 值 |
|---|---|
| 前端容器 | `scm-web`（`node:22-alpine`，`Up`） |
| 端口 | `3000`（`--network host`） |
| 部署目录 | `/root/scm-web`（standalone 解压目录） |
| 日志 | `/root/scm-web/scm-web.log`，或 `docker logs -f scm-web` |
| 运行方式 | `node server.js`，`HOSTNAME=0.0.0.0`，`PORT=3000` |
| 内存 | 容器约 124MiB；VM 可用内存约 492MB（后端未受影响） |
| 重启策略 | `no`（重启主机后需人工启动） |

访问地址：`http://192.168.0.121:3000/zh-CN/login`

## 2. 为什么用 standalone + Node 容器

- `next.config.ts` 已经包含 `output: "standalone"`，本轮沿用，无需改配置。
- `scm-web/.env.local` 的 `API_*_TARGET` 已经全部指向 `192.168.0.121:<实际端口>`，
  没有 `192.168.0.132`；`NEXT_PUBLIC_API_BASE_URL` 为空，前端走同源 rewrite。
- VM 上 **没有 node/npm**，按要求不安装系统级 Node/依赖；使用 Docker 镜像
  `node:22-alpine` 运行 standalone 产物，VM 不执行 `npm install`。
- standalone 自带最小 `node_modules`，无需在 VM 重新装依赖。

## 3. 本机构建与同步

本机目录：`D:\ProgramProject\scm-platform\scm-web`（Node v24.22.0，node_modules 已存在）。

```powershell
npm run typecheck   # 退出码 0，耗时 1.3 秒
npm run build       # 退出码 0，耗时 9.8 秒
```

产物：

- `.next/standalone`：31.9MB
- 叠加 `.next/static` + `public` 后的部署目录：37.7MB
- 打包 `scm-web-deploy.tar.gz`：8.1MB（MD5 `a82fc31c1f42fdf9739e41c1722fd829`）

同步方式：本机临时 HTTP 服务（端口 8099）提供 tar 包，VM 侧 `curl` 下载到
`/root/scm-web-deploy.tar.gz`，再 `tar -xzf ... -C /root/scm-web`。传输完成后临时 HTTP 服务已停止。
tar 解压时提示 Windows 与 VM 存在约 76 分钟时间差（未来时间戳），不影响解压与运行。

目录结构：

```
/root/scm-web/
  server.js
  .next/           # 含 static
  node_modules/
  public/
  package.json
  scm-web.log
```

## 4. 启动命令

```bash
docker rm -f scm-web 2>/dev/null || true
docker run -d --name scm-web --network host --restart no --user 0:0 \
  -e HOSTNAME=0.0.0.0 -e PORT=3000 -e NODE_ENV=production \
  -e NODE_OPTIONS=--max-old-space-size=192 \
  -v /root/scm-web:/app -w /app node:22-alpine \
  sh -c 'node server.js 2>&1 | tee -a /app/scm-web.log'
```

说明：

- `--network host` 让前端直接访问 `192.168.0.121:8106/8201/8302...`，rewrite 目标已烘焙进构建产物。
- `--restart no`：不参与开机自启；主机重启后需 `docker start scm-web` 或重新执行上面的启动命令。
- `scm-web` 是独立 Docker 容器，**不属于** `scm-platform` compose 项目；不要用
  `docker compose down` 去操作它。

## 5. 验收结果

### 页面可达

| 检查 | VM 侧 | 本机 Windows 侧 |
|---|---|---|
| `HEAD /zh-CN/login` | **200** | **200** |
| `HEAD /zh-CN/dashboard`（无 cookie） | **307** → `/zh-CN/login` | **307** → `/zh-CN/login` |
| 登录页 CSS 静态资源 | **200** | — |
| 登录页 `/_next/image`（`/auth/cover.jpg`） | **200 image/jpeg 149KB** | — |

### 登录链路（curl 方式）

```text
POST http://127.0.0.1:3000/auth/api/auth/login  -> 200，返回 access token（长度不打印）
GET  /zh-CN/dashboard + Cookie: access_token=... -> 200
```

说明：浏览器未做自动化驱动；以上 curl 流程验证了前端代理登录、token 获取、cookie 鉴权与 dashboard 渲染。
登录页本身 200、CSS/图片均 200。

### 前端代理（`/scm-product`、`/scm-member`）

| 请求头组合 | `prod-brand` | 说明 |
|---|---|---|
| 无 token、无租户头 | **500** | 缺 `X-Tenant-Id`，TenantFilter 在过滤器链抛错，未进全局 advice |
| 仅租户头（无 token） | **401** | 未认证 |
| 仅 token（无租户头） | **500** | 缺 `X-Tenant-Id` |
| token + 租户头 | **200** | 返回 6 条品牌数据 |
| member：token + 租户头 | **200** | 空分页（`total=0`） |

结论：rewrite 目标正确；前端实际请求必须同时带 Bearer token 与
`X-Tenant-Id` / `X-Tenant-Code`，浏览器端由前端代码从 env 注入。无 token 的 500 不是代理地址错误。

### 后端未被影响

| 项目 | 前端启动前 | 前端启动后 |
|---|---|---|
| scm-platform 容器数 | 31 | **31（未变）** |
| 8761/8081/8106/8203/8201 health | 200 | **200** |
| VM 可用内存 | 约 493MB | 约 492MB |
| scm-web 容器内存 | — | 124MiB |

## 6. 停止 / 重启 / 回滚

```bash
# 停止前端（不影响后端）
docker stop scm-web

# 重新启动已有容器
docker start scm-web

# 查看日志
docker logs -f scm-web
tail -f /root/scm-web/scm-web.log

# 回滚：移除前端容器与部署目录（后端完全不受影响）
docker rm -f scm-web
rm -rf /root/scm-web
```

重新部署：

1. 本机修改前端代码后 `npm run typecheck && npm run build`；
2. 将 `.next/standalone`、`.next/static`、`public` 重新打包（static 需放到 standalone 的 `.next/static`）；
3. 传输并解压到 `/root/scm-web`（建议先备份旧目录）；
4. `docker restart scm-web`。

## 7. 注意事项

- 前端容器 `restart=no`；主机重启后需人工启动。
- 构建产物里的 rewrite 目标是构建时值；修改 `API_*_TARGET` 后必须重新 `npm run build`，仅重启容器不生效。
- 本机为 Windows，standalone 内含平台相关包；本次 Linux 上页面、CSS、`/_next/image` 均验证正常。
- 临时 HTTP 传输服务已停止；不要将 `.env`、数据库密码、JWT 密钥写入仓库或本文档。
- 后端 `scm-auth` 的 `X-Device-ID` 绑定问题与本前端部署无关；前端浏览器请求保持同一 UA/IP 时会正常通过。
