import { beforeEach, describe, expect, it, vi } from "vitest";

import { useAuthStore } from "./useAuthStore";

const user = { id: "1", username: "admin", displayName: "Admin" };

/**
 * jsdom 的 document.cookie 读回来只剩 name=value,expires 之类的属性看不见 ——
 * 要证明「7 天持久 cookie」还是「会话 cookie」只能看写进去的原始字符串。
 * 这里包一层 setter:既记录写入,又照常写进真实的 cookie jar。
 */
function captureCookieWrites() {
  const writes: string[] = [];
  const original = Object.getOwnPropertyDescriptor(Document.prototype, "cookie")!.set!;
  const spy = vi
    .spyOn(document, "cookie", "set")
    .mockImplementation((value: string) => {
      writes.push(value);
      original.call(document, value);
    });
  return { writes, restore: () => spy.mockRestore() };
}

describe("useAuthStore 的 cookie 持久化", () => {
  beforeEach(() => {
    document.cookie = "access_token=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
    useAuthStore.setState({ rememberMe: true });
  });

  it("记住我时写 access_token cookie", () => {
    useAuthStore.getState().login(user, "at", "rt", true);
    expect(document.cookie).toContain("access_token=at");
  });

  it("记住我时写的是 7 天过期的持久 cookie", () => {
    const { writes, restore } = captureCookieWrites();

    useAuthStore.getState().login(user, "at", "rt", true);
    restore();

    expect(writes).toHaveLength(1);
    expect(writes[0]).toContain("access_token=at");
    const expires = /expires=([^;]+)/.exec(writes[0])?.[1];
    expect(expires).toBeTruthy();
    const days = (new Date(expires!).getTime() - Date.now()) / 864e5;
    expect(days).toBeGreaterThan(6.99);
    expect(days).toBeLessThan(7.01);
  });

  it("不记住我时写的是会话 cookie(没有 expires)", () => {
    const { writes, restore } = captureCookieWrites();
    useAuthStore.getState().login(user, "at", "rt", false);
    restore();
    expect(writes).toHaveLength(1);
    expect(writes[0]).toContain("access_token=at");
    expect(writes[0]).not.toContain("expires=");
    // cookie 必须真的在 —— 否则刷新页面时 proxy.ts 会把人弹回登录页
    expect(document.cookie).toContain("access_token=at");
  });

  it("不记住我时用会话 cookie 覆盖掉之前的持久 cookie", () => {
    useAuthStore.getState().login(user, "at", "rt", true);
    expect(document.cookie).toContain("access_token=at");

    const { writes, restore } = captureCookieWrites();
    useAuthStore.getState().login(user, "at2", "rt", false);
    restore();

    expect(writes[0]).not.toContain("expires=");
    expect(document.cookie).toContain("access_token=at2");
  });

  it("省略 remember 时沿用当前模式", () => {
    useAuthStore.getState().login(user, "at", "rt", false);

    const { writes, restore } = captureCookieWrites();
    useAuthStore.getState().login(user, "at2", "rt", undefined);
    restore();

    expect(writes[0]).not.toContain("expires=");
  });

  it("退出登录后 rememberMe 复位为 true", () => {
    useAuthStore.getState().login(user, "at", "rt", false);
    expect(useAuthStore.getState().rememberMe).toBe(false);

    useAuthStore.getState().logout();
    expect(useAuthStore.getState().rememberMe).toBe(true);
  });
});
