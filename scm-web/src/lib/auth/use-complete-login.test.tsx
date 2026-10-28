import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { LoginResponse } from "@/lib/api/contracts/auth";

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

    // login 现在总是被调用四个参数 —— 少写一个就不匹配。
    expect(login).toHaveBeenCalledWith(
      expect.objectContaining({ id: "u", username: "admin" }),
      "at",
      "rt",
      undefined
    );
    expect(setPermissions).toHaveBeenCalledWith(["system:user:list"]);
    expect(clear).toHaveBeenCalled();
    await waitFor(() => expect(push).toHaveBeenCalledWith("/zh-CN/dashboard"));
  });

  it("passes the remember choice down to the store", async () => {
    const { result } = renderHook(() => useCompleteLogin());

    await act(async () => {
      await result.current(
        {
          accessToken: "at",
          refreshToken: "rt",
          userId: "u",
          username: "admin",
        },
        { remember: false }
      );
    });

    expect(login).toHaveBeenCalledWith(expect.anything(), "at", "rt", false);
  });

  it("returns a failure result instead of throwing when the response has no token", async () => {
    const { result } = renderHook(() => useCompleteLogin());

    let outcome: Awaited<ReturnType<typeof result.current>> | undefined;
    await act(async () => {
      // 后端以 HTTP 200 + 错误信封拒绝 —— 没有 accessToken
      outcome = await result.current({ message: "账号已锁定" } as LoginResponse);
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
      outcome = await result.current({} as LoginResponse);
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
