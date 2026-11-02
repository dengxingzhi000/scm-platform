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

type Provider = "google" | "microsoft";

interface LoginFormProps {
  onSuccess: (response: LoginResponse, remember: boolean) => void;
  onMfaRequired: (values: LoginFormValues) => void;
  onError: (error: ApiError) => void;
  /** 第三方登录尚未接入:按钮保留参考图版式,点击后由页面提示未开通。 */
  onProviderUnavailable?: (provider: Provider) => void;
}

const isMfaRequired = (message: string, code: number): boolean =>
  code === 40104 || /two-factor|verification code|MFA/i.test(message);

/** 输入框共用的尺寸与描边:参考图的发丝线 + 紫罗兰聚焦环。 */
const inputClass =
  "h-9 rounded-lg border-[var(--auth-line)] bg-transparent px-3 text-sm text-[var(--auth-ink)] placeholder:text-[var(--auth-ink-soft)] hover:border-[var(--auth-line-strong)] focus-visible:border-[var(--auth-focus)] focus-visible:bg-white focus-visible:ring-[3px] focus-visible:ring-[var(--auth-focus-ring)]";

const providerButtonClass =
  "inline-flex h-8 w-full items-center justify-center gap-2 rounded-lg border border-[var(--auth-line)] bg-white text-sm font-medium text-[var(--auth-ink)] transition-colors hover:border-[var(--auth-ink-soft)] focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-[var(--auth-focus-ring)]";

export function LoginForm({
  onSuccess,
  onMfaRequired,
  onError,
  onProviderUnavailable,
}: LoginFormProps) {
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
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <div className="relative">
          <button
            type="button"
            className={providerButtonClass}
            onClick={() => onProviderUnavailable?.("google")}
          >
            <GoogleIcon />
            {t("providers.google")}
          </button>
          <span className="pointer-events-none absolute -top-2.5 right-4 rounded-full bg-[var(--auth-ink)] px-2 py-0.5 text-[10px] font-medium leading-4 text-white">
            {t("providers.lastUsed")}
          </span>
        </div>

        <button
          type="button"
          className={providerButtonClass}
          onClick={() => onProviderUnavailable?.("microsoft")}
        >
          <MicrosoftIcon />
          {t("providers.microsoft")}
        </button>
      </div>

      <div className="relative text-center">
        <span aria-hidden="true" className="absolute inset-x-0 top-1/2 h-px bg-[var(--auth-line)]" />
        <span className="relative bg-[var(--auth-bg)] px-4 text-xs text-[var(--auth-ink-muted)]">
          {t("divider")}
        </span>
      </div>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-3" noValidate>
          <FormField
            control={form.control}
            name="username"
            render={({ field }) => (
              <FormItem>
                <Label htmlFor="username" className="sr-only">
                  {t("username")}
                </Label>
                <Input
                  id="username"
                  autoComplete="username"
                  placeholder={t("usernamePlaceholder")}
                  className={inputClass}
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
                <Label htmlFor="password" className="sr-only">
                  {t("password")}
                </Label>
                <Input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  placeholder={t("passwordPlaceholder")}
                  className={inputClass}
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

          <div className="flex items-center justify-between gap-3 pt-1">
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
              <Label
                htmlFor="remember"
                className="text-xs font-normal text-[var(--auth-ink-soft)]"
              >
                {t("remember")}
              </Label>
            </div>
            <Link
              href={`/${locale}/forgot-password`}
              className="text-xs text-[var(--auth-btn-hover)] underline-offset-4 hover:underline"
            >
              {t("forgotPassword")}
            </Link>
          </div>

          <Button
            type="submit"
            disabled={loginMutation.isPending}
            className="h-8 w-full rounded-lg bg-[var(--auth-btn)] text-sm font-medium text-[var(--auth-btn-fg)] hover:bg-[var(--auth-btn-hover)] focus-visible:ring-[3px] focus-visible:ring-[var(--auth-focus-ring)]"
          >
            {t("submit")}
          </Button>
        </form>
      </Form>
    </div>
  );
}

function GoogleIcon() {
  return (
    <svg aria-hidden="true" width="16" height="16" viewBox="0 0 22 22" fill="none">
      <g clipPath="url(#google-logo-clip)">
        <path
          d="M21.56 11.25C21.56 10.47 21.49 9.72 21.36 9H11V13.26H16.92C16.66 14.63 15.88 15.79 14.71 16.57V19.34H18.28C20.36 17.42 21.56 14.6 21.56 11.25Z"
          fill="#4285F4"
        />
        <path
          d="M10.9997 21.9996C13.9697 21.9996 16.4597 21.0196 18.2797 19.3396L14.7097 16.5696C13.7297 17.2296 12.4797 17.6296 10.9997 17.6296C8.13969 17.6296 5.70969 15.6996 4.83969 13.0996H1.17969V15.9396C2.98969 19.5296 6.69969 21.9996 10.9997 21.9996Z"
          fill="#34A853"
        />
        <path
          d="M4.84 13.0903C4.62 12.4303 4.49 11.7303 4.49 11.0003C4.49 10.2703 4.62 9.57031 4.84 8.91031V6.07031H1.18C0.43 7.55031 0 9.22031 0 11.0003C0 12.7803 0.43 14.4503 1.18 15.9303L4.03 13.7103L4.84 13.0903Z"
          fill="#FBBC05"
        />
        <path
          d="M10.9997 4.38C12.6197 4.38 14.0597 4.94 15.2097 6.02L18.3597 2.87C16.4497 1.09 13.9697 0 10.9997 0C6.69969 0 2.98969 2.47 1.17969 6.07L4.83969 8.91C5.70969 6.31 8.13969 4.38 10.9997 4.38Z"
          fill="#EA4335"
        />
      </g>
      <defs>
        <clipPath id="google-logo-clip">
          <rect width="22" height="22" fill="white" />
        </clipPath>
      </defs>
    </svg>
  );
}

function MicrosoftIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 21 21" height="16" width="16">
      <path fill="#f35325" d="M0 0h10v10H0z" />
      <path fill="#81bc06" d="M11 0h10v10H11z" />
      <path fill="#05a6f0" d="M0 11h10v10H0z" />
      <path fill="#ffba08" d="M11 11h10v10H11z" />
    </svg>
  );
}
