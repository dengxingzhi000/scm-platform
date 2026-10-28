"use client";

import { useTranslations } from "next-intl";

import { LoginCard } from "@/components/auth/login-card";
import { ForgotPasswordForm } from "@/components/auth/forgot-password-form";

export default function ForgotPasswordPage() {
  const t = useTranslations("forgot");
  return (
    <LoginCard title={t("title")} subtitle={t("subtitle")}>
      <ForgotPasswordForm />
    </LoginCard>
  );
}
