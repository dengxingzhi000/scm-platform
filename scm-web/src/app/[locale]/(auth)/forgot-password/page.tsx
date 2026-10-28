"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useLocale, useTranslations } from "next-intl";
import { App } from "antd";

import { LoginCard } from "@/components/auth/login-card";
import { ForgotPasswordForm } from "@/components/auth/forgot-password-form";

export default function ForgotPasswordPage() {
  const t = useTranslations("forgot");
  const locale = useLocale();
  const router = useRouter();
  const { message } = App.useApp();

  // 找回密码没有后端接口,三步走完即视为重置成功 ——
  // 与原页面一致:弹一句成功提示,然后回登录页。不能什么都不做(那是个死胡同)。
  const handleDone = () => {
    message.success(t("resetSuccess"));
    router.push(`/${locale}/login`);
  };

  return (
    <LoginCard title={t("title")} subtitle={t("subtitle")}>
      <ForgotPasswordForm onDone={handleDone} />
      {/* 旧页面卡片底部有「返回登录」入口。丢了它,用户从登录页点进来就再也回不去了。 */}
      <p className="mt-6 text-center text-sm">
        <Link
          href={`/${locale}/login`}
          className="text-[var(--auth-ink-soft)] underline-offset-4 hover:text-[var(--auth-ink)] hover:underline"
        >
          {t("back")}
        </Link>
      </p>
    </LoginCard>
  );
}
