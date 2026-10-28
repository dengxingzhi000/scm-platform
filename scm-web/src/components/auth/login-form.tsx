"use client";

import { useMutation } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Form, FormField, FormItem } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { authService } from "@/lib/api/services/auth";
import { toApiError, type ApiError } from "@/lib/api/envelope";
import type { LoginResponse } from "@/lib/api/contracts/auth";

import { buildLoginSchema, type LoginFormValues } from "./login-schema";

interface LoginFormProps {
  onSuccess: (response: LoginResponse, remember: boolean) => void;
  onMfaRequired: (values: LoginFormValues) => void;
  onError: (error: ApiError) => void;
}

const isMfaRequired = (message: string, code: number): boolean =>
  code === 40104 || /two-factor|verification code|MFA/i.test(message);

export function LoginForm({ onSuccess, onMfaRequired, onError }: LoginFormProps) {
  const t = useTranslations("login");
  const locale = useLocale();
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(buildLoginSchema(t)),
    defaultValues: { username: "", password: "", remember: true },
  });

  const loginMutation = useMutation({
    mutationFn: (data: { username: string; password: string }) => authService.login(data),
  });

  const onSubmit = async (values: LoginFormValues) => {
    try {
      const response = await loginMutation.mutateAsync({
        username: values.username,
        password: values.password,
      });
      onSuccess(response, values.remember ?? true);
    } catch (error: unknown) {
      const apiError = toApiError(error);
      if (isMfaRequired(apiError.message, apiError.code)) {
        onMfaRequired(values);
        return;
      }
      // 不能 throw:本函数是 form.handleSubmit 的回调,没有人 catch 这个 Promise,
      // 抛出去就是「点登录没反应」+ 一条 unhandled rejection。交给调用方提示(旧页面是 message.error)。
      onError(apiError);
    }
  };

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(onSubmit)}
        className="flex flex-col gap-4"
        noValidate
      >
        <FormField
          control={form.control}
          name="username"
          render={({ field }) => (
            <FormItem>
              <Label htmlFor="username">{t("username")}</Label>
              <Input
                id="username"
                autoComplete="username"
                placeholder={t("usernamePlaceholder")}
                className="h-12 rounded-lg bg-transparent"
                {...field}
              />
              {form.formState.errors.username && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.username.message}
                </p>
              )}
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <Label htmlFor="password">{t("password")}</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                placeholder={t("passwordPlaceholder")}
                className="h-12 rounded-lg bg-transparent"
                {...field}
              />
              {form.formState.errors.password && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.password.message}
                </p>
              )}
            </FormItem>
          )}
        />

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <FormField
              control={form.control}
              name="remember"
              render={({ field }) => (
                <Checkbox
                  id="remember"
                  checked={field.value}
                  onCheckedChange={field.onChange}
                  onBlur={field.onBlur}
                />
              )}
            />
            <Label htmlFor="remember" className="text-sm font-normal text-[var(--auth-ink-soft)]">
              {t("remember")}
            </Label>
          </div>
          <Link href={`/${locale}/forgot-password`} className="text-sm text-[var(--color-brand-accent)] underline-offset-4 hover:underline">
            {t("forgotPassword")}
          </Link>
        </div>

        <Button
          type="submit"
          disabled={loginMutation.isPending}
          className="h-12 w-full rounded-lg bg-[var(--auth-btn)] text-[var(--auth-btn-fg)] hover:bg-[var(--auth-ink-soft)]"
        >
          {t("submit")}
        </Button>

        {/*
          「服务条款 / 隐私政策」目前没有对应路由(仓库里不存在 /terms 与 /privacy),
          之前渲染成 <Link> 就是死链 —— 点下去会被 proxy.ts 弹回登录页,看起来像链接坏了。
          路由上线后(记得补上 locale 前缀)再换回 Link。
        */}
        <p className="text-center text-[11px] leading-relaxed text-[var(--auth-ink-soft)]">
          {t("legalPrefix")} {t("terms")} {t("legalMiddle")} {t("privacy")}{t("legalSuffix")}
        </p>
      </form>
    </Form>
  );
}
