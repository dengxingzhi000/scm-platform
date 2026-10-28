# 登录页视觉重构(Claude 版式) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `/{locale}/login` 与 `/{locale}/forgot-password` 重做成参考图的双栏版式(暖象牙底 + 衬线大标题 + 白卡片 + 近黑主按钮 + 右侧满高圆角图),并从 Ant Design 迁移到 shadcn/ui + Tailwind v4 + React Hook Form + Zod,文案接入 next-intl 双语。

**Architecture:** 认证外壳(`AuthShell`)负责双栏构图与品牌标记,登录卡片(`LoginCard`)负责表单;表单校验用 Zod schema 放在独立模块,便于单测。配色通过 `design-tokens.css` 新增的 auth 域令牌表达,登录外壳根元素上覆写 shadcn 变量(`--primary`/`--ring`/`--accent`)把橙色限制在登录体验内,全局靛蓝不变。会话建立逻辑从 `login/page.tsx` 抽成共享 hook,供登录页与(后续计划的)社交回调页共用。

**Tech Stack:** Next.js 16.4 App Router / React 19 / TypeScript 7 / Tailwind CSS v4 / shadcn/ui(`src/components/ui`)/ React Hook Form + Zod / next-intl / Vitest + @testing-library/react

**Spec:** `docs/superpowers/specs/2026-10-27-login-page-claude-redesign-social-auth-design.md`(A 部分:A1–A8)

**配套计划:** `docs/superpowers/plans/2026-10-27-social-auth-google-apple.md`(社交登录,依赖本计划产出的登录卡片结构)

## Global Constraints

- 设计令牌是唯一色源:新增的 auth 域令牌一律加到 `src/lib/design-tokens.css`,**不得**在组件里写死 hex 值。
- 强调色令牌名带 `brand-` 前缀(`--color-brand-accent*`)。**不得**使用 `--color-accent` —— `src/app/globals.css` 的 `@theme inline` 块里已有 `--color-accent: var(--accent)`(shadcn muted-accent,驱动 Tailwind 的 `bg-accent` 工具类),同名会覆盖它。
- 登录页只在 `AuthShell` 根元素上覆写 `--primary` / `--ring` / `--accent` 三个 shadcn 变量;**不得**改 `src/lib/design-tokens.css` 里的 `--color-primary`(靛蓝 `#4f46e5`)—— 其它页面保持靛蓝。
- 不新增任何 npm 依赖。shadcn 组件已全部 vendor 在 `src/components/ui`(button / input / card / checkbox / label / separator / form 都在),需要新组件时用 `npx shadcn@latest add <component>` 写进 `src/components/ui/`。
- antd 只允许在 `ThemeProvider` 里保留 `App` 外壳(用于 `message` 弹提示);**登录/找回密码页面内不得再出现任何 antd 组件**。
- ESLint 禁止裸 import:`echarts` / `echarts-for-react` / `ag-grid-community` / `ag-grid-react` / `@antv/g6` 只能在 `src/components/charts`、`src/components/data-grid`、`src/lib/charts` 里 import。
- 所有面向用户的文案必须走 next-intl,**不得**硬编码中文或英文。
- TypeScript 7 与 TypeScript 6 并存:`npm run typecheck` 是 TS7 闸门;**不要**直接跑 `npx tsc`。
- 提交粒度 = 一个 Task 一次提交,提交信息用中文 + 约定式前缀(`feat:` / `refactor:` / `test:` / `docs:` / `chore:`)。

## Review Focus

以下 5 类失败模式在规格里被提及,但没有哪个 Task 的测试真正覆盖。实现时,每一条都要在**拥有该代码的 Task** 里补上对应测试。

1. **未登录时直接访问受保护页面** —— `src/proxy.ts` 靠 `access_token` cookie 判断登录态;重构若动了 cookie 的写入方式(例如改成只写内存不写 cookie),未登录用户会绕过前端守卫。期望:登录后 cookie 存在,刷新页面仍保持登录态。
2. **「记住我」被取消勾选** —— 当前实现无论勾选与否都写 7 天 cookie。期望:取消勾选时**不得**写 `access_token` cookie(或不写长效 cookie),避免用户以为"不记住"结果 token 仍在。
3. **表单校验失败时不应发起登录请求** —— 空用户名/空密码提交,期望:`authService.login` **不被调用**,且错误提示出现在对应字段下方。
4. **TOTP 分支判定错误** —— 服务端返回 `40104` 或消息含 `two-factor|verification code|MFA` 时才切验证码页。期望:其它 401(密码错误)必须停在登录页并提示,不得误切到验证码页。
5. **找回密码页的三步流程** —— 步骤之间必须能前进/后退。期望:发送验证码后进入第 2 步,验证后进入第 3 步,第 3 步提交成功后跳回 `/{locale}/login`。

---

### Task 1: 设计令牌与衬线字体栈

**Files:**
- Modify: `scm-web/src/lib/design-tokens.css`(在 `--auth-*` 现有段落追加)
- Modify: `scm-web/src/app/globals.css`(在 `@theme inline` 块追加 `--font-serif` 别名)
- Test: `scm-web/src/lib/design-tokens.test.ts`

**Interfaces:**
- Consumes: 无
- Produces: CSS 自定义属性 `--color-brand-accent`、`--color-brand-accent-hover`、`--color-brand-accent-active`、`--color-brand-accent-bg`、`--auth-bg`、`--auth-surface`、`--auth-ink`、`--auth-ink-soft`、`--auth-line`、`--auth-line-strong`、`--auth-btn-soft-bg`、`--auth-btn-soft-hover`、`--auth-btn-fg`,以及 Tailwind 工具类 `font-serif`。

- [ ] **Step 1: 写失败的测试**

创建 `scm-web/src/lib/design-tokens.test.ts`:

```ts
import { readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const TOKENS = readFileSync(join(process.cwd(), "src/lib/design-tokens.css"), "utf8");
const GLOBALS = readFileSync(join(process.cwd(), "src/app/globals.css"), "utf8");

/**
 * 去掉 CSS 注释再匹配:注释里提到 `--color-accent: var(--accent)` 只是说明文字,
 * 不是令牌声明 —— 断言要盯的是「有没有声明」,不是「有没有提到」。
 */
const DECLARATIONS = TOKENS.replace(/\/\*[\s\S]*?\*\//g, "");

/**
 * Tailwind v4 只从 `@theme` 块里读变量:别名写在普通 `:root` 里不会生成
 * `font-serif` 工具类(而且不会报错,只是样式静默失效)。所以断言要落在块内。
 */
const THEME_BLOCK = (() => {
  const match = GLOBALS.match(/@theme\s+inline\s*\{([\s\S]*?)\n\}/);
  if (!match) throw new Error("@theme inline block not found in globals.css");
  return match[1];
})();

/**
 * 深色模式块。登录页配色是固定浅色(规格 A1 没给深色变体),
 * 这里不允许再出现 `--auth-bg` / `--auth-ink` 覆写:近黑文字落在深色底上不可读。
 */
const DARK_BLOCK = (() => {
  const match = TOKENS.match(/\[data-theme=['"]dark['"]\]\s*\{([\s\S]*?)\n\}/);
  if (!match) throw new Error("[data-theme='dark'] block not found in design-tokens.css");
  return match[1].replace(/\/\*[\s\S]*?\*\//g, "");
})();

function token(name: string): string {
  const match = DECLARATIONS.match(new RegExp(`--${name}:\\s*([^;]+);`));
  if (!match) throw new Error(`token --${name} not found in design-tokens.css`);
  return match[1].trim();
}

/** 源码文件(排除测试文件),用来核对「被引用的令牌都有定义」。 */
function sourceFiles(dir = join(process.cwd(), "src")): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return sourceFiles(path);
    if (!/\.tsx?$/.test(entry.name)) return [];
    return /\.test\.tsx?$/.test(entry.name) ? [] : [path];
  });
}

describe("auth design tokens", () => {
  it("pins the Claude accent ramp", () => {
    expect(token("color-brand-accent")).toBe("#D97757");
    expect(token("color-brand-accent-hover")).toBe("#C2603F");
    expect(token("color-brand-accent-active")).toBe("#A95134");
    expect(token("color-brand-accent-bg")).toBe("#F5E7E0");
  });

  it("pins the warm neutral ramp", () => {
    expect(token("auth-bg")).toBe("#F0EEE6");
    expect(token("auth-surface")).toBe("#FFFFFF");
    expect(token("auth-ink")).toBe("#262624");
    expect(token("auth-ink-soft")).toBe("#6B6A63");
    expect(token("auth-line")).toBe("#E5E3DA");
    expect(token("auth-line-strong")).toBe("#D4D1C5");
    expect(token("auth-btn")).toBe("#262624");
    expect(token("auth-btn-soft-bg")).toBe("#E9E6DC");
    expect(token("auth-btn-soft-hover")).toBe("#DFDBCE");
    expect(token("auth-btn-fg")).toBe("#FFFFFF");
  });

  it("does not shadow the shadcn --color-accent alias", () => {
    expect(DECLARATIONS).not.toMatch(/--color-accent\s*:/);
  });

  it("keeps the global indigo primary for every non-auth page", () => {
    expect(token("color-primary")).toBe("#4f46e5");
  });

  it("does not override the fixed-light auth palette in dark mode", () => {
    expect(DARK_BLOCK).not.toMatch(/--auth-bg\s*:/);
    expect(DARK_BLOCK).not.toMatch(/--auth-ink\s*:/);
  });

  it("exposes a serif stack that covers Latin and CJK", () => {
    // 令牌在 design-tokens.css 里叫 --scm-font-serif:globals.css 的 @theme inline
    // 要写 --font-serif: var(--scm-font-serif),同名会变成自引用。
    const serif = token("scm-font-serif");
    expect(serif).toContain("Georgia");
    expect(serif).toContain("Songti SC");
    expect(serif).toContain("Noto Serif CJK SC");

    // 别名必须落在 @theme inline 块内(普通 :root 不生成工具类),且不能自引用。
    expect(THEME_BLOCK).toMatch(/--font-serif:\s*var\(--scm-font-serif\);/);
    expect(THEME_BLOCK).not.toMatch(/--font-serif:\s*var\(--font-serif\)/);
  });

  it("defines every auth token that the source still references", () => {
    const referenced = new Set<string>();
    for (const file of sourceFiles()) {
      const content = readFileSync(file, "utf8");
      for (const match of content.matchAll(/var\(--((?:auth|color-brand)[a-z0-9-]*)\)/g)) {
        referenced.add(match[1]);
      }
    }

    // 扫描本身要有效:空集合说明遍历挂了,断言会变成空转。
    expect(referenced.size).toBeGreaterThan(0);
    for (const name of referenced) {
      expect(DECLARATIONS, `--${name} 被引用但未定义`).toMatch(new RegExp(`--${name}\\s*:`));
    }
  });
});

```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd scm-web && npm test -- src/lib/design-tokens.test.ts`
Expected: FAIL —— `--color-brand-accent not found in design-tokens.css`

- [ ] **Step 3: 在 design-tokens.css 追加 auth 域令牌**

在 `src/lib/design-tokens.css` 里,`/* Auth page background */` 段落之前插入:

```css
  /* Claude 强调色(仅登录体验)
     命名带 brand- 前缀是刻意的:globals.css 的 @theme inline 里已有
     --color-accent: var(--accent)(shadcn muted-accent,驱动 bg-accent 工具类) */
  --color-brand-accent: #D97757;
  --color-brand-accent-hover: #C2603F;
  --color-brand-accent-active: #A95134;
  --color-brand-accent-bg: #F5E7E0;
