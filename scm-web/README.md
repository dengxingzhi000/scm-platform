# @scmcloud/scm-web

Next.js 16 App Router frontend for the SCM platform. The project directory is
`scm-web`; the npm package name is `@scmcloud/scm-web`. It is **not** part of
the Maven reactor.

## Stack contract

| Layer | Choice | Key constraint |
|-------|--------|----------------|
| Framework | Next.js App Router + React 19 + TypeScript 7 | Interactive modules are `'use client'`; first paint uses a Server Component with `prefetchQuery` |
| Styling | Tailwind CSS v4 | Do not introduce a second CSS-in-JS solution |
| UI | shadcn/ui | Component source lives in `src/components/ui/`; edit it directly |
| Server state | TanStack Query v5 | One `QueryClient` reused from the client `QueryProvider`; never put query data inside `'use cache'` |
| Client state | Zustand | UI state only (sidebar, theme, global filter context); server data never goes here |
| Forms | React Hook Form + Zod | Purchase orders, supplier onboarding, stock adjustments and other long forms use `FormField` + `zodResolver` |
| Tables | TanStack Table (default); AG Grid Enterprise on demand | AG Grid is `dynamic(() => import(...), { ssr: false })`; its CSS is imported statically in `src/app/layout.tsx` |
| Charts | Recharts (KPI cards) / ECharts (complex charts) / G6 (topology) | All dynamic-loaded; ECharts registers modules from `echarts/core`, never the full bundle |
| Tests | Vitest + Playwright | Unit tests cover data transforms; E2E covers core ledger paths |

## Commands

```bash
npm install
npm run dev          # http://localhost:3000
npm run lint         # eslint . (flat config)
npm run typecheck    # TypeScript 7 (tsc --noEmit)
npm test             # Vitest unit tests
npm run test:coverage
npm run test:e2e     # Playwright (starts the dev server)
```

## TypeScript 7

TypeScript runs side-by-side, as recommended by the TypeScript team:

- `@typescript/native` aliases `typescript@7` and is the `tsc` used by
  `npm run typecheck`.
- `typescript` aliases `@typescript/typescript6`. `typescript-eslint` bundles
  with `eslint-config-next@16` and still requires the 6.x API
  (`>=4.8.4 <6.1.0`), so ESLint and Next's build-time type check use TS 6.
- Always type-check through `npm run typecheck`. Running `npx tsc` directly can
  hit the TS 6 shim pulled in by `@typescript/typescript6`, because npm links a
  single `.bin/tsc`.
- `overrides.effect` only exists to satisfy npm's optional-peer resolution for
  `@hookform/resolvers@6`; the app does not use effect.

## API layer

The browser talks to the gateway (`NEXT_PUBLIC_API_BASE_URL`, default
`http://localhost:8761`). Gateway routes strip their first path segment, so the
public paths are:

| Backend service | Gateway path | Downstream path |
|-----------------|--------------|-----------------|
| auth | `/auth/api/auth/**` | `/api/auth/**` |
| auth (OAuth2) | `/auth/oauth2/**` | `/oauth2/**` |
| system | `/system/api/system/**` | `/api/system/**` |

- Typed request/response contracts live in `src/lib/api/contracts/`.
- Gateway-aware service functions live in `src/lib/api/services/`.
- `src/lib/api/envelope.ts` unwraps `ApiResponse<T>` (`code`/`message`/`data`).
- Feature mappers (for example `src/features/system/lib/mappers.ts`) convert
  backend DTOs (`realName`, numeric `status`, `pageNum`) into UI models
  (`displayName`, `active/disabled/locked`, `current`).
- `openapitools.json` still points at the direct auth/system Swagger endpoints.
  When those services are running, `npm run generate:api` can refresh generated
  clients; the checked-in contracts are the source of truth while Swagger is
  offline.

## Project layout

