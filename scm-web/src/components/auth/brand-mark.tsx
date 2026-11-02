import { useTranslations } from "next-intl";

import { cn } from "@/lib/utils";

interface BrandMarkProps {
  className?: string;
}

/**
 * 登录页左上的品牌标记:紫罗兰 S 磁贴 + 衬线字标。
 * 磁贴与侧边栏 logo 同一符号,避免登录页出现第二套品牌语言。
 */
export function BrandMark({ className }: BrandMarkProps) {
  const t = useTranslations("login");

  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <BrandGlyph />
      <span className="font-serif text-lg font-medium tracking-tight text-[var(--auth-ink)]">
        {t("brandName")}
      </span>
    </span>
  );
}

interface BrandGlyphProps {
  /** inverse 供右侧玻璃卡使用:白底紫字,与页头的紫底白字互为反白。 */
  tone?: "brand" | "inverse";
  className?: string;
}

export function BrandGlyph({ tone = "brand", className }: BrandGlyphProps) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        "grid size-6 shrink-0 place-items-center rounded-md font-serif text-[17px] leading-none",
        tone === "brand"
          ? "bg-[var(--auth-btn)] text-white"
          : "bg-white text-[var(--auth-btn)]",
        className
      )}
    >
      S
    </span>
  );
}