```

并把现有的 `--auth-bg` / `--auth-card-shadow` / `--auth-brand-bg` 三行**替换**为:

```css
  /* Auth 页面暖中性色 */
  --auth-bg: #F0EEE6;
  --auth-surface: #FFFFFF;
  --auth-ink: #262624;
  --auth-ink-soft: #6B6A63;
  --auth-line: #E5E3DA;
  --auth-line-strong: #D4D1C5;
  --auth-btn: #262624;
  --auth-btn-soft-bg: #E9E6DC;
  --auth-btn-soft-hover: #DFDBCE;
  --auth-btn-fg: #FFFFFF;
```

> `--auth-btn` 是主按钮的底色(近黑),Task 2/3/5 的主按钮都用 `bg-[var(--auth-btn)]` ——
> 少这一条,主按钮会静默丢掉背景色。
>
> **`--auth-card-shadow` 不要删** —— 它被 antd 的 `auth-brand-card.tsx` 用着(那个组件 Task 5 才删)。
> 现在删掉会让登录/找回密码卡片在 Task 3/5 之前渲染成无阴影(`box-shadow: var(--undefined)` 计算值非法)。
> Task 5 删组件时连同这个令牌一起删。
> **但不要删 `--auth-brand-bg`** —— 它还有第二个消费者:应用侧边栏的 logo 徽标
> (`src/components/Layout/sidebar.tsx`)。保留它,并加一行注释说明它已不再用于登录页。
>
> 登录页配色是**固定的浅色调**:`[data-theme="dark"]` 块里**不要**再覆写 `--auth-bg` /
> `--auth-ink` 等 —— 深色底上放近黑的标题会读不清。`--auth-brand-bg` 的深色变体保留。

并在文件末尾追加:

```css
  /* 衬线字体栈:英文落到 Georgia,中文落到宋体/思源宋体
     名字带 scm- 前缀是刻意的:globals.css 的 @theme inline 要写
     --font-serif: var(--scm-font-serif)。若两边都叫 --font-serif,
     @theme 生成的那条就是自引用,--font-serif 会解析成 invalid。 */
  --scm-font-serif: Georgia, 'Times New Roman', 'Songti SC', 'Noto Serif CJK SC',
    'Source Han Serif SC', serif;
```

- [ ] **Step 4: 在 globals.css 暴露 font-serif 工具类**

在 `src/app/globals.css` 的 `@theme inline { ... }` 块内追加一行:

```css
  --font-serif: var(--scm-font-serif);
```

> 该块里的 `--color-primary: var(--primary)` 等是 shadcn 变量 → Tailwind 颜色工具类的别名。
> 这里同理,把 `design-tokens.css` 的 `--scm-font-serif` 暴露成 Tailwind 的 `font-serif` 工具类。
> **注意**:这里的 `--font-serif` 必须指向 `--scm-font-serif`,**不能**写成
> `--font-serif: var(--font-serif)` —— 那是一条自引用,变量会解析失败,`font-serif` 工具类
> 随之失效。
> **不要**往这个块里加 `--color-brand-accent-*`。Tailwind v4 只会从 `@theme` 块里生成
> `--color-*` 工具类;`design-tokens.css` 里的 `--color-brand-accent` 定义在普通 `:root`
> 块中,**不会**生成 `bg-brand-accent` 这样的工具类。所以组件里一律用
> `bg-[var(--color-brand-accent)]` / `text-[var(--color-brand-accent)]` 这种任意值写法。
> 也不要把 `--color-brand-accent` 写进 `@theme inline` —— 那会和 `:root` 里的同名声明
> 形成自引用。


- [ ] **Step 5: 运行测试确认通过**

Run: `cd scm-web && npm test -- src/lib/design-tokens.test.ts`
Expected: PASS(7 个用例)

- [ ] **Step 6: 提交**

```bash
cd scm-web
git add src/lib/design-tokens.css src/app/globals.css src/lib/design-tokens.test.ts
git commit -m "feat(web): 新增登录页 Claude 强调色与暖中性色设计令牌"
```

---

### Task 2: 认证外壳、品牌标记与占位图

**Files:**
- Create: `scm-web/src/components/auth/auth-shell.tsx`
- Create: `scm-web/src/components/auth/brand-mark.tsx`
- Create: `scm-web/public/auth-hero.svg`
- Create: `scm-web/src/components/auth/auth-shell.test.tsx`
- Modify: `scm-web/src/app/[locale]/(auth)/layout.tsx`

**Interfaces:**
- Consumes: Task 1 的 `--auth-bg`、`--auth-surface`、`--auth-ink`、`--color-brand-accent`、`font-serif` 工具类
- Produces:
  - `BrandMark(props: { className?: string })` —— 渲染橙色标记 + 「SCM 供应链管理平台」字标
  - `AuthShell(props: { children: React.ReactNode })` —— 双栏外壳;根元素上覆写 `--primary`/`--ring`/`--accent` 为橙色

- [ ] **Step 1: 写失败的测试**

创建 `scm-web/src/components/auth/auth-shell.test.tsx`:

```tsx
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { AuthShell } from "./auth-shell";

