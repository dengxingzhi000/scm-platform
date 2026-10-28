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

const buildForgotSchema = (t: (key: string) => string) =>
  z.object({
    email: z.string().min(1, t("errors.emailRequired")).email(t("errors.emailInvalid")),
  });

type ForgotValues = z.infer<ReturnType<typeof buildForgotSchema>>;

type Step = "email" | "code" | "password";

export function ForgotPasswordForm() {
  const t = useTranslations("forgot");
  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");

  const form = useForm<ForgotValues>({
    resolver: zodResolver(buildForgotSchema(t)),
    defaultValues: { email: "" },
  });

  const onEmailSubmit = (values: ForgotValues) => {
    setEmail(values.email);
    setStep("code");
  };

  return (
    <div className="flex flex-col gap-4">
      {step === "email" && (
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onEmailSubmit)} className="flex flex-col gap-4" noValidate>
            <FormField
              control={form.control}
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
                  {form.formState.errors.email && (
                    <p className="text-sm text-destructive">
                      {form.formState.errors.email.message}
                    </p>
                  )}
                </FormItem>
              )}
            />
            <Button
              type="submit"
              className="h-12 w-full rounded-lg bg-[var(--auth-btn)] text-[var(--auth-btn-fg)] hover:bg-[var(--auth-ink-soft)]"
            >
              {t("sendCode")}
            </Button>
          </form>
        </Form>
      )}

      {step === "code" && (
        <div className="flex flex-col gap-4">
          <p className="text-sm text-[var(--auth-ink-soft)]">{t("codeSent", { email })}</p>
          <Input
            name="code"
            inputMode="numeric"
            maxLength={6}
            placeholder={t("codePlaceholder")}
            className="h-12 rounded-lg bg-transparent text-center text-lg tracking-[0.5em]"
          />
          <Button
            type="button"
            onClick={() => setStep("password")}
            className="h-12 w-full rounded-lg bg-[var(--auth-btn)] text-[var(--auth-btn-fg)] hover:bg-[var(--auth-ink-soft)]"
          >
            {t("verify")}
          </Button>
        </div>
      )}

      {step === "password" && (
        <div className="flex flex-col gap-4">
          <Input
            name="password"
            type="password"
            autoComplete="new-password"
            placeholder={t("newPasswordPlaceholder")}
            className="h-12 rounded-lg bg-transparent"
          />
          <Input
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            placeholder={t("confirmPasswordPlaceholder")}
            className="h-12 rounded-lg bg-transparent"
          />
          <Button
            type="button"
            className="h-12 w-full rounded-lg bg-[var(--auth-btn)] text-[var(--auth-btn-fg)] hover:bg-[var(--auth-ink-soft)]"
          >
            {t("reset")}
          </Button>
        </div>
      )}
    </div>
  );
}
