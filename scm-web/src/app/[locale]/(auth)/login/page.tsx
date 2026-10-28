"use client";

import { useState } from "react";
import { App } from "antd";
import { useTranslations } from "next-intl";

import { LoginCard } from "@/components/auth/login-card";
import { LoginForm } from "@/components/auth/login-form";
import { TotpForm } from "@/components/auth/totp-form";
import type { LoginFormValues } from "@/components/auth/login-schema";
import { useCompleteLogin } from "@/lib/auth/use-complete-login";

export default function LoginPage() {
  const t = useTranslations("login");
  const { message } = App.useApp();
  const completeLogin = useCompleteLogin();
  const [pendingCredentials, setPendingCredentials] = useState<LoginFormValues | null>(null);

  if (pendingCredentials) {
    return (
      <LoginCard title={t("mfa.title")} subtitle={t("mfa.subtitle")}>
        <TotpForm
          pendingCredentials={pendingCredentials}
          onBack={() => setPendingCredentials(null)}
          onSuccess={(response) => {
            message.success(t("success"));
            void completeLogin(response);
          }}
        />
      </LoginCard>
    );
  }

  return (
    <LoginCard title={t("title")} subtitle={t("subtitle")}>
      <LoginForm
        onSuccess={(response) => {
          message.success(t("success"));
          void completeLogin(response);
        }}
        onMfaRequired={setPendingCredentials}
        onError={(error) => message.error(error.message || t("failed"))}
      />
    </LoginCard>
  );
}