describe("AuthShell", () => {
  it("renders the brand mark and both header actions", () => {
    render(
      <AuthShell>
        <p>content</p>
      </AuthShell>
    );

    expect(screen.getByText("SCM 供应链管理平台")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "联系销售" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "试用平台" })).toBeInTheDocument();
  });

  it("scopes the Claude accent to the login experience only", () => {
    const { container } = render(<AuthShell>content</AuthShell>);
    const root = container.firstElementChild as HTMLElement;
    expect(root.style.getPropertyValue("--primary")).toBe("var(--color-brand-accent)");
    expect(root.style.getPropertyValue("--ring")).toBe("var(--color-brand-accent)");
  });

  it("renders the children in the left column and the hero image in the right column", () => {
    render(
      <AuthShell>
        <p>content</p>
      </AuthShell>
    );

    expect(screen.getByText("content")).toBeInTheDocument();
    const hero = screen.getByRole("img", { name: "供应链协同" });
    expect(hero).toHaveAttribute("src", "/auth-hero.svg");
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd scm-web && npm test -- src/components/auth/auth-shell.test.tsx`
Expected: FAIL —— `Cannot find module './auth-shell'`

- [ ] **Step 3: 创建 `brand-mark.tsx`**

`scm-web/src/components/auth/brand-mark.tsx`:

```tsx
import { cn } from "@/lib/utils";

interface BrandMarkProps {
  className?: string;
}

/** 登录页左上角的品牌标记:橙色放射状标记 + 平台字标。 */
export function BrandMark({ className }: BrandMarkProps) {
  return (
    <div className={cn("flex items-center gap-2.5", className)}>
      <svg
        viewBox="0 0 24 24"
        aria-hidden="true"
        className="size-6 text-[var(--color-brand-accent)]"
      >
        <g stroke="currentColor" strokeWidth="1.6" strokeLinecap="round">
          <path d="M12 12V3" />
          <path d="M12 12 4.2 6.6" />
          <path d="M12 12l7.8-5.4" />
          <path d="M12 12v9" />
          <path d="M12 12 5.4 17.4" />
          <path d="M12 12l6.6 5.4" />
        </g>
      </svg>
      <span className="font-serif text-lg tracking-tight text-[var(--auth-ink)]">
        SCM 供应链管理平台
      </span>
    </div>
  );
}
```

- [ ] **Step 4: 创建 `auth-shell.tsx`**

`scm-web/src/components/auth/auth-shell.tsx`:

```tsx
import type { ReactNode } from "react";

import { BrandMark } from "./brand-mark";

interface AuthShellProps {
  children: ReactNode;
}

/**
 * 认证页双栏外壳。
 * 左栏是暖象牙底的表单区,右栏是满高圆角占位图。
 * 根元素上覆写 shadcn 的 --primary/--ring/--accent,把 Claude 橙限制在登录体验内,
 * 全局 --color-primary(靛蓝)不变。
 */
export function AuthShell({ children }: AuthShellProps) {
  return (
    <main
      className="min-h-dvh bg-[var(--auth-bg)] lg:grid lg:grid-cols-[1fr_minmax(0,42%)]"
      style={
        {
          "--primary": "var(--color-brand-accent)",
          "--ring": "var(--color-brand-accent)",
          "--accent": "var(--color-brand-accent-bg)",
        } as React.CSSProperties
      }
    >
      <section className="flex flex-col px-6 py-6 lg:px-12">
        <header className="flex items-center justify-between gap-4">
          <BrandMark />
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="rounded-lg border border-[var(--auth-line-strong)] bg-[var(--auth-btn-soft-bg)] px-4 py-2 text-sm font-medium text-[var(--auth-ink)] transition-colors hover:bg-[var(--auth-btn-soft-hover)]"
            >
              联系销售
            </button>
            <button
              type="button"
              className="rounded-lg bg-[var(--auth-btn)] px-4 py-2 text-sm font-medium text-[var(--auth-btn-fg)] transition-colors hover:bg-[var(--auth-ink-soft)]"
            >
              试用平台
            </button>
          </div>
        </header>

        <div className="flex flex-1 items-center justify-center py-12">
          <div className="w-full max-w-[520px]">{children}</div>
        </div>
      </section>

      <aside className="hidden p-6 lg:block">
        <img
          src="/auth-hero.svg"
          alt="供应链协同"
          className="h-full w-full rounded-2xl object-cover"
        />
      </aside>
    </main>
  );
}
```

> 顶栏两个按钮的文案在这一版先硬编码;Task 6 会把它们换成 next-intl 文案并同步更新本文件的测试断言。
> 「联系销售」「试用平台」在本次范围内**不绑定跳转**(规格未定义目标路由)—— 保持 `<button type="button">`,不写 `Link`。

- [ ] **Step 5: 创建占位图 `public/auth-hero.svg`**

`scm-web/public/auth-hero.svg`:

```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 1200" width="800" height="1200" role="img" aria-label="供应链协同">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#F0EEE6"/>
      <stop offset="55%" stop-color="#E4E0D2"/>
      <stop offset="100%" stop-color="#D8D3C2"/>
    </linearGradient>
    <linearGradient id="accent" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#D97757" stop-opacity="0.85"/>
      <stop offset="100%" stop-color="#A95134" stop-opacity="0.55"/>
    </linearGradient>
  </defs>
  <rect width="800" height="1200" fill="url(#bg)"/>
  <g fill="#FFFFFF" opacity="0.55">
    <rect x="80" y="140" width="240" height="150" rx="12"/>
    <rect x="360" y="180" width="180" height="110" rx="12"/>
    <rect x="120" y="340" width="200" height="120" rx="12"/>
    <rect x="400" y="360" width="300" height="180" rx="12"/>
    <rect x="90" y="620" width="320" height="200" rx="12"/>
    <rect x="470" y="610" width="230" height="140" rx="12"/>
  </g>
  <g stroke="#C9C3B1" stroke-width="2" fill="none">
    <path d="M200 640 L400 720 L585 680"/>
    <path d="M200 500 L400 540 L200 640"/>
    <path d="M400 540 L585 460"/>
  </g>
  <rect x="120" y="900" width="300" height="180" rx="16" fill="url(#accent)"/>
  <rect x="470" y="880" width="230" height="140" rx="16" fill="#FFFFFF" opacity="0.75"/>
</svg>
```

- [ ] **Step 6: 接线 `layout.tsx`**

把 `scm-web/src/app/[locale]/(auth)/layout.tsx` 改成:

```tsx
import { ReactNode } from "react";

import { AuthShell } from "@/components/auth/auth-shell";

export default function AuthRouteLayout({ children }: { children: ReactNode }) {
  return <AuthShell>{children}</AuthShell>;
}
```

- [ ] **Step 7: 运行测试确认通过**

Run: `cd scm-web && npm test -- src/components/auth/auth-shell.test.tsx`
Expected: PASS(3 个用例)

- [ ] **Step 8: 提交**

```bash
cd scm-web
git add src/components/auth/auth-shell.tsx src/components/auth/brand-mark.tsx \
        src/components/auth/auth-shell.test.tsx public/auth-hero.svg \
        "src/app/[locale]/(auth)/layout.tsx"
git commit -m "feat(web): 新增 Claude 版式认证双栏外壳与品牌标记"
```

---

### Task 3: 登录卡片与表单(React Hook Form + Zod)

**Files:**
- Create: `scm-web/src/components/auth/login-schema.ts`
- Create: `scm-web/src/components/auth/login-card.tsx`
- Create: `scm-web/src/components/auth/login-form.tsx`
- Create: `scm-web/src/components/auth/login-form.test.tsx`
- Create: `scm-web/src/components/auth/login-schema.test.ts`
- Modify: `scm-web/src/app/[locale]/(auth)/login/page.tsx`

**Interfaces:**
- Consumes: Task 1 的令牌;Task 2 的 `AuthShell`;现有 `authService.login`(`scm-web/src/lib/api/services/auth.ts`)、`useAuthStore`、`useLocale`
- Produces:
  - `loginSchema` —— `z.object({ username: z.string().min(1, ...), password: z.string().min(1, ...) })`
  - `totpSchema` —— `z.object({ code: z.string().length(6).regex(/^\d+$/, ...) })`
  - `LoginCard(props: { title: string; subtitle: string; children: ReactNode })`
  - `LoginForm(props: { onSuccess: (response: LoginResponse) => void; onMfaRequired: (values: LoginFormValues) => void; onError: (error: ApiError) => void })`

- [ ] **Step 1: 写失败的测试(schema)**

创建 `scm-web/src/components/auth/login-schema.test.ts`:

```ts
import { describe, expect, it } from "vitest";

import { buildLoginSchema, buildTotpSchema } from "./login-schema";

// 翻译函数原样返回 key,这样断言里能直接看到是哪个 key
const t = (key: string) => key;

describe("buildLoginSchema", () => {
  it("rejects an empty username", () => {
    const result = buildLoginSchema(t).safeParse({ username: "", password: "x" });
    expect(result.success).toBe(false);
  });

  it("rejects an empty password", () => {
    const result = buildLoginSchema(t).safeParse({ username: "admin", password: "" });
    expect(result.success).toBe(false);
  });

  it("accepts a filled pair", () => {
    expect(buildLoginSchema(t).safeParse({ username: "admin", password: "admin123" }).success).toBe(true);
  });

  it("puts the translated message into the error", () => {
    const result = buildLoginSchema(t).safeParse({ username: "", password: "" });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe("errors.usernameRequired");
    }
  });
});

describe("buildTotpSchema", () => {
  it("requires exactly six digits", () => {
    expect(buildTotpSchema(t).safeParse({ code: "12345" }).success).toBe(false);
    expect(buildTotpSchema(t).safeParse({ code: "12345a" }).success).toBe(false);
    expect(buildTotpSchema(t).safeParse({ code: "123456" }).success).toBe(true);
  });
});
```

- [ ] **Step 2: 写失败的测试(表单)**

创建 `scm-web/src/components/auth/login-form.test.tsx`:

```tsx
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { LoginForm } from "./login-form";

const login = vi.fn();

vi.mock("@/lib/api/services/auth", () => ({
  authService: { login: (...args: unknown[]) => login(...args) },
}));

// useMutation 需要 QueryClient 上下文;这里直接 mock 掉,让测试只关心表单行为。
// useQuery 也要一并提供 —— Task 6 会把 SocialAuthButtons(useQuery)插进这个表单。
vi.mock("@tanstack/react-query", () => ({
  useMutation: () => ({
    mutateAsync: (...args: unknown[]) => login(...args),
    isPending: false,
  }),
  useQuery: () => ({ data: [] as string[] }),
}));

vi.mock("next-intl", () => ({
  useLocale: () => "zh-CN",
  // 模拟 next-intl 的命名空间行为:useTranslations("login") 返回的 t 会把命名空间拼进 key
  useTranslations: (ns: string) => (key: string) => `${ns}.${key}`,
}));

describe("LoginForm", () => {
  beforeEach(() => {
    login.mockReset();
    login.mockResolvedValue({ accessToken: "t", refreshToken: "r", userId: "u", username: "admin" });
  });

  it("does not call the API when required fields are empty", async () => {
    const onSuccess = vi.fn();
    render(<LoginForm onSuccess={onSuccess} onMfaRequired={vi.fn()} onError={vi.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: "login.submit" }));

    await waitFor(() => {
      expect(screen.getByText("login.errors.usernameRequired")).toBeInTheDocument();
    });
    expect(login).not.toHaveBeenCalled();
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it("submits the credentials and forwards the response", async () => {
    const onSuccess = vi.fn();
    render(<LoginForm onSuccess={onSuccess} onMfaRequired={vi.fn()} onError={vi.fn()} />);

    await userEvent.type(screen.getByLabelText("login.username"), "admin");
    await userEvent.type(screen.getByLabelText("login.password"), "admin123");
    await userEvent.click(screen.getByRole("button", { name: "login.submit" }));

    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
    expect(login).toHaveBeenCalledWith({ username: "admin", password: "admin123" });
  });

  it("reports a non-MFA server failure instead of swallowing it", async () => {
    const onError = vi.fn();
    // 后端拒绝:密码错误。必须走 onError,不能静默 ——
    // 旧页面在这里调 message.error,丢了就是「点登录没反应」。
    login.mockRejectedValue(
      Object.assign(new Error("密码错误"), {
        response: { status: 400, data: { code: 40001, message: "密码错误" } },
      })
    );
    render(<LoginForm onSuccess={vi.fn()} onMfaRequired={vi.fn()} onError={onError} />);

    await userEvent.type(screen.getByLabelText("login.username"), "admin");
    await userEvent.type(screen.getByLabelText("login.password"), "wrong");
    await userEvent.click(screen.getByRole("button", { name: "login.submit" }));

    await waitFor(() => expect(onError).toHaveBeenCalledTimes(1));
    expect(onError.mock.calls[0][0]).toMatchObject({ message: "密码错误" });
  });

  it("exposes name attributes the e2e spec relies on", () => {
    render(<LoginForm onSuccess={vi.fn()} onMfaRequired={vi.fn()} onError={vi.fn()} />);
    expect(screen.getByLabelText("login.username")).toHaveAttribute("name", "username");
    expect(screen.getByLabelText("login.password")).toHaveAttribute("name", "password");
  });
});
```

- [ ] **Step 3: 运行测试确认失败**

Run: `cd scm-web && npm test -- src/components/auth/login-schema.test.ts src/components/auth/login-form.test.tsx`
Expected: FAIL —— `Cannot find module './login-schema'`

- [ ] **Step 4: 创建 `login-schema.ts`**

`scm-web/src/components/auth/login-schema.ts`:

```ts
import { z } from "zod";

/** next-intl 的翻译函数签名(只用到 key,不接参数)。 */
export type Translator = (key: string) => string;

/**
 * 校验 schema 是**工厂**而不是常量:错误文案要在 schema 构建时就翻译好,
 * 这样组件只要渲染 `errors.x?.message` 即可 ——
 * 校验错误是已翻译的文案,服务端错误(如验证码错误)是后端原样返回的文案,
 * 两条路径共用同一处渲染,不需要再做一层 key 映射。
 */
export function buildLoginSchema(t: Translator) {
  return z.object({
    username: z.string().min(1, t("errors.usernameRequired")),
    password: z.string().min(1, t("errors.passwordRequired")),
    // `remember` 只在 `defaultValues` 里出现,不是校验目标 —— 但它必须是 schema 的一部分,
    // 否则 `LoginFormValues` 里没有它,`useForm` 的 defaultValues 与 `FormField name="remember"`
    // 都会报类型错(连带 control/handleSubmit 的泛型一起塌)。
    remember: z.boolean().optional(),
  });
}

export function buildTotpSchema(t: Translator) {
  return z.object({
    code: z
      .string()
      .min(1, t("errors.codeRequired"))
      .length(6, t("errors.codeLength"))
      .regex(/^\d+$/, t("errors.codeDigits")),
  });
}

export type LoginFormValues = z.infer<ReturnType<typeof buildLoginSchema>>;
export type TotpFormValues = z.infer<ReturnType<typeof buildTotpSchema>>;
```

> **不要**把 i18n key 存进 schema 的 message —— 那样组件还得把 key 再翻译一次,
> 而服务端错误(不是 key)就没法走同一条渲染路径。

- [ ] **Step 5: 创建 `login-card.tsx`**

`scm-web/src/components/auth/login-card.tsx`:

```tsx
import type { ReactNode } from "react";

interface LoginCardProps {
  title: string;
  subtitle: string;
  children: ReactNode;
}

/** 登录/找回密码共用的白卡片:细描边 + 大圆角 + 极轻阴影。 */
export function LoginCard({ title, subtitle, children }: LoginCardProps) {
  return (
    <div>
      <h1 className="font-serif text-4xl leading-tight text-[var(--auth-ink)] lg:text-[56px]">
        {title}
      </h1>
      <p className="mt-3 font-serif text-lg text-[var(--auth-ink-soft)]">{subtitle}</p>

      <div className="mt-8 rounded-2xl border border-[var(--auth-line)] bg-[var(--auth-surface)] p-6 shadow-sm lg:p-8">
        {children}
      </div>
    </div>
  );
}
```

- [ ] **Step 6: 创建 `login-form.tsx`**

`scm-web/src/components/auth/login-form.tsx`:

```tsx
"use client";

import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Form, FormField, FormItem } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { authService } from "@/lib/api/services/auth";
import { toApiError } from "@/lib/api/envelope";
import type { LoginResponse } from "@/lib/api/contracts/auth";

import { buildLoginSchema, type LoginFormValues } from "./login-schema";
import type { ApiError } from "@/lib/api/envelope";

/** 服务端要求二次验证。抽成常量函数是为了让旧页面的判定规则原样保留。 */
const isMfaRequired = (message: string, code: number): boolean =>
  code === 40104 || /two-factor|verification code|MFA/i.test(message);

interface LoginFormProps {
  onSuccess: (response: LoginResponse) => void;
  onMfaRequired: (values: LoginFormValues) => void;
  onError: (error: ApiError) => void;
}

export function LoginForm({ onSuccess, onMfaRequired, onError }: LoginFormProps) {
  const t = useTranslations("login");
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(buildLoginSchema(t)),
    defaultValues: { username: "", password: "", remember: true },
  });

  const loginMutation = useMutation({
    mutationFn: (data: { username: string; password: string }) => authService.login(data),
  });

  const onSubmit = async (values: LoginFormValues) => {
    try {
      const response = await loginMutation.mutateAsync({
        username: values.username,
        password: values.password,
      });
      onSuccess(response);
    } catch (error: unknown) {
      const apiError = toApiError(error);
      if (isMfaRequired(apiError.message, apiError.code)) {
        onMfaRequired(values);
        return;
      }
      // 非 MFA 失败(密码错误 / 账号锁定 / 网络错误)必须冒泡给页面去弹提示。
      // 不能 `throw error`:onSubmit 是 handleSubmit 的回调,抛出的 promise 无人 catch,
      // 用户点了登录会**毫无反馈**(旧页面在这里调 message.error)。
      onError(apiError);
    }
  };

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(onSubmit)}
        className="flex flex-col gap-4"
        noValidate
      >
        <FormField
          control={form.control}
          name="username"
          render={({ field }) => (
            <FormItem>
              <Label htmlFor="username">{t("username")}</Label>
              <Input
                id="username"
                name="username"
                autoComplete="username"
                placeholder={t("usernamePlaceholder")}
                className="h-12 rounded-lg bg-transparent"
                {...field}
              />
              {form.formState.errors.username && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.username.message}
                </p>
              )}
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <Label htmlFor="password">{t("password")}</Label>
              <Input
                id="password"
                name="password"
                type="password"
                autoComplete="current-password"
                placeholder={t("passwordPlaceholder")}
                className="h-12 rounded-lg bg-transparent"
                {...field}
              />
              {form.formState.errors.password && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.password.message}
                </p>
              )}
            </FormItem>
          )}
        />

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <FormField
              control={form.control}
              name="remember"
              render={({ field }) => (
                <Checkbox
                  id="remember"
                  checked={field.value}
                  onCheckedChange={field.onChange}
                  onBlur={field.onBlur}
                />
              )}
            />
            <Label htmlFor="remember" className="text-sm font-normal text-[var(--auth-ink-soft)]">
              {t("remember")}
            </Label>
          </div>
          <a href="/zh-CN/forgot-password" className="text-sm text-[var(--color-brand-accent)] underline-offset-4 hover:underline">
            {t("forgotPassword")}
          </a>
        </div>

        <Button
          type="submit"
          disabled={loginMutation.isPending}
          className="h-12 w-full rounded-lg bg-[var(--auth-btn)] text-[var(--auth-btn-fg)] hover:bg-[var(--auth-ink-soft)]"
        >
          {t("submit")}
        </Button>

        <p className="text-center text-[11px] leading-relaxed text-[var(--auth-ink-soft)]">
          {t("legalPrefix")}{" "}
          <a href="/terms" className="underline underline-offset-4">
            {t("terms")}
          </a>{" "}
          {t("legalMiddle")}{" "}
          <a href="/privacy" className="underline underline-offset-4">
            {t("privacy")}
          </a>
          {t("legalSuffix")}
        </p>
      </form>
    </Form>
  );
}
```

> **不要用 shadcn 的 `FormMessage` 承载错误文案**。它的实现是
> `error ? String(error.message) : props.children` —— 没有错误时会把 children 直接渲染出来
> (错误提示一进页面就显示),有错误时又优先显示 `error.message`。
> 所以错误文案自己用条件段落渲染,`FormItem` 只用来做间距。
> 段落里直接渲染 `errors.x?.message`:校验错误在 schema 构建时已翻译,
> 服务端错误(如验证码错误)是后端原样返回的文案 —— 两条路径共用这一处渲染。

> 「忘记密码」链接这一版硬编码 `/zh-CN/forgot-password`;Task 6 换成 `useLocale()` 拼出来的路径。

- [ ] **Step 7: 重写 `login/page.tsx`**

把 `scm-web/src/app/[locale]/(auth)/login/page.tsx` 改成下面的骨架 —— 会话建立逻辑先留在页面里,Task 4 再抽出去:

```tsx
"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { App } from "antd";
import { useQueryClient } from "@tanstack/react-query";

