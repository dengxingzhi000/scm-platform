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

  it("does not repaint the global primary for the auth experience", () => {
    // 全局 --color-primary 归应用设计系统所有(现在是紫罗兰)。
    // 登录页的 Claude 橙只能通过 AuthShell 根元素上的 --primary 覆写生效 ——
    // 两者的值必须不同,相等就说明登录页把全局主色也染成了橙色。
    expect(token("color-primary")).not.toBe(token("color-brand-accent"));
  });

  it("does not override the fixed-light auth palette in dark mode", () => {
    expect(DARK_BLOCK).not.toMatch(/--auth-bg\s*:/);
    expect(DARK_BLOCK).not.toMatch(/--auth-ink\s*:/);
  });

  it("exposes a serif stack that covers Latin and CJK", () => {
    // 令牌在 design-tokens.css 里叫 --font-family-serif:globals.css 的 @theme inline
    // 要写 --font-serif: var(--font-family-serif),同名会变成自引用。
    const serif = token("font-family-serif");
    expect(serif).toContain("Georgia");
    expect(serif).toContain("Songti SC");
    expect(serif).toContain("Noto Serif SC");

    // 别名必须落在 @theme inline 块内(普通 :root 不生成工具类),且不能自引用。
    expect(THEME_BLOCK).toMatch(/--font-serif:\s*var\(--font-family-serif\);/);
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
