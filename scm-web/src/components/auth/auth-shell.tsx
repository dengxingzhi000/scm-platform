import type { ReactNode } from "react";
import { useTranslations } from "next-intl";

import { BrandMark } from "./brand-mark";

interface AuthShellProps {
  children: ReactNode;
}

/**
 * 认证页双栏外壳。
 * 左栏是暖象牙底的表单区,右栏是满高圆角占位图。
 * 根元素上覆写 shadcn 的 --primary/--ring/--accent,把 Claude 橙限制在登录体验内,
 * 全局 --color-primary(靛蓝)不变。
 */
export function AuthShell({ children }: AuthShellProps) {
  const t = useTranslations("login");

  return (
    <main
      className="min-h-dvh bg-[var(--auth-bg)] lg:grid lg:grid-cols-[1fr_minmax(0,42%)]"
      style={
        {
          "--primary": "var(--color-brand-accent)",
          "--ring": "var(--color-brand-accent)",
          "--accent": "var(--color-brand-accent-bg)",
        } as React.CSSProperties
      }
    >
      <section className="flex flex-col px-6 py-6 lg:px-12">
        <header className="flex items-center justify-between gap-4">
          <BrandMark />
          <div className="flex items-center gap-2">
            <button
              type="button"
              className="rounded-lg border border-[var(--auth-line-strong)] bg-[var(--auth-btn-soft-bg)] px-4 py-2 text-sm font-medium text-[var(--auth-ink)] transition-colors hover:bg-[var(--auth-btn-soft-hover)]"
            >
              {t("header.contactSales")}
            </button>
            <button
              type="button"
              className="rounded-lg bg-[var(--auth-btn)] px-4 py-2 text-sm font-medium text-[var(--auth-btn-fg)] transition-colors hover:bg-[var(--auth-ink-soft)]"
            >
              {t("header.tryPlatform")}
            </button>
          </div>
        </header>

        <div className="flex flex-1 items-center justify-center py-12">
          <div className="w-full max-w-[520px]">{children}</div>
        </div>
      </section>

      <aside className="hidden p-6 lg:block">
        <img
          src="/auth-hero.svg"
          alt={t("heroAlt")}
          className="h-full w-full rounded-2xl object-cover"
        />
      </aside>
    </main>
  );
}
