import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { useAuthStore } from "./useAuthStore";

const user = { id: "1", username: "admin", displayName: "Admin" };

/**
 * jsdom 的 document.cookie 读回来只剩 name=value,expires 之类的属性看不见 ——
 * 要证明「7 天持久 cookie」还是「会话 cookie」只能看写进去的原始字符串。
 * 这里包一层 setter:既记录写入,又照常写进真实的 cookie jar。
 */
let cookieWrites: string[] = [];
let cookieSpy: { mockRestore: () => void } | null = null;

function captureCookieWrites(): string[] {
  cookieSpy?.mockRestore();
  cookieWrites = [];
  const original = Object.getOwnPropertyDescriptor(Document.prototype, "cookie")!.set!;
  cookieSpy = vi
    .spyOn(document, "cookie", "set")
    .mockImplementation((value: string) => {
      cookieWrites.push(value);
      original.call(document, value);
    });
  return cookieWrites;
}

// spy 统一在 afterEach 复原:断言在 capture 与 restore 之间抛错时,
// 手动 restore 根本执行不到,spy 会漏到后面的用例里。
afterEach(() => {
  cookieSpy?.mockRestore();
  cookieSpy = null;
});

describe("useAuthStore 的 cookie 持久化", () => {
  beforeEach(() => {
    document.cookie = "access_token=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
    document.cookie = "access_token_persist=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
    useAuthStore.setState({ rememberMe: true });
  });

  it("记住我时写 access_token cookie", () => {
    useAuthStore.getState().login(user, "at", "rt", true);
    expect(document.cookie).toContain("access_token=at");
  });

  it("记住我时写的是 7 天过期的持久 cookie", () => {
    const writes = captureCookieWrites();

    useAuthStore.getState().login(user, "at", "rt", true);

    // login() 会写两个 cookie:token 与记住模式的标记 —— 这里只看 token 那个。
    const token = writes.find((w) => w.includes("access_token=at"));
    expect(token).toBeTruthy();
    const expires = /expires=([^;]+)/.exec(token!)?.[1];
    expect(expires).toBeTruthy();
    const days = (new Date(expires!).getTime() - Date.now()) / 864e5;
    expect(days).toBeGreaterThan(6.99);
    expect(days).toBeLessThan(7.01);
  });

  it("不记住我时写的是会话 cookie(没有 expires)", () => {
    const writes = captureCookieWrites();
    useAuthStore.getState().login(user, "at", "rt", false);

    const token = writes.find((w) => w.includes("access_token=at"));
    expect(token).toBeTruthy();
    expect(token).not.toContain("expires=");
    // cookie 必须真的在 —— 否则刷新页面时 proxy.ts 会把人弹回登录页
    expect(document.cookie).toContain("access_token=at");
  });

  it("不记住我时用会话 cookie 覆盖掉之前的持久 cookie", () => {
    useAuthStore.getState().login(user, "at", "rt", true);
    expect(document.cookie).toContain("access_token=at");

    const writes = captureCookieWrites();
    useAuthStore.getState().login(user, "at2", "rt", false);

    const token = writes.find((w) => w.includes("access_token=at2"));
    expect(token).toBeTruthy();
    expect(token).not.toContain("expires=");
    expect(document.cookie).toContain("access_token=at2");
  });

  it("省略 remember 时沿用当前模式", () => {
    useAuthStore.getState().login(user, "at", "rt", false);

    const writes = captureCookieWrites();
    useAuthStore.getState().login(user, "at2", "rt", undefined);

    const token = writes.find((w) => w.includes("access_token=at2"));
    expect(token).toBeTruthy();
    expect(token).not.toContain("expires=");
  });

  it("退出登录后 rememberMe 复位为 true", () => {
    useAuthStore.getState().login(user, "at", "rt", false);
    expect(useAuthStore.getState().rememberMe).toBe(false);

    useAuthStore.getState().logout();
    expect(useAuthStore.getState().rememberMe).toBe(true);
  });

  it("刷新后 store 状态复位也不会把会话升级成持久 cookie", () => {
    // 用户取消「记住我」登录 → 只有会话 cookie,没有标记 cookie
    useAuthStore.getState().login(user, "at", "rt", false);
    expect(document.cookie).not.toContain("access_token_persist");

    // 模拟一次页面刷新:store 回到初始值(rememberMe 又是 true),cookie 还在
    useAuthStore.setState({ rememberMe: true });

    // 401 刷新路径会不带 remember 参数地重新 login()
    const writes = captureCookieWrites();
    useAuthStore.getState().login(user, "at2", "rt", undefined);

    const token = writes.find((w) => w.includes("access_token=at2"));
    expect(token).toBeTruthy();
    expect(token).not.toContain("expires=");
  });

  it("刷新后记住我的会话仍然是持久 cookie", () => {
    useAuthStore.getState().login(user, "at", "rt", true);
    useAuthStore.setState({ rememberMe: true });

    const writes = captureCookieWrites();
    useAuthStore.getState().login(user, "at2", "rt", undefined);

    const token = writes.find((w) => w.includes("access_token=at2"));
    expect(token).toBeTruthy();
    expect(token).toContain("expires=");
  });

  it("兜底以标记 cookie 为准,而不是 store 里的 rememberMe", () => {
    // 记住我登录 → 标记 cookie 在
    useAuthStore.getState().login(user, "at", "rt", true);
    expect(document.cookie).toContain("access_token_persist");

    // store 里的内存状态说「不记住我」,但标记 cookie 才是真相来源
    useAuthStore.setState({ rememberMe: false });

    const writes = captureCookieWrites();
    useAuthStore.getState().login(user, "at2", "rt", undefined);

    const token = writes.find((w) => w.includes("access_token=at2"));
    expect(token).toBeTruthy();
    expect(token).toContain("expires=");
  });

  it("记住我时写标记 cookie,过期时间与 token 一致", () => {
    const writes = captureCookieWrites();

    useAuthStore.getState().login(user, "at", "rt", true);

    const marker = writes.find((w) => w.includes("access_token_persist"));
    expect(marker).toBeTruthy();
    const token = writes.find((w) => w.includes("access_token=at"));
    expect(token).toBeTruthy();
    // 两边的过期时间必须落在同一刻 —— 标记 cookie 比 token 活得久的话,
    // token 过期之后标记还在,下一次登录会平白继承「记住我」。
    // toUTCString 只精确到秒,所以比时间戳而不是比字符串。
    const markerExpiry = new Date(marker!.match(/expires=([^;]+)/)![1]).getTime();
    const tokenExpiry = new Date(token!.match(/expires=([^;]+)/)![1]).getTime();
    // toUTCString() 只精确到秒,两次 Date.now() 相隔不到 1 ms 时会跨秒 →
    // 差值是 1000 ms 的整数倍,所以容差必须含 1000,否则跨秒那一次会误报。
    expect(Math.abs(markerExpiry - tokenExpiry)).toBeLessThanOrEqual(1000);
    const days = (markerExpiry - Date.now()) / 864e5;
    expect(days).toBeGreaterThan(6.99);
    expect(days).toBeLessThan(7.01);
  });

  it("不记住我时清掉标记 cookie", () => {
    useAuthStore.getState().login(user, "at", "rt", true);
    expect(document.cookie).toContain("access_token_persist");

    useAuthStore.getState().login(user, "at2", "rt", false);
    expect(document.cookie).not.toContain("access_token_persist");
  });

  it("退出登录时清掉标记 cookie", () => {
    useAuthStore.getState().login(user, "at", "rt", true);
    expect(document.cookie).toContain("access_token_persist");

    useAuthStore.getState().logout();
    expect(document.cookie).not.toContain("access_token_persist");
  });

  it("刷新后从标记 cookie 恢复记住模式", async () => {
    useAuthStore.getState().login(user, "at", "rt", true);
    expect(document.cookie).toContain("access_token_persist");

    // store 里的内存状态不可信(刷新后回到默认的 true),标记 cookie 才是真相来源。
    useAuthStore.setState({ rememberMe: false });

    await useAuthStore.getState().initialize();
    expect(useAuthStore.getState().rememberMe).toBe(true);
  });

  it("没有标记 cookie 时刷新后不记住我", async () => {
    useAuthStore.getState().login(user, "at", "rt", false);
    expect(document.cookie).not.toContain("access_token_persist");

    // 模拟页面刷新:store 回到初始值(rememberMe 默认 true)
    useAuthStore.setState({ rememberMe: true });

    await useAuthStore.getState().initialize();
    expect(useAuthStore.getState().rememberMe).toBe(false);
  });

  // 401 刷新路径走的是 setTokens(刷新后 user 还是 null,不能要求有用户档案),
  // 它必须沿用当前的记住模式,否则「不记住我」的会话会在刷新时被悄悄升级。
  it("401 刷新路径(setTokens)沿用当前的记住模式", () => {
    useAuthStore.getState().login(user, "at", "rt", false);

    const writes = captureCookieWrites();
    useAuthStore.getState().setTokens("at2", "rt2");

    const token = writes.find((w) => w.includes("access_token=at2"));
    expect(token).toBeTruthy();
    expect(token).not.toContain("expires=");
  });

  it("401 刷新路径(setTokens)让持久会话继续保持持久", () => {
    useAuthStore.getState().login(user, "at", "rt", true);

    const writes = captureCookieWrites();
    useAuthStore.getState().setTokens("at2", "rt2");

    const token = writes.find((w) => w.includes("access_token=at2"));
    expect(token).toBeTruthy();
    expect(token).toContain("expires=");
  });

  it("setTokens 让标记 cookie 与 token 一起滑动", () => {
    useAuthStore.getState().login(user, "at", "rt", true);

    const writes = captureCookieWrites();
    useAuthStore.getState().setTokens("at2", "rt2");

    // 标记必须被重新写一次 —— 否则它会比 token 先到期,
    // 下一次刷新就会把持久会话当成不记住我。
    const marker = writes.find((w) => w.includes("access_token_persist"));
    expect(marker).toBeTruthy();
    expect(marker).toContain("expires=");
  });
});
