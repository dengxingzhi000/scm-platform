import type { ReactNode } from "react";

interface LoginCardProps {
  title: string;
  subtitle: string;
  children: ReactNode;
}

/** 登录/找回密码共用的白卡片:细描边 + 大圆角 + 极轻阴影。 */
export function LoginCard({ title, subtitle, children }: LoginCardProps) {
  return (
    <div>
      <h1 className="font-serif text-4xl leading-tight text-[var(--auth-ink)] lg:text-[56px]">
        {title}
      </h1>
      <p className="mt-3 font-serif text-lg text-[var(--auth-ink-soft)]">{subtitle}</p>

      <div className="mt-8 rounded-2xl border border-[var(--auth-line)] bg-[var(--auth-surface)] p-6 shadow-sm lg:p-8">
        {children}
      </div>
    </div>
  );
}
