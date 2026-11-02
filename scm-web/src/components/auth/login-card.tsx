import type { ReactNode } from "react";

interface LoginCardProps {
  title: string;
  subtitle: string;
  children: ReactNode;
}

/** 登录/找回密码共用的标题区:居中衬线标题 + 说明,表单直接落在页面上。 */
export function LoginCard({ title, subtitle, children }: LoginCardProps) {
  return (
    <div>
      <div className="flex flex-col items-center gap-2 text-center">
        <h1 className="auth-serif-heading text-3xl text-[var(--auth-ink)]">{title}</h1>
        <p className="text-balance text-sm text-[var(--auth-ink-muted)]">{subtitle}</p>
      </div>

      <div className="mt-8">{children}</div>
    </div>
  );
}
