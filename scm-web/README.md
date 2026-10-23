# scm-web

Next.js 16 App Router frontend for the SCM platform. Separate npm project —
it is **not** part of the Maven reactor.

## Stack contract

| Layer | Choice | Key constraint |
|-------|--------|----------------|
| Framework | Next.js App Router + React 19 + TypeScript | Interactive modules are `'use client'`; first paint uses a Server Component with `prefetchQuery` |
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
npm run typecheck    # tsc --noEmit
npm test             # Vitest unit tests
npm run test:coverage
npm run test:e2e     # Playwright (starts the dev server)
```

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

## Migration status

The shared infrastructure is in place and the **dashboard** and **product**
screens are ported to the new stack (shadcn/ui, TanStack Table, RHF + Zod,
Recharts/ECharts). Other domains still render Ant Design 6 components.

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
