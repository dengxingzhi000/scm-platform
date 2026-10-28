"use client";

import { useState } from "react";
import { App } from "antd";
import { useTranslations } from "next-intl";

import { LoginCard } from "@/components/auth/login-card";
import { LoginForm } from "@/components/auth/login-form";
import { TotpForm } from "@/components/auth/totp-form";
import type { LoginFormValues } from "@/components/auth/login-schema";
import { useCompleteLogin } from "@/lib/auth/use-complete-login";
import type { LoginResponse } from "@/lib/api/contracts/auth";

export default function LoginPage() {
  const t = useTranslations("login");
  const { message } = App.useApp();
  const completeLogin = useCompleteLogin();
  const [pendingCredentials, setPendingCredentials] = useState<LoginFormValues | null>(null);

  const handleSuccess = async (response: LoginResponse, remember: boolean) => {
    try {
      const result = await completeLogin(response, { remember });
      if (!result.ok) {
        message.error(result.message || t("failed"));
        return;
      }
      message.success(t("success"));
    } catch {
      // 兜底:建会话途中的意外异常也不能变成静默失败
      message.error(t("failed"));
    }
  };

  if (pendingCredentials) {
    return (
      <LoginCard title={t("mfa.title")} subtitle={t("mfa.subtitle")}>
        <TotpForm
          pendingCredentials={pendingCredentials}
          onBack={() => setPendingCredentials(null)}
          // 「记住我」的勾选状态要跨过 MFA 这一步 —— 从待验证的凭据里取
          onSuccess={(response) => void handleSuccess(response, pendingCredentials.remember ?? true)}
        />
      </LoginCard>
    );
  }

  return (
    <LoginCard title={t("title")} subtitle={t("subtitle")}>
      <LoginForm
        onSuccess={(response, remember) => void handleSuccess(response, remember)}
        onMfaRequired={setPendingCredentials}
        onError={(error) => message.error(error.message || t("failed"))}
      />
    </LoginCard>
  );
}