import { LoginCard } from "@/components/auth/login-card";
import { LoginForm } from "@/components/auth/login-form";
import type { LoginFormValues } from "@/components/auth/login-schema";
import { useAuthStore } from "@/stores/useAuthStore";
import { toSessionUser, mergeUserInfo } from "@/lib/auth/session";
import { authService } from "@/lib/api/services/auth";

export default function LoginPage() {
  const t = useTranslations("login");
  const locale = useLocale();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { message } = App.useApp();
  const { login, setUser, setPermissions } = useAuthStore();
  const [mfaValues, setMfaValues] = useState<LoginFormValues | null>(null);

  const completeLogin = async (response: Parameters<typeof toSessionUser>[0]) => {
    if (!response.accessToken) {
      message.error(response.message || t("failed"));
      return;
    }
    login(toSessionUser(response), response.accessToken, response.refreshToken ?? "");
    setPermissions(response.permissions ?? []);
    try {
      const userInfo = await authService.getUserInfo();
      setUser(mergeUserInfo(useAuthStore.getState().user, userInfo));
    } catch {
      /* token 有效,资料补全是尽力而为 */
    }
    queryClient.clear();
    message.success(t("success"));
    router.push(`/${locale}/dashboard`);
  };

  if (mfaValues) {
    return (
      <LoginCard title={t("mfa.title")} subtitle={t("mfa.subtitle")}>
        {/* Task 3 只需要主流程;MFA 表单在 Task 4 一并接线 */}
      </LoginCard>
    );
  }

  return (
    <LoginCard title={t("title")} subtitle={t("subtitle")}>
      <LoginForm
        onSuccess={(response) => void completeLogin(response)}
        onMfaRequired={(values) => setMfaValues(values)}
        onError={(error) => message.error(error.message || t("failed"))}
      />
    </LoginCard>
  );
}
```

- [ ] **Step 8: 运行测试确认通过**

Run: `cd scm-web && npm test -- src/components/auth/login-schema.test.ts src/components/auth/login-form.test.tsx`
Expected: PASS(schema 5 个用例 + 表单 4 个用例)

- [ ] **Step 9: 提交**

```bash
cd scm-web
git add src/components/auth/login-schema.ts src/components/auth/login-schema.test.ts \
        src/components/auth/login-card.tsx src/components/auth/login-form.tsx \
        src/components/auth/login-form.test.tsx "src/app/[locale]/(auth)/login/page.tsx"
git commit -m "feat(web): 登录卡片与表单迁移到 shadcn/ui + React Hook Form + Zod"
```

---

### Task 4: 共享会话建立 hook 与 TOTP 两步验证

**Files:**
- Create: `scm-web/src/lib/auth/use-complete-login.ts`
- Create: `scm-web/src/lib/auth/use-complete-login.test.tsx`
- Create: `scm-web/src/components/auth/totp-form.tsx`
- Modify: `scm-web/src/app/[locale]/(auth)/login/page.tsx`

**Interfaces:**
- Consumes: Task 3 的 `LoginFormValues`、`totpSchema`;现有 `useAuthStore`、`authService`、`toSessionUser`、`mergeUserInfo`
- Produces:
  - `useCompleteLogin(): (response: LoginResponse) => Promise<void>` —— 建立会话:写 store → 补资料 → 清 query cache → 跳 `/{locale}/dashboard`
  - `TotpForm(props: { pendingCredentials: LoginFormValues; onBack: () => void; onSuccess: (response: LoginResponse) => void })`

- [ ] **Step 1: 写失败的测试**

创建 `scm-web/src/lib/auth/use-complete-login.test.tsx`:

```tsx
import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { useCompleteLogin } from "./use-complete-login";

const push = vi.fn();
const clear = vi.fn();
const login = vi.fn();
const setPermissions = vi.fn();
const setUser = vi.fn();
const getUserInfo = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
  usePathname: () => "/zh-CN/login",
}));

vi.mock("next-intl", () => ({ useLocale: () => "zh-CN" }));

vi.mock("@tanstack/react-query", () => ({
  useQueryClient: () => ({ clear }),
}));

vi.mock("@/stores/useAuthStore", () => ({
  useAuthStore: Object.assign(
    () => ({ login, setUser, setPermissions }),
    { getState: () => ({ user: null }) }
  ),
}));

vi.mock("@/lib/api/services/auth", () => ({
  authService: { getUserInfo: (...args: unknown[]) => getUserInfo(...args) },
}));

