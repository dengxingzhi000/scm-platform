import Image from "next/image";
import type { CSSProperties, ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Check } from "lucide-react";

import { BrandGlyph, BrandMark } from "./brand-mark";

interface AuthShellProps {
  children: ReactNode;
}

/**
 * 认证页双栏外壳:左侧纯白表单区,右侧满高流体封面 + 玻璃卡。
 * 根元素把 shadcn 变量锁到 --auth-* 上,登录体验固定浅色,
 * 不受应用深色主题影响(否则深色下的白底输入框会继承深色文字)。
 */
export function AuthShell({ children }: AuthShellProps) {
  const t = useTranslations("login");
  const features = [t("hero.feature1"), t("hero.feature2"), t("hero.feature3")];

  return (
    <main
      className="flex min-h-dvh bg-[var(--auth-bg)] text-[var(--auth-ink)]"
      style={
        {
          "--background": "var(--auth-bg)",
          "--foreground": "var(--auth-ink)",
          "--primary": "var(--auth-btn)",
          "--primary-foreground": "var(--auth-btn-fg)",
          "--ring": "var(--auth-focus)",
          "--input": "var(--auth-line-strong)",
          "--border": "var(--auth-line)",
          "--muted-foreground": "var(--auth-ink-muted)",
          "--accent": "var(--auth-bg-soft)",
          "--accent-foreground": "var(--auth-ink)",
        } as CSSProperties
      }
    >
      <section className="flex min-w-0 flex-1 flex-col gap-4 p-6 md:p-10">
        <header className="flex justify-center lg:justify-start">
          <BrandMark />
        </header>

        <div className="flex flex-1 items-center justify-center py-8">
          <div className="w-full max-w-[320px]">{children}</div>
        </div>

        {/*
          「服务条款 / 隐私政策」目前没有对应路由(仓库里不存在 /terms 与 /privacy),
          渲染成 <Link> 就是死链 —— 点下去会被 proxy.ts 弹回登录页。
          路由上线后(记得补 locale 前缀)再换成链接。
        */}
        <p className="mx-auto max-w-[420px] text-center text-xs leading-relaxed text-[var(--auth-ink-soft)]">
          {t("legal")}
        </p>
      </section>

      <aside className="relative hidden w-[clamp(400px,30vw,480px)] shrink-0 items-center justify-center overflow-hidden p-12 lg:flex xl:p-16">
        <Image
          src="/auth/cover.jpg"
          alt={t("heroAlt")}
          fill
          priority
          sizes="(min-width: 1024px) 480px, 1px"
          className="object-cover"
        />

        <div className="relative mt-10 w-full max-w-[320px] rounded-[2rem] bg-[var(--auth-glass)] p-8 text-white backdrop-blur-[4px]">
          <BrandGlyph tone="inverse" className="mb-2 size-8 rounded-lg text-[22px]" />
          <h2 className="auth-serif-heading text-xl text-white">{t("hero.title")}</h2>
          <ul className="mt-3 flex flex-col gap-1.5">
            {features.map((feature) => (
              <li key={feature} className="flex items-center gap-2 text-sm text-white/90">
                <Check aria-hidden="true" className="size-4 shrink-0 text-[var(--auth-check)]" />
                {feature}
              </li>
            ))}
          </ul>
        </div>
      </aside>
    </main>
  );
}
