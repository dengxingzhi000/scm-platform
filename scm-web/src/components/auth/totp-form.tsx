"use client";

import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Form, FormField, FormItem } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { authService } from "@/lib/api/services/auth";
import { toApiError } from "@/lib/api/envelope";
import type { LoginResponse } from "@/lib/api/contracts/auth";

import { buildTotpSchema, type LoginFormValues, type TotpFormValues } from "./login-schema";

interface TotpFormProps {
  pendingCredentials: LoginFormValues;
  onBack: () => void;
  onSuccess: (response: LoginResponse) => void;
}

export function TotpForm({ pendingCredentials, onBack, onSuccess }: TotpFormProps) {
  const t = useTranslations("login");
  const form = useForm<TotpFormValues>({
    resolver: zodResolver(buildTotpSchema(t)),
    defaultValues: { code: "" },
  });

  const loginMutation = useMutation({
    mutationFn: (data: { username: string; password: string; twoFactorCode: string }) =>
      authService.login(data),
  });

  const onSubmit = async (values: TotpFormValues) => {
    try {
      const response = await loginMutation.mutateAsync({
        username: pendingCredentials.username,
        password: pendingCredentials.password,
        twoFactorCode: values.code,
      });
      onSuccess(response);
    } catch (error: unknown) {
      // 验证码错误意味着「就差这一步」:留在本步,把后端文案就地显示,
      // 而不是像密码错误那样交给页面弹全局提示。
      form.setError("code", { message: toApiError(error).message || t("errors.codeInvalid") });
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
        <FormField
          control={form.control}
          name="code"
          render={({ field }) => (
            <FormItem>
              <Label htmlFor="code">{t("mfa.title")}</Label>
              <Input
                id="code"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                placeholder={t("mfa.codePlaceholder")}
                className="h-12 rounded-lg bg-transparent text-center text-lg tracking-[0.5em]"
                {...field}
              />
              {form.formState.errors.code && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.code.message}
                </p>
              )}
            </FormItem>
          )}
        />

        <Button
          type="submit"
          disabled={loginMutation.isPending}
          className="h-12 w-full rounded-lg bg-[var(--auth-btn)] text-[var(--auth-btn-fg)] hover:bg-[var(--auth-ink-soft)]"
        >
          {t("mfa.submit")}
        </Button>

        <Button
          type="button"
          variant="ghost"
          onClick={onBack}
          className="text-[var(--auth-ink-soft)]"
        >
          {t("mfa.back")}
        </Button>
      </form>
    </Form>
  );
}
