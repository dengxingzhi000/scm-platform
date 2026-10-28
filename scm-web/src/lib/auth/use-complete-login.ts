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

/** 建立会话的选项。`remember` 省略时沿用 store 里当前的模式。 */
export interface CompleteLoginOptions {
  remember?: boolean;
}

/**
 * 建立会话的共享逻辑。密码登录与社交登录回调页共用。
 * 顺序不可调换:先写 store(后续 getUserInfo 才带得上 token),再清 query cache,最后跳路由。
 */
export function useCompleteLogin() {
  const router = useRouter();
  const locale = useLocale();
  const queryClient = useQueryClient();
  const { login, setUser, setPermissions } = useAuthStore();

  return async (
    response: LoginResponse,
    options?: CompleteLoginOptions
  ): Promise<LoginCompletion> => {
    // 后端可能以 HTTP 200 返回带 message 的错误信封 —— 此时没有 token,不能进 store。
    // 旧页面在这里 `message.error(...)` 然后停在原地;现在把文案交给调用方去弹。
    if (!response.accessToken) {
      return { ok: false, message: response.message ?? "" };
    }

    login(
      toSessionUser(response),
      response.accessToken,
      response.refreshToken ?? "",
      options?.remember
    );
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
