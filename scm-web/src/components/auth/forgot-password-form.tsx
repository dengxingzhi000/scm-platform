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

/** 「上一步」按钮:次要样式,不能跟主 CTA 一样重。 */
const backButtonClass =
  "h-11 w-full rounded-lg border-[var(--auth-line-strong)] bg-transparent text-[var(--auth-ink)] hover:bg-[var(--auth-btn-soft-hover)] hover:text-[var(--auth-ink)]";

export function ForgotPasswordForm({ onDone }: ForgotPasswordFormProps) {
  const t = useTranslations("forgot");
  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");

  // 步骤指示:旧页面的 antd <Steps> 有这三个标题,迁到 shadcn 后不能丢 ——
  // 用户得知道自己站在第几步,也才想得到还能往回走。
  const steps = [
    { key: "email", label: t("steps.email") },
    { key: "code", label: t("steps.code") },
    { key: "password", label: t("steps.password") },
  ] as const;

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

  /**
   * 回到邮箱步:把用户敲过的地址还回去,改一版就能重发验证码,不用刷新页面。
   * 输入框的值由 email state 驱动,这里同步把表单的值重置成同一个邮箱 ——
   * 免得输入框与表单内部的值各说各话。
   */
  const backToEmailStep = () => {
    emailForm.reset({ email });
    setStep("email");
  };

  return (
    <div className="flex flex-col gap-4">
      <ol className="flex items-center gap-4">
        {steps.map((item) => {
          const isCurrent = item.key === step;
          return (
            <li
              key={item.key}
              className="flex items-center gap-1.5"
              aria-current={isCurrent ? "step" : undefined}
            >
              <span
                aria-hidden="true"
                className={
                  isCurrent
                    ? "h-1.5 w-1.5 rounded-full bg-[var(--color-brand-accent)]"
                    : "h-1.5 w-1.5 rounded-full bg-[var(--auth-ink-soft)]"
                }
              />
              <span
                className={
                  isCurrent
                    ? "text-xs font-medium text-[var(--auth-ink)]"
                    : "text-xs text-[var(--auth-ink-soft)]"
                }
              >
                {item.label}
              </span>
            </li>
          );
        })}
      </ol>

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
                    // 邮箱是跨步骤存活的:输入时就同步到 state,回到这一步时内容还在。
                    value={email}
                    onChange={(event) => {
                      setEmail(event.target.value);
                      field.onChange(event);
                    }}
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
            <Button type="button" variant="outline" className={backButtonClass} onClick={backToEmailStep}>
              {t("backStep")}
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
            <Button type="button" variant="outline" className={backButtonClass} onClick={() => setStep("code")}>
              {t("backStep")}
            </Button>
          </form>
        </Form>
      )}
    </div>
  );
}
