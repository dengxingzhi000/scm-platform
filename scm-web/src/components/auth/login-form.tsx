"use client";

import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Form, FormField, FormItem } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { authService } from "@/lib/api/services/auth";
import { toApiError } from "@/lib/api/envelope";
import type { LoginResponse } from "@/lib/api/contracts/auth";

import { buildLoginSchema, type LoginFormValues } from "./login-schema";

interface LoginFormProps {
  onSuccess: (response: LoginResponse) => void;
  onMfaRequired: (values: LoginFormValues) => void;
}

export function LoginForm({ onSuccess, onMfaRequired }: LoginFormProps) {
  const t = useTranslations("login");
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
      onSuccess(response);
    } catch (error: unknown) {
      const apiError = toApiError(error);
      if (apiError.code === 40104 || /two-factor|verification code|MFA/i.test(apiError.message)) {
        onMfaRequired(values);
        return;
      }
      throw error;
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
          <Link href="/zh-CN/forgot-password" className="text-sm text-[var(--color-brand-accent)] underline-offset-4 hover:underline">
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

        <p className="text-center text-[11px] leading-relaxed text-[var(--auth-ink-soft)]">
          {t("legalPrefix")}{" "}
          <Link href="/terms" className="underline underline-offset-4">
            {t("terms")}
          </Link>{" "}
          {t("legalMiddle")}{" "}
          <Link href="/privacy" className="underline underline-offset-4">
            {t("privacy")}
          </Link>
          {t("legalSuffix")}
        </p>
      </form>
    </Form>
  );
}
