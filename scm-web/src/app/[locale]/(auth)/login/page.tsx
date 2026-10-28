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
        {null}
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
