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