describe("useCompleteLogin", () => {
  beforeEach(() => {
    push.mockReset();
    clear.mockReset();
    login.mockReset();
    setPermissions.mockReset();
    setUser.mockReset();
    getUserInfo.mockReset();
    getUserInfo.mockResolvedValue({ userId: "u", username: "admin", realName: "管理员" });
  });

  it("stores the tokens, clears the query cache and redirects to the dashboard", async () => {
    const { result } = renderHook(() => useCompleteLogin());

    await act(async () => {
      await result.current({
        accessToken: "at",
        refreshToken: "rt",
        userId: "u",
        username: "admin",
        realName: "管理员",
        permissions: ["system:user:list"],
      });
    });

    expect(login).toHaveBeenCalledWith(
      expect.objectContaining({ id: "u", username: "admin" }),
      "at",
      "rt"
    );
    expect(setPermissions).toHaveBeenCalledWith(["system:user:list"]);
    expect(clear).toHaveBeenCalled();
    await waitFor(() => expect(push).toHaveBeenCalledWith("/zh-CN/dashboard"));
  });

  it("returns a failure result instead of throwing when the response has no token", async () => {
    const { result } = renderHook(() => useCompleteLogin());

    let outcome: Awaited<ReturnType<typeof result.current>> | undefined;
    await act(async () => {
      // 后端以 HTTP 200 + 错误信封拒绝 —— 没有 accessToken
      outcome = await result.current({ message: "账号已锁定" });
    });

    expect(outcome).toEqual({ ok: false, message: "账号已锁定" });
    // 关键:没有建会话、没有跳转 —— 也不能抛异常(抛了就是 unhandled rejection,用户无反馈)
    expect(login).not.toHaveBeenCalled();
    expect(setPermissions).not.toHaveBeenCalled();
    expect(push).not.toHaveBeenCalled();
  });

  it("falls back to an empty message when the failure has none", async () => {
    const { result } = renderHook(() => useCompleteLogin());

    let outcome: Awaited<ReturnType<typeof result.current>> | undefined;
    await act(async () => {
      outcome = await result.current({});
    });

    expect(outcome).toEqual({ ok: false, message: "" });
  });

  it("reports success so the caller knows to show the success toast", async () => {
    const { result } = renderHook(() => useCompleteLogin());

    let outcome: Awaited<ReturnType<typeof result.current>> | undefined;
    await act(async () => {
      outcome = await result.current({
        accessToken: "at",
        refreshToken: "rt",
        userId: "u",
        username: "admin",
      });
    });

    expect(outcome).toEqual({ ok: true });
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd scm-web && npm test -- src/lib/auth/use-complete-login.test.tsx`
Expected: FAIL —— `Cannot find module './use-complete-login'`

- [ ] **Step 3: 创建 `use-complete-login.ts`**

`scm-web/src/lib/auth/use-complete-login.ts`:

```tsx
"use client";

import { useRouter } from "next/navigation";
import { useLocale } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";

import { useAuthStore } from "@/stores/useAuthStore";
import { toSessionUser, mergeUserInfo } from "@/lib/auth/session";
import { authService } from "@/lib/api/services/auth";
import type { LoginResponse } from "@/lib/api/contracts/auth";

/**
 * 建立会话的结果。**不要用抛异常表达失败** —— 这个 hook 由 `onSuccess` 回调调用,
 * 而 `onSuccess` 的签名是 `(response) => void`,返回的 promise 没人 await:
 * 抛出去就变成 unhandled rejection,用户点完登录**毫无反馈**。
 * 用可判别联合逼调用方处理失败分支。
 */
export type LoginCompletion = { ok: true } | { ok: false; message: string };

/**
 * 建立会话的共享逻辑。密码登录与社交登录回调页共用。
 * 顺序不可调换:先写 store(后续 getUserInfo 才带得上 token),再清 query cache,最后跳路由。
 */
export function useCompleteLogin() {
  const router = useRouter();
  const locale = useLocale();
  const queryClient = useQueryClient();
  const { login, setUser, setPermissions } = useAuthStore();

  return async (response: LoginResponse): Promise<LoginCompletion> => {
    // 后端可能以 HTTP 200 返回带 message 的错误信封 —— 此时没有 token,不能进 store。
    // 旧页面在这里 `message.error(...)` 然后停在原地;现在把文案交给调用方去弹。
    if (!response.accessToken) {
      return { ok: false, message: response.message ?? "" };
    }

    login(toSessionUser(response), response.accessToken, response.refreshToken ?? "");
    setPermissions(response.permissions ?? []);

    try {
      const userInfo = await authService.getUserInfo();
      setUser(mergeUserInfo(useAuthStore.getState().user, userInfo));
    } catch {
      /* token 有效,资料补全是尽力而为 */
    }

    queryClient.clear();
    router.push(`/${locale}/dashboard`);
    return { ok: true };
  };
}
```

> `LoginResponse.message` 在契约里是可选的;`?? ""` 让调用方能退回自己的兜底文案
> (`error.message || t("failed")` 的写法依赖空串是假值)。

- [ ] **Step 4: 写失败的测试(TOTP 表单 + MFA 分流)**

创建 `scm-web/src/components/auth/totp-form.test.tsx`:

```tsx
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { TotpForm } from "./totp-form";

const login = vi.fn();

vi.mock("@/lib/api/services/auth", () => ({
  authService: { login: (...args: unknown[]) => login(...args) },
}));

// useMutation 需要 QueryClient 上下文;直接 mock 掉,让测试只关心表单行为
vi.mock("@tanstack/react-query", () => ({
  useMutation: () => ({
    mutateAsync: (...args: unknown[]) => login(...args),
    isPending: false,
  }),
}));

vi.mock("next-intl", () => ({
  // 模拟命名空间:useTranslations("login") 的 t("mfa.submit") 解析为 login.mfa.submit
  useTranslations: (ns: string) => (key: string) => `${ns}.${key}`,
}));

const credentials = { username: "admin", password: "admin123" };

describe("TotpForm", () => {
  beforeEach(() => {
    login.mockReset();
  });

  it("sends the pending credentials together with the code", async () => {
    const onSuccess = vi.fn();
    login.mockResolvedValue({ accessToken: "t", refreshToken: "r", userId: "u", username: "admin" });
    render(<TotpForm pendingCredentials={credentials} onBack={vi.fn()} onSuccess={onSuccess} />);

    await userEvent.type(screen.getByLabelText("login.mfa.title"), "123456");
    await userEvent.click(screen.getByRole("button", { name: "login.mfa.submit" }));

    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
    expect(login).toHaveBeenCalledWith({ ...credentials, twoFactorCode: "123456" });
  });

  it("rejects a code that is not six digits before hitting the API", async () => {
    render(<TotpForm pendingCredentials={credentials} onBack={vi.fn()} onSuccess={vi.fn()} />);

    await userEvent.type(screen.getByLabelText("login.mfa.title"), "123");
    await userEvent.click(screen.getByRole("button", { name: "login.mfa.submit" }));

    await waitFor(() => {
      expect(screen.getByText("login.errors.codeLength")).toBeInTheDocument();
    });
    expect(login).not.toHaveBeenCalled();
  });

  it("surfaces a rejected code without leaving the TOTP step", async () => {
    const onSuccess = vi.fn();
    login.mockRejectedValue(
      Object.assign(new Error("验证码错误"), {
        response: { status: 400, data: { code: 40002, message: "验证码错误" } },
      })
    );
    render(<TotpForm pendingCredentials={credentials} onBack={vi.fn()} onSuccess={onSuccess} />);

    await userEvent.type(screen.getByLabelText("login.mfa.title"), "000000");
    await userEvent.click(screen.getByRole("button", { name: "login.mfa.submit" }));

    await waitFor(() => {
      expect(screen.getByText("验证码错误")).toBeInTheDocument();
    });
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it("goes back to the password step", async () => {
    const onBack = vi.fn();
    render(<TotpForm pendingCredentials={credentials} onBack={onBack} onSuccess={vi.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: "login.mfa.back" }));
    expect(onBack).toHaveBeenCalledTimes(1);
    expect(login).not.toHaveBeenCalled();
  });
});
```

> 验证码输入框需要一个 `<Label htmlFor="code">` 才能被 `getByLabelText` 找到 ——
> 上面第一条用例用 `login.mfa.title` 当标签文案,所以 `totp-form.tsx` 里那个
> `FormItem` 必须补一个 `<Label htmlFor="code">{t("mfa.title")}</Label>`。
> 这是 Task 3 的登录表单已经建立的同一套约定(标签 + `htmlFor` 与输入框 `id` 对齐)。

同时在 `scm-web/src/components/auth/login-form.test.tsx` 末尾追加两条 MFA 分流用例
(它们钉住的是「密码登录零回归」的核心保证 —— `isMfaRequired` 判定错一次,TOTP 用户就永远进不了第二步):

```tsx
  it("routes a 40104 challenge to the MFA step, not to the error path", async () => {
    const onMfaRequired = vi.fn();
    const onError = vi.fn();
    login.mockRejectedValue(
      Object.assign(new Error("需要二次验证"), {
        response: { status: 401, data: { code: 40104, message: "需要二次验证" } },
      })
    );
    render(<LoginForm onSuccess={vi.fn()} onMfaRequired={onMfaRequired} onError={onError} />);

    await userEvent.type(screen.getByLabelText("login.username"), "admin");
    await userEvent.type(screen.getByLabelText("login.password"), "admin123");
    await userEvent.click(screen.getByRole("button", { name: "login.submit" }));

    await waitFor(() => expect(onMfaRequired).toHaveBeenCalledTimes(1));
    expect(onMfaRequired).toHaveBeenCalledWith({ username: "admin", password: "admin123", remember: true });
    expect(onError).not.toHaveBeenCalled();
  });

  it("routes a message-only MFA challenge the same way", async () => {
    const onMfaRequired = vi.fn();
    const onError = vi.fn();
    login.mockRejectedValue(
      Object.assign(new Error("MFA required"), {
        response: { status: 401, data: { code: 0, message: "MFA required" } },
      })
    );
    render(<LoginForm onSuccess={vi.fn()} onMfaRequired={onMfaRequired} onError={onError} />);

    await userEvent.type(screen.getByLabelText("login.username"), "admin");
    await userEvent.type(screen.getByLabelText("login.password"), "admin123");
    await userEvent.click(screen.getByRole("button", { name: "login.submit" }));

    await waitFor(() => expect(onMfaRequired).toHaveBeenCalledTimes(1));
    expect(onError).not.toHaveBeenCalled();
  });
```

> 断言 `onError` **没有**被调用,与断言 `onMfaRequired` 被调用同样重要:
> 两条路径互斥,只测其中一条会让「两个都触发」的实现照样变绿。

- [ ] **Step 5: 运行测试确认失败**

Run: `cd scm-web && npm test -- src/components/auth/totp-form.test.tsx`
Expected: FAIL —— `Failed to resolve import "./totp-form"`

- [ ] **Step 6: 创建 `totp-form.tsx`**

`scm-web/src/components/auth/totp-form.tsx`:

```tsx
"use client";

import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Form, FormField, FormItem } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { authService } from "@/lib/api/services/auth";
import { toApiError } from "@/lib/api/envelope";
import type { LoginResponse } from "@/lib/api/contracts/auth";

import { buildTotpSchema, type LoginFormValues, type TotpFormValues } from "./login-schema";

interface TotpFormProps {
  pendingCredentials: LoginFormValues;
  onBack: () => void;
  onSuccess: (response: LoginResponse) => void;
}

export function TotpForm({ pendingCredentials, onBack, onSuccess }: TotpFormProps) {
  const t = useTranslations("login");
  const form = useForm<TotpFormValues>({
    resolver: zodResolver(buildTotpSchema(t)),
    defaultValues: { code: "" },
  });

  const loginMutation = useMutation({
    mutationFn: (data: { username: string; password: string; twoFactorCode: string }) =>
      authService.login(data),
  });

  const onSubmit = async (values: TotpFormValues) => {
    try {
      const response = await loginMutation.mutateAsync({
        username: pendingCredentials.username,
        password: pendingCredentials.password,
        twoFactorCode: values.code,
      });
      onSuccess(response);
    } catch (error: unknown) {
      form.setError("code", { message: toApiError(error).message || t("errors.codeInvalid") });
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <FormField
          control={form.control}
          name="code"
          render={({ field }) => (
            <FormItem>
              <Label htmlFor="code">{t("mfa.title")}</Label>
              <Input
                id="code"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                placeholder={t("mfa.codePlaceholder")}
                className="h-12 rounded-lg bg-transparent text-center text-lg tracking-[0.5em]"
                {...field}
              />
              {form.formState.errors.code && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.code.message}
                </p>
              )}
            </FormItem>
          )}
        />

        <Button
          type="submit"
          disabled={loginMutation.isPending}
          className="h-12 w-full rounded-lg bg-[var(--auth-btn)] text-[var(--auth-btn-fg)] hover:bg-[var(--auth-ink-soft)]"
        >
          {t("mfa.submit")}
        </Button>

        <Button
          type="button"
          variant="ghost"
          onClick={onBack}
          className="text-[var(--auth-ink-soft)]"
        >
          {t("mfa.back")}
        </Button>
      </form>
    </Form>
  );
}
```

- [ ] **Step 7: 接线 `login/page.tsx`(含 TOTP 分支)**

把 `scm-web/src/app/[locale]/(auth)/login/page.tsx` 改成:

```tsx
"use client";

import { useState } from "react";
import { App } from "antd";
import { useTranslations } from "next-intl";

import { LoginCard } from "@/components/auth/login-card";
import { LoginForm } from "@/components/auth/login-form";
import { TotpForm } from "@/components/auth/totp-form";
import type { LoginFormValues } from "@/components/auth/login-schema";
import { useCompleteLogin } from "@/lib/auth/use-complete-login";
import type { LoginResponse } from "@/lib/api/contracts/auth";

export default function LoginPage() {
  const t = useTranslations("login");
  const { message } = App.useApp();
  const completeLogin = useCompleteLogin();
  const [pendingCredentials, setPendingCredentials] = useState<LoginFormValues | null>(null);

  /**
   * 密码登录与 TOTP 登录共用同一个收尾:先建会话,成功了才提示成功。
   * 不能先 `message.success` 再建会话 —— 后端以 200 + 错误信封返回时没有 token,
   * 那样会先闪一句「登录成功」再毫无反应地停在原页。
   */
  const handleSuccess = async (response: LoginResponse) => {
    try {
      const result = await completeLogin(response);
      if (!result.ok) {
        message.error(result.message || t("failed"));
        return;
      }
      message.success(t("success"));
    } catch {
      // 兜底:建会话途中的意外异常也不能变成静默失败
      message.error(t("failed"));
    }
  };

  if (pendingCredentials) {
    return (
      <LoginCard title={t("mfa.title")} subtitle={t("mfa.subtitle")}>
        <TotpForm
          pendingCredentials={pendingCredentials}
          onBack={() => setPendingCredentials(null)}
          onSuccess={(response) => void handleSuccess(response)}
        />
      </LoginCard>
    );
  }

  return (
    <LoginCard title={t("title")} subtitle={t("subtitle")}>
      <LoginForm
        onSuccess={(response) => void handleSuccess(response)}
        onMfaRequired={setPendingCredentials}
        onError={(error) => message.error(error.message || t("failed"))}
      />
    </LoginCard>
  );
}
```

> `completeLogin` 负责建会话与跳转,页面负责所有提示 —— 且**成功提示在会话建立之后**,
> 与原实现一致(旧页面的 `message.success` 也在 `completeLogin` 内部、store 写完才调)。
> `onError` 接住非 MFA 失败(密码错误 / 账号锁定 / 网络错误)并弹后端返回的文案,
> 与原页面 `message.error(apiError.message || '登录失败')` 等价。
> `onError` 接住非 MFA 失败(密码错误 / 账号锁定 / 网络错误)并弹后端返回的文案,
> 与原页面 `message.error(apiError.message || '登录失败')` 等价。

- [ ] **Step 8: 运行测试确认通过**

Run: `cd scm-web && npm test -- src/lib/auth/use-complete-login.test.tsx src/components/auth/totp-form.test.tsx src/components/auth/login-form.test.tsx`
Expected: PASS(hook 4 个用例 + TOTP 表单 4 个用例 + 登录表单 6 个用例)

- [ ] **Step 9: 跑全量前端测试与类型检查**

Run: `cd scm-web && npm test && npm run typecheck`
Expected: 全部 PASS;typecheck 无错误

- [ ] **Step 10: 提交**

```bash
cd scm-web
git add src/lib/auth/use-complete-login.ts src/lib/auth/use-complete-login.test.tsx \
        src/components/auth/totp-form.tsx src/components/auth/totp-form.test.tsx \
        src/components/auth/login-form.test.tsx \
        "src/app/[locale]/(auth)/login/page.tsx"
git commit -m "refactor(web): 抽取共享会话建立 hook 并接入 TOTP 两步验证"
```

---

### Task 5: 找回密码页迁移 + 删除旧 antd 组件

**Files:**
- Modify: `scm-web/src/app/[locale]/(auth)/forgot-password/page.tsx`
- Create: `scm-web/src/components/auth/forgot-password-form.tsx`
- Create: `scm-web/src/components/auth/forgot-password-form.test.tsx`
- Delete: `scm-web/src/components/ui/auth-brand-card.tsx`
- Delete: `scm-web/src/layouts/auth-layout.tsx`

**Interfaces:**
- Consumes: Task 1 的令牌;Task 2 的 `AuthShell`;Task 3 的 `LoginCard`
- Produces: 无(本任务是收口,不对外提供新接口)

- [ ] **Step 1: 写失败的测试**

创建 `scm-web/src/components/auth/forgot-password-form.test.tsx`:

```tsx
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { ForgotPasswordForm } from "./forgot-password-form";

vi.mock("next-intl", () => ({
  // 模拟 next-intl 的命名空间行为:useTranslations("forgot") 返回的 t 会把命名空间拼进 key
  useTranslations: (ns: string) => (key: string) => `${ns}.${key}`,
}));

/** 走完邮箱步,停在验证码步。 */
const advanceToCodeStep = async () => {
  await userEvent.type(screen.getByLabelText("forgot.email"), "ops@example.com");
  await userEvent.click(screen.getByRole("button", { name: "forgot.sendCode" }));
  await waitFor(() => expect(screen.getByText("forgot.codeSent")).toBeInTheDocument());
};

/** 走完验证码步,停在重置密码步。 */
const advanceToPasswordStep = async () => {
  await advanceToCodeStep();
  await userEvent.type(screen.getByLabelText("forgot.code"), "123456");
  await userEvent.click(screen.getByRole("button", { name: "forgot.verify" }));
  await waitFor(() => expect(screen.getByLabelText("forgot.newPassword")).toBeInTheDocument());
};

describe("ForgotPasswordForm", () => {
  it("requires a valid email before sending a code", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: "forgot.sendCode" }));
    await waitFor(() => {
      expect(screen.getByText("forgot.errors.emailRequired")).toBeInTheDocument();
    });
  });

  it("rejects a malformed email", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);

    await userEvent.type(screen.getByLabelText("forgot.email"), "not-an-email");
    await userEvent.click(screen.getByRole("button", { name: "forgot.sendCode" }));

    await waitFor(() => {
      expect(screen.getByText("forgot.errors.emailInvalid")).toBeInTheDocument();
    });
  });

  it("advances to the code step after a valid email", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);

    await advanceToCodeStep();
  });

  it("blocks the code step until six digits are entered", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);
    await advanceToCodeStep();

    await userEvent.type(screen.getByLabelText("forgot.code"), "123");
    await userEvent.click(screen.getByRole("button", { name: "forgot.verify" }));

    await waitFor(() => {
      expect(screen.getByText("forgot.errors.codeLength")).toBeInTheDocument();
    });
    // 没通过校验就不能进入下一步
    expect(screen.getByText("forgot.codeSent")).toBeInTheDocument();
  });

  it("advances to the password step with a six-digit code", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);

    await advanceToPasswordStep();
  });

  it("rejects a password shorter than eight characters", async () => {
    const onDone = vi.fn();
    render(<ForgotPasswordForm onDone={onDone} />);
    await advanceToPasswordStep();

    await userEvent.type(screen.getByLabelText("forgot.newPassword"), "short");
    await userEvent.type(screen.getByLabelText("forgot.confirmPassword"), "short");
    await userEvent.click(screen.getByRole("button", { name: "forgot.reset" }));

    await waitFor(() => {
      expect(screen.getByText("forgot.errors.passwordMin")).toBeInTheDocument();
    });
    expect(onDone).not.toHaveBeenCalled();
  });

  it("rejects a mismatched confirmation", async () => {
    const onDone = vi.fn();
    render(<ForgotPasswordForm onDone={onDone} />);
    await advanceToPasswordStep();

    await userEvent.type(screen.getByLabelText("forgot.newPassword"), "admin12345");
    await userEvent.type(screen.getByLabelText("forgot.confirmPassword"), "admin12346");
    await userEvent.click(screen.getByRole("button", { name: "forgot.reset" }));

    await waitFor(() => {
      expect(screen.getByText("forgot.errors.passwordMismatch")).toBeInTheDocument();
    });
    expect(onDone).not.toHaveBeenCalled();
  });

  it("reports completion once the passwords match", async () => {
    const onDone = vi.fn();
    render(<ForgotPasswordForm onDone={onDone} />);
    await advanceToPasswordStep();

    await userEvent.type(screen.getByLabelText("forgot.newPassword"), "admin12345");
    await userEvent.type(screen.getByLabelText("forgot.confirmPassword"), "admin12345");
    await userEvent.click(screen.getByRole("button", { name: "forgot.reset" }));

    await waitFor(() => expect(onDone).toHaveBeenCalledTimes(1));
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd scm-web && npm test -- src/components/auth/forgot-password-form.test.tsx`
Expected: FAIL —— `Cannot find module './forgot-password-form'`

- [ ] **Step 3: 创建 `forgot-password-form.tsx`**

`scm-web/src/components/auth/forgot-password-form.tsx`:

```tsx
"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";

import { Form, FormField, FormItem } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

/** next-intl 的 t:key 进、文案出。三个 schema 工厂共用。 */
type Translator = (key: string) => string;

const buildEmailSchema = (t: Translator) =>
  z.object({
    email: z.string().min(1, t("errors.emailRequired")).email(t("errors.emailInvalid")),
  });

const buildCodeSchema = (t: Translator) =>
  z.object({
    code: z.string().min(1, t("errors.codeRequired")).length(6, t("errors.codeLength")),
  });

const buildPasswordSchema = (t: Translator) =>
  z
    .object({
      password: z.string().min(1, t("errors.passwordRequired")).min(8, t("errors.passwordMin")),
      confirmPassword: z.string().min(1, t("errors.confirmRequired")),
    })
    .refine((values) => values.password === values.confirmPassword, {
      message: t("errors.passwordMismatch"),
      path: ["confirmPassword"],
    });

type EmailValues = z.infer<ReturnType<typeof buildEmailSchema>>;
type CodeValues = z.infer<ReturnType<typeof buildCodeSchema>>;
type PasswordValues = z.infer<ReturnType<typeof buildPasswordSchema>>;

type Step = "email" | "code" | "password";

interface ForgotPasswordFormProps {
  /** 三步都通过(模拟重置成功)。提示与跳转由页面负责 —— 与 LoginForm 的分工一致。 */
  onDone: () => void;
}

/** 三个步骤共用的主按钮样式,免得同一串令牌重复四遍。 */
const primaryButtonClass =
  "h-12 w-full rounded-lg bg-[var(--auth-btn)] text-[var(--auth-btn-fg)] hover:bg-[var(--auth-ink-soft)]";

export function ForgotPasswordForm({ onDone }: ForgotPasswordFormProps) {
  const t = useTranslations("forgot");
  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");

  // 每一步一个表单:三步的校验目标互不相干,共用一个表单只会让 defaultValues 越滚越大。
  const emailForm = useForm<EmailValues>({
    resolver: zodResolver(buildEmailSchema(t)),
    defaultValues: { email: "" },
  });
  const codeForm = useForm<CodeValues>({
    resolver: zodResolver(buildCodeSchema(t)),
    defaultValues: { code: "" },
  });
  const passwordForm = useForm<PasswordValues>({
    resolver: zodResolver(buildPasswordSchema(t)),
    defaultValues: { password: "", confirmPassword: "" },
  });

  const onEmailSubmit = (values: EmailValues) => {
    setEmail(values.email);
    setStep("code");
  };

  return (
    <div className="flex flex-col gap-4">
      {step === "email" && (
        <Form {...emailForm}>
          <form
            onSubmit={emailForm.handleSubmit(onEmailSubmit)}
            className="flex flex-col gap-4"
            noValidate
          >
            <FormField
              control={emailForm.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <Label htmlFor="email">{t("email")}</Label>
                  <Input
                    id="email"
                    type="email"
                    autoComplete="email"
                    placeholder={t("emailPlaceholder")}
                    className="h-12 rounded-lg bg-transparent"
                    {...field}
                  />
                  {emailForm.formState.errors.email && (
                    <p className="text-sm text-destructive">
                      {emailForm.formState.errors.email.message}
                    </p>
                  )}
                </FormItem>
              )}
            />
            <Button type="submit" className={primaryButtonClass}>
              {t("sendCode")}
            </Button>
          </form>
        </Form>
      )}

      {step === "code" && (
        <Form {...codeForm}>
          <form
            onSubmit={codeForm.handleSubmit(() => setStep("password"))}
            className="flex flex-col gap-4"
            noValidate
          >
            <p className="text-sm text-[var(--auth-ink-soft)]">{t("codeSent", { email })}</p>
            <FormField
              control={codeForm.control}
              name="code"
              render={({ field }) => (
                <FormItem>
                  <Label htmlFor="code">{t("code")}</Label>
                  <Input
                    id="code"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    maxLength={6}
                    placeholder={t("codePlaceholder")}
                    className="h-12 rounded-lg bg-transparent text-center text-lg tracking-[0.5em]"
                    {...field}
                  />
                  {codeForm.formState.errors.code && (
                    <p className="text-sm text-destructive">
                      {codeForm.formState.errors.code.message}
                    </p>
                  )}
                </FormItem>
              )}
            />
            <Button type="submit" className={primaryButtonClass}>
              {t("verify")}
            </Button>
          </form>
        </Form>
      )}

      {step === "password" && (
        <Form {...passwordForm}>
          <form
            onSubmit={passwordForm.handleSubmit(() => onDone())}
            className="flex flex-col gap-4"
            noValidate
          >
            <FormField
              control={passwordForm.control}
              name="password"
              render={({ field }) => (
                <FormItem>
                  <Label htmlFor="password">{t("newPassword")}</Label>
                  <Input
                    id="password"
                    type="password"
                    autoComplete="new-password"
                    placeholder={t("newPasswordPlaceholder")}
                    className="h-12 rounded-lg bg-transparent"
                    {...field}
                  />
                  {passwordForm.formState.errors.password && (
                    <p className="text-sm text-destructive">
                      {passwordForm.formState.errors.password.message}
                    </p>
                  )}
                </FormItem>
              )}
            />
            <FormField
              control={passwordForm.control}
              name="confirmPassword"
              render={({ field }) => (
                <FormItem>
                  <Label htmlFor="confirmPassword">{t("confirmPassword")}</Label>
                  <Input
                    id="confirmPassword"
                    type="password"
                    autoComplete="new-password"
                    placeholder={t("confirmPasswordPlaceholder")}
                    className="h-12 rounded-lg bg-transparent"
                    {...field}
                  />
                  {passwordForm.formState.errors.confirmPassword && (
                    <p className="text-sm text-destructive">
                      {passwordForm.formState.errors.confirmPassword.message}
                    </p>
                  )}
                </FormItem>
              )}
            />
            <Button type="submit" className={primaryButtonClass}>
              {t("reset")}
            </Button>
          </form>
        </Form>
      )}
    </div>
  );
}
```

> 找回密码的后端接口在仓库里本来就没有(`forgot-password/page.tsx` 原来也只是本地 `setCurrentStep` 模拟,没有真实请求)。本 Task 保持同样的行为边界,不新增后端调用 —— **但三步的校验规则必须与旧页面逐条对齐**:
> 验证码必填且 6 位;新密码必填且至少 8 位;确认密码必填且两次一致。旧页面这三条规则就是它全部的行为。

- [ ] **Step 4: 改写 `forgot-password/page.tsx`**

`scm-web/src/app/[locale]/(auth)/forgot-password/page.tsx`:

```tsx
"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { App } from "antd";

import { LoginCard } from "@/components/auth/login-card";
import { ForgotPasswordForm } from "@/components/auth/forgot-password-form";

export default function ForgotPasswordPage() {
  const t = useTranslations("forgot");
  const locale = useLocale();
  const router = useRouter();
  const { message } = App.useApp();

  // 找回密码没有后端接口,三步走完即视为重置成功 ——
  // 与原页面一致:弹一句成功提示,然后回登录页。不能什么都不做(那是个死胡同)。
  const handleDone = () => {
    message.success(t("resetSuccess"));
    router.push(`/${locale}/login`);
  };

  return (
    <LoginCard title={t("title")} subtitle={t("subtitle")}>
      <ForgotPasswordForm onDone={handleDone} />
      {/* 旧页面卡片底部有「返回登录」入口。丢了它,用户从登录页点进来就再也回不去了。 */}
      <p className="mt-6 text-center text-sm">
        <Link
          href={`/${locale}/login`}
          className="text-[var(--auth-ink-soft)] underline-offset-4 hover:text-[var(--auth-ink)] hover:underline"
        >
          {t("back")}
        </Link>
      </p>
    </LoginCard>
  );
}
```

- [ ] **Step 5: 删除旧 antd 组件并确认无残留引用**

```bash
cd scm-web
git rm src/components/ui/auth-brand-card.tsx src/layouts/auth-layout.tsx
```

同时从 `src/lib/design-tokens.css` 的 `:root` 里删掉 `--auth-card-shadow`
(`auth-brand-card.tsx` 是它最后一个消费者;**不要**删 `--auth-brand-bg` ——
它还被 `src/components/Layout/sidebar.tsx` 用着)。

> `design-tokens.test.ts` 里的契约守卫会自动发现这个令牌已无人引用 —— 它断言的是
> 「被引用的令牌都有定义」,不是「定义的令牌都被引用」,所以删除它不会让测试变红。

然后全局搜索确认没有引用残留:

Run: `grep -rn "AuthBrandCard\|auth-layout\|AuthLayout" src/`
Expected: 无输出(有输出就把该引用改掉再删)

- [ ] **Step 6: 运行测试确认通过**

Run: `cd scm-web && npm test -- src/components/auth/forgot-password-form.test.tsx`
Expected: PASS(8 个用例)

- [ ] **Step 7: 提交**

```bash
cd scm-web
git add src/components/auth/forgot-password-form.tsx \
        src/components/auth/forgot-password-form.test.tsx \
        "src/app/[locale]/(auth)/forgot-password/page.tsx"
git commit -m "refactor(web): 找回密码页迁移到 shadcn/ui 并移除旧 antd 认证组件"
```

---

### Task 6: 双语文案、e2e 断言更新与收尾

**Files:**
- Modify: `scm-web/messages/zh-CN.json`
- Modify: `scm-web/messages/en-US.json`
- Modify: `scm-web/src/components/auth/auth-shell.tsx`(顶栏文案改走 i18n)
- Modify: `scm-web/src/components/auth/login-form.tsx`(忘记密码链接改走 `useLocale()`)
- Modify: `scm-web/src/components/auth/auth-shell.test.tsx`(断言跟随文案变化)
- Modify: `scm-web/e2e/login.spec.ts`
- Create: `scm-web/src/lib/i18n-messages.test.ts`

**Interfaces:**
- Consumes: Task 1–5 产出的组件与文案 key
- Produces: `messages/{zh-CN,en-US}.json` 里的 `login` 与 `forgot` 两个命名空间

- [ ] **Step 1: 写失败的测试(i18n 对齐)**

创建 `scm-web/src/lib/i18n-messages.test.ts`:

```ts
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

function load(locale: string): Record<string, unknown> {
  return JSON.parse(
    readFileSync(join(process.cwd(), `messages/${locale}.json`), "utf8")
  ) as Record<string, unknown>;
}

function keysOf(value: unknown, prefix = ""): string[] {
  if (typeof value !== "object" || value === null) return [prefix];
  return Object.entries(value as Record<string, unknown>).flatMap(([k, v]) =>
    keysOf(v, prefix ? `${prefix}.${k}` : k)
  );
}

/** 递归取到叶子节点:命名空间下有嵌套对象(errors / mfa / social),浅层 spread 取不到叶子。 */
function leavesOf(value: unknown, prefix = ""): Array<[string, unknown]> {
  if (typeof value !== "object" || value === null) return [[prefix, value]];
  return Object.entries(value as Record<string, unknown>).flatMap(([k, v]) =>
    leavesOf(v, prefix ? `${prefix}.${k}` : k)
  );
}

describe("i18n message catalogs", () => {
  it("zh-CN and en-US expose the same auth keys", () => {
    const zh = keysOf(load("zh-CN")).filter((k) => k.startsWith("login.") || k.startsWith("forgot."));
    const en = keysOf(load("en-US")).filter((k) => k.startsWith("login.") || k.startsWith("forgot."));
    expect(zh.sort()).toEqual(en.sort());
    expect(zh.length).toBeGreaterThan(0);
  });

  it("has no empty auth translations", () => {
    for (const locale of ["zh-CN", "en-US"]) {
      const catalog = load(locale) as Record<string, unknown>;
      for (const ns of ["login", "forgot"]) {
        const leaves = leavesOf(catalog[ns], ns);
        expect(leaves.length, `${locale}.${ns}`).toBeGreaterThan(0);
        for (const [key, value] of leaves) {
          expect(typeof value, key).toBe("string");
          expect(String(value).trim().length, key).toBeGreaterThan(0);
        }
      }
    }
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd scm-web && npm test -- src/lib/i18n-messages.test.ts`
Expected: FAIL —— `login` 命名空间不存在,key 列表为空 / 不相等

- [ ] **Step 3: 写 `zh-CN.json` 的 `login` 与 `forgot` 命名空间**

在 `scm-web/messages/zh-CN.json` 的顶层加入(与既有 `app` / `nav` / `analytics` / `dashboard` / `common` 平级):

```json
  "login": {
    "title": "让供应链运转如丝",
    "subtitle": "采购、库存、履约与结算,一个平台贯通",
    "username": "用户名",
    "usernamePlaceholder": "请输入用户名",
    "password": "密码",
    "passwordPlaceholder": "请输入密码",
    "remember": "记住我",
    "forgotPassword": "忘记密码?",
    "submit": "登录",
    "success": "登录成功",
    "failed": "登录失败",
    "legalPrefix": "继续即表示您同意",
    "legalMiddle": "与",
    "terms": "服务条款",
    "privacy": "隐私政策",
    "legalSuffix": ",并同意接收产品通知。",
    "errors": {
      "usernameRequired": "请输入用户名",
      "passwordRequired": "请输入密码",
      "codeRequired": "请输入验证码",
      "codeLength": "验证码为 6 位数字",
      "codeDigits": "验证码只能包含数字",
      "codeInvalid": "验证码错误"
    },
    "mfa": {
      "title": "两步验证",
      "subtitle": "请输入您的 TOTP 验证码",
      "codePlaceholder": "6 位验证码",
      "submit": "验证",
      "back": "返回登录"
    }
  },
  "forgot": {
    "title": "找回密码",
    "subtitle": "通过已验证邮箱重置您的密码",
    "email": "邮箱地址",
    "emailPlaceholder": "请输入注册邮箱",
    "sendCode": "发送验证码",
    "codeSent": "验证码已发送至 {email}",
    "code": "验证码",
    "codePlaceholder": "6 位验证码",
    "verify": "验证",
    "newPassword": "新密码",
    "newPasswordPlaceholder": "新密码",
    "confirmPassword": "确认密码",
    "confirmPasswordPlaceholder": "确认密码",
    "reset": "重置密码",
    "resetSuccess": "密码重置成功",
    "back": "返回登录",
    "errors": {
      "emailRequired": "请输入邮箱地址",
      "emailInvalid": "请输入有效的邮箱地址",
      "codeRequired": "请输入验证码",
      "codeLength": "验证码为 6 位数字",
      "passwordRequired": "请输入新密码",
      "passwordMin": "密码至少 8 位",
      "confirmRequired": "请确认密码",
      "passwordMismatch": "两次密码不一致"
    }
  }
```

- [ ] **Step 4: 写 `en-US.json` 的同名命名空间**

在 `scm-web/messages/en-US.json` 加入**完全相同**的 key 结构:

```json
  "login": {
    "title": "Make your supply chain flow",
    "subtitle": "Procurement, inventory, fulfillment and finance in one place",
    "username": "Username",
    "usernamePlaceholder": "Enter your username",
    "password": "Password",
    "passwordPlaceholder": "Enter your password",
    "remember": "Remember me",
    "forgotPassword": "Forgot password?",
    "submit": "Log in",
    "success": "Signed in",
    "failed": "Sign-in failed",
    "legalPrefix": "By continuing you agree to our",
    "legalMiddle": "and",
    "terms": "Terms of Service",
    "privacy": "Privacy Policy",
    "legalSuffix": ", and consent to receive product updates.",
    "errors": {
      "usernameRequired": "Username is required",
      "passwordRequired": "Password is required",
      "codeRequired": "Verification code is required",
      "codeLength": "The code must be 6 digits",
      "codeDigits": "The code must contain digits only",
      "codeInvalid": "Incorrect verification code"
    },
    "mfa": {
      "title": "Two-step verification",
      "subtitle": "Enter your TOTP verification code",
      "codePlaceholder": "6-digit code",
      "submit": "Verify",
      "back": "Back to sign in"
    }
  },
  "forgot": {
    "title": "Reset your password",
    "subtitle": "Reset your password with a verified email",
    "email": "Email address",
    "emailPlaceholder": "Enter your registered email",
    "sendCode": "Send verification code",
    "codeSent": "Verification code sent to {email}",
    "code": "Verification code",
    "codePlaceholder": "6-digit code",
    "verify": "Verify",
    "newPassword": "New password",
    "newPasswordPlaceholder": "New password",
    "confirmPassword": "Confirm password",
    "confirmPasswordPlaceholder": "Confirm password",
    "reset": "Reset password",
    "resetSuccess": "Password reset",
    "back": "Back to sign in",
    "errors": {
      "emailRequired": "Email address is required",
      "emailInvalid": "Enter a valid email address",
      "codeRequired": "Verification code is required",
      "codeLength": "The code must be 6 digits",
      "passwordRequired": "New password is required",
      "passwordMin": "Password must be at least 8 characters",
      "confirmRequired": "Please confirm your password",
      "passwordMismatch": "The two passwords do not match"
    }
  }
```

- [ ] **Step 5: 把组件里的硬编码文案换成 i18n**

`auth-shell.tsx`:改用 `useTranslations("login")`,把「联系销售」「试用平台」换成 `t("header.contactSales")` / `t("header.tryPlatform")`,并在两个命名空间里补上对应 key:

```json
    "header": {
      "contactSales": "联系销售",
      "tryPlatform": "试用平台"
    }
```
(en-US 侧为 `"Contact sales"` / `"Try the platform"`)

同时把 `brand-mark.tsx` 里的「SCM 供应链管理平台」改为 `t("brandName")`,两边分别是 `"SCM 供应链管理平台"` / `"SCM Supply Chain Platform"`,并在 `login` 命名空间补 `brandName`。

`login-form.tsx`:忘记密码链接改成用 `useLocale()` 拼路径:

```tsx
const locale = useLocale();
...
<a href={`/${locale}/forgot-password`} className="text-sm text-[var(--color-brand-accent)] underline-offset-4 hover:underline">
```

- [ ] **Step 6: 更新 `auth-shell.test.tsx` 的文案断言**

把测试里的三个文案断言改为 key 断言(测试里 `useTranslations` mock 成带命名空间前缀):

```tsx
vi.mock("next-intl", () => ({
  // 模拟 next-intl 的命名空间行为:useTranslations("login") 返回的 t 会把命名空间拼进 key
  useTranslations: (ns: string) => (key: string) => `${ns}.${key}`,
}));
```

断言改为:

```tsx
expect(screen.getByText("login.brandName")).toBeInTheDocument();
expect(screen.getByRole("button", { name: "login.header.contactSales" })).toBeInTheDocument();
expect(screen.getByRole("button", { name: "login.header.tryPlatform" })).toBeInTheDocument();
```

> `brand-mark.tsx` 与 `auth-shell.tsx` **不要**加 `"use client"` —— next-intl 的
> `useTranslations` 在 Server Component 里同样可用,保持它们是服务端组件可以避免
> 多余的 client boundary。`login-form.tsx` 与 `totp-form.tsx` 因为用了 React Hook Form
> 的 hooks,**必须**保留 `"use client"`。

- [ ] **Step 7: 更新 `e2e/login.spec.ts`**

`scm-web/e2e/login.spec.ts` 改成:

```ts
import { test, expect } from "@playwright/test";

test("login page displays correctly", async ({ page }) => {
  await page.goto("/zh-CN/login");
  await expect(page.locator("h1")).toContainText("让供应链运转如丝");
  await expect(page.locator('input[name="username"]')).toBeVisible();
  await expect(page.locator('input[name="password"]')).toBeVisible();
});

test("successful login redirects to dashboard", async ({ page }) => {
  await page.goto("/zh-CN/login");
  await page.fill('input[name="username"]', "admin");
  await page.fill('input[name="password"]', "admin123");
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL(/\/zh-CN\/dashboard/);
});
```

- [ ] **Step 8: 跑全量校验**

Run: `cd scm-web && npm test && npm run typecheck && npm run lint`
Expected: 全部通过

- [ ] **Step 9: 跑生产构建**

Run: `cd scm-web && npm run build`
Expected: 构建成功,无 ESLint / TS 错误

- [ ] **Step 10: 提交**

```bash
cd scm-web
git add messages/zh-CN.json messages/en-US.json \
        src/components/auth/auth-shell.tsx src/components/auth/auth-shell.test.tsx \
        src/components/auth/brand-mark.tsx src/components/auth/login-form.tsx \
        src/lib/i18n-messages.test.ts e2e/login.spec.ts
git commit -m "feat(web): 登录页接入 next-intl 双语文案并更新 e2e 断言"
```

---

## 完成定义

全部 6 个 Task 完成后,下列命令必须全绿:

```bash
cd scm-web
npm run typecheck
npm run lint
npm test
npm run build
```

且下列人工检查项成立:

- `/{locale}/login` 在 `lg` 以上是双栏(左表单右图),`lg` 以下单栏且不显示图片
- 登录卡片含:衬线标题、白卡片、用户名、密码、记住我、忘记密码、近黑「登录」按钮、底部细则
- 密码错误停在登录页并提示,只有 `40104` / 含 MFA 关键字才切到两步验证
- 登录成功后 `access_token` cookie 被写入,刷新页面仍是登录态
- 找回密码页三步可前进后退
- 页面里搜不到任何 `antd` 组件 import(除 `App` 用于 `message`)
- `grep -rn "AuthBrandCard\|auth-layout" scm-web/src/` 无输出

社交登录按钮、回调页与后端端点**不在本计划范围**,见配套计划 `docs/superpowers/plans/2026-10-27-social-auth-google-apple.md`。