```
src/app/layout.tsx                 # owns <html>/<body>, Tailwind + AG Grid CSS
src/app/[locale]/layout.tsx        # next-intl + AntdRegistry + Theme + Query providers
src/app/[locale]/(app)/<domain>/   # locale-prefixed pages
src/app/globals.css                # Tailwind v4 + shadcn variables -> design tokens
src/components/ui/                 # vendored shadcn/ui components
src/components/charts/             # lazy ECharts / Recharts / G6 wrappers
src/components/data-grid/          # lazy AG Grid wrapper
src/features/<domain>/             # components | hooks | services | types | lib
src/lib/api/contracts/             # backend DTOs for the gateway-facing API
src/lib/api/services/              # typed auth/system clients using the gateway
src/lib/api/envelope.ts            # ApiResponse<T> unwrap + ApiError
src/lib/charts/echarts-core.ts     # the only place that registers ECharts modules
src/lib/design-tokens.css          # single palette source (AntD + Tailwind + canvas)
src/lib/design-tokens.ts           # numeric tokens for the AntD theme
src/providers/                     # theme + auth providers
src/stores/                        # Zustand (UI state only)
src/test/setup.ts                  # Vitest setup
```

## Rules encoded in tooling

- `eslint.config.mjs` blocks bare imports of `echarts`, `echarts-for-react`,
  `@antv/g6`, `ag-grid-community` and `ag-grid-react` outside their wrappers.
  Add new chart types by registering them in `src/lib/charts/echarts-core.ts`.
- AG Grid and every chart are loaded with `next/dynamic` + `ssr: false`; they
  must only be imported from client components.
- Dark mode is the `data-theme="dark"` attribute on `<html>` (set by
  `ThemeProvider`), not a `.dark` class. Tailwind's `dark:` variant is mapped to
  it in `globals.css`.
- Canvas chart colours come from `useThemeTokens()` so ECharts/G6 follow the
  same palette as Tailwind and Ant Design.
- The dashboard is a Server Component prefetching `dashboardService.getStats()`
  and hydrating React Query on the client. When that service moves to the
  gateway, forward the auth/tenant cookies from the server fetch.
- Zustand is for UI state only. `useAuthStore` is the documented exception: it
  persists the session token and current user for the Axios interceptor. Do not
  add more server data to any Zustand store.
- AG Grid Community is installed today. Add `ag-grid-enterprise` (and register
  `AllEnterpriseModule` inside `src/components/data-grid/ag-grid-client.tsx`)
  only when the first enterprise feature is needed — never at the root layout.
- E2E: `e2e/product-ledger.spec.ts` covers the product ledger with the mock
  service and an injected `access_token` cookie. Install browsers once with
  `npx playwright install` before running `npm run test:e2e`.

## Migration status

The shared infrastructure is in place and the **dashboard** and **product**
screens are ported to the new stack (shadcn/ui, TanStack Table, RHF + Zod,
Recharts/ECharts). The **auth** flow and **system management** screens
(user/role/dept/dict/permission) now call the real auth/system service through
the gateway; the rest of the domains still render Ant Design 6 components and
use local mocks.

When porting a domain:

1. Replace Ant Design primitives with the vendored shadcn/ui components.
2. Move page state to `useQuery`/`useMutation`; keep only UI state in Zustand.
3. Convert long forms to `useForm` + `zodResolver` and put the numeric/date
   conversion in a pure `lib/*-schema.ts` helper with a Vitest test.
4. Use TanStack Table for lists; only reach for the lazy `DataGrid` when AG Grid
   features (pivoting, grouping, huge datasets) are required.
5. Add ECharts modules to `echarts-core.ts`; never import `echarts` directly.
6. Delete the Ant Design import; do not leave both implementations behind.

## Internationalisation

Routes are locale-prefixed (`/zh-CN/...`, `/en-US/...`). `src/proxy.ts` handles
locale detection and redirects unauthenticated requests to `/login` unless an
`access_token` cookie is present. `LocaleAttributes` applies `lang` to the
document because the root layout owns `<html>`.
