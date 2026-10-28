"use client";

import { useRouter } from "next/navigation";
import { useLocale } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";

import { useAuthStore } from "@/stores/useAuthStore";
import { toSessionUser, mergeUserInfo } from "@/lib/auth/session";
import { authService } from "@/lib/api/services/auth";
import type { LoginResponse } from "@/lib/api/contracts/auth";

/**
 * 建立会话的共享逻辑。密码登录与社交登录回调页共用。
 * 顺序不可调换:先写 store(后续 getUserInfo 才带得上 token),再清 query cache,最后跳路由。
 */
export function useCompleteLogin() {
  const router = useRouter();
  const locale = useLocale();
  const queryClient = useQueryClient();
  const { login, setUser, setPermissions } = useAuthStore();

  return async (response: LoginResponse): Promise<void> => {
    if (!response.accessToken) {
      throw new Error(response.message || "Login response has no access token");
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
  };
}
