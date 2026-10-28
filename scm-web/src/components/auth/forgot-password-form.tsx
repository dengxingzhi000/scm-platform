"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";

import { Form, FormField, FormItem } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

/** next-intl 的 t:key 进、文案出。三个 schema 工厂共用。 */
type Translator = (key: string) => string;

const buildEmailSchema = (t: Translator) =>
  z.object({
    email: z.string().min(1, t("errors.emailRequired")).email(t("errors.emailInvalid")),
  });

const buildCodeSchema = (t: Translator) =>
  z.object({
    code: z.string().min(1, t("errors.codeRequired")).length(6, t("errors.codeLength")),
  });

const buildPasswordSchema = (t: Translator) =>
  z
    .object({
      password: z.string().min(1, t("errors.passwordRequired")).min(8, t("errors.passwordMin")),
      confirmPassword: z.string().min(1, t("errors.confirmRequired")),
    })
    .refine((values) => values.password === values.confirmPassword, {
      message: t("errors.passwordMismatch"),
      path: ["confirmPassword"],
    });

type EmailValues = z.infer<ReturnType<typeof buildEmailSchema>>;
type CodeValues = z.infer<ReturnType<typeof buildCodeSchema>>;
type PasswordValues = z.infer<ReturnType<typeof buildPasswordSchema>>;

type Step = "email" | "code" | "password";

interface ForgotPasswordFormProps {
  /** 三步都通过(模拟重置成功)。提示与跳转由页面负责 —— 与 LoginForm 的分工一致。 */
  onDone: () => void;
}

/** 三个步骤共用的主按钮样式,免得同一串令牌重复四遍。 */
const primaryButtonClass =
  "h-12 w-full rounded-lg bg-[var(--auth-btn)] text-[var(--auth-btn-fg)] hover:bg-[var(--auth-ink-soft)]";

export function ForgotPasswordForm({ onDone }: ForgotPasswordFormProps) {
  const t = useTranslations("forgot");
  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");

  // 每一步一个表单:三步的校验目标互不相干,共用一个表单只会让 defaultValues 越滚越大。
  const emailForm = useForm<EmailValues>({
    resolver: zodResolver(buildEmailSchema(t)),
    defaultValues: { email: "" },
  });
  const codeForm = useForm<CodeValues>({
    resolver: zodResolver(buildCodeSchema(t)),
    defaultValues: { code: "" },
  });
  const passwordForm = useForm<PasswordValues>({
    resolver: zodResolver(buildPasswordSchema(t)),
    defaultValues: { password: "", confirmPassword: "" },
  });

  const onEmailSubmit = (values: EmailValues) => {
    setEmail(values.email);
    setStep("code");
  };

  return (
    <div className="flex flex-col gap-4">
      {step === "email" && (
        <Form {...emailForm}>
          <form
            onSubmit={emailForm.handleSubmit(onEmailSubmit)}
            className="flex flex-col gap-4"
            noValidate
          >
            <FormField
              control={emailForm.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <Label htmlFor="email">{t("email")}</Label>
                  <Input
                    id="email"
                    type="email"
                    autoComplete="email"
                    placeholder={t("emailPlaceholder")}
                    className="h-12 rounded-lg bg-transparent"
                    {...field}
                  />
                  {emailForm.formState.errors.email && (
                    <p className="text-sm text-destructive">
                      {emailForm.formState.errors.email.message}
                    </p>
                  )}
                </FormItem>
              )}
            />
            <Button type="submit" className={primaryButtonClass}>
              {t("sendCode")}
            </Button>
          </form>
        </Form>
      )}

      {step === "code" && (
        <Form {...codeForm}>
          <form
            onSubmit={codeForm.handleSubmit(() => setStep("password"))}
            className="flex flex-col gap-4"
            noValidate
          >
            <p className="text-sm text-[var(--auth-ink-soft)]">{t("codeSent", { email })}</p>
            <FormField
              control={codeForm.control}
              name="code"
              render={({ field }) => (
                <FormItem>
                  <Label htmlFor="code">{t("code")}</Label>
                  <Input
                    id="code"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    maxLength={6}
                    placeholder={t("codePlaceholder")}
                    className="h-12 rounded-lg bg-transparent text-center text-lg tracking-[0.5em]"
                    {...field}
                  />
                  {codeForm.formState.errors.code && (
                    <p className="text-sm text-destructive">
                      {codeForm.formState.errors.code.message}
                    </p>
                  )}
                </FormItem>
              )}
            />
            <Button type="submit" className={primaryButtonClass}>
              {t("verify")}
            </Button>
          </form>
        </Form>
      )}

      {step === "password" && (
        <Form {...passwordForm}>
          <form
            onSubmit={passwordForm.handleSubmit(() => onDone())}
            className="flex flex-col gap-4"
            noValidate
          >
            <FormField
              control={passwordForm.control}
              name="password"
              render={({ field }) => (
                <FormItem>
                  <Label htmlFor="password">{t("newPassword")}</Label>
                  <Input
                    id="password"
                    type="password"
                    autoComplete="new-password"
                    placeholder={t("newPasswordPlaceholder")}
                    className="h-12 rounded-lg bg-transparent"
                    {...field}
                  />
                  {passwordForm.formState.errors.password && (
                    <p className="text-sm text-destructive">
                      {passwordForm.formState.errors.password.message}
                    </p>
                  )}
                </FormItem>
              )}
            />
            <FormField
              control={passwordForm.control}
              name="confirmPassword"
              render={({ field }) => (
                <FormItem>
                  <Label htmlFor="confirmPassword">{t("confirmPassword")}</Label>
                  <Input
                    id="confirmPassword"
                    type="password"
                    autoComplete="new-password"
                    placeholder={t("confirmPasswordPlaceholder")}
                    className="h-12 rounded-lg bg-transparent"
                    {...field}
                  />
                  {passwordForm.formState.errors.confirmPassword && (
                    <p className="text-sm text-destructive">
                      {passwordForm.formState.errors.confirmPassword.message}
                    </p>
                  )}
                </FormItem>
              )}
            />
            <Button type="submit" className={primaryButtonClass}>
              {t("reset")}
            </Button>
          </form>
        </Form>
      )}
    </div>
  );
}
