import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const TOKENS = readFileSync(join(process.cwd(), "src/lib/design-tokens.css"), "utf8");
const GLOBALS = readFileSync(join(process.cwd(), "src/app/globals.css"), "utf8");

/**
 * 去掉 CSS 注释再匹配:注释里提到 `--color-accent: var(--accent)` 只是说明文字,
 * 不是令牌声明 —— 断言要盯的是「有没有声明」,不是「有没有提到」。
 */
const DECLARATIONS = TOKENS.replace(/\/\*[\s\S]*?\*\//g, "");

function token(name: string): string {
  const match = DECLARATIONS.match(new RegExp(`--${name}:\\s*([^;]+);`));
  if (!match) throw new Error(`token --${name} not found in design-tokens.css`);
  return match[1].trim();
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

  it("exposes a serif stack that covers Latin and CJK", () => {
    // 令牌在 design-tokens.css 里叫 --scm-font-serif:globals.css 的 @theme inline
    // 要写 --font-serif: var(--scm-font-serif),同名会变成自引用。
    const serif = token("scm-font-serif");
    expect(serif).toContain("Georgia");
    expect(serif).toContain("Songti SC");
    expect(serif).toContain("Noto Serif CJK SC");

    // 别名必须是 --scm-font-serif,不能是自引用。
    expect(GLOBALS).toMatch(/--font-serif:\s*var\(--scm-font-serif\);/);
    expect(GLOBALS).not.toMatch(/--font-serif:\s*var\(--font-serif\)/);
  });
});
