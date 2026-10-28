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
});
