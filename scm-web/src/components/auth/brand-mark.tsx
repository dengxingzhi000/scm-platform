import { cn } from "@/lib/utils";

interface BrandMarkProps {
  className?: string;
}

/** 登录页左上角的品牌标记:橙色放射状标记 + 平台字标。 */
export function BrandMark({ className }: BrandMarkProps) {
  return (
    <div className={cn("flex items-center gap-2.5", className)}>
      <svg
        viewBox="0 0 24 24"
        aria-hidden="true"
        className="size-6 text-[var(--color-brand-accent)]"
      >
        <g stroke="currentColor" strokeWidth="1.6" strokeLinecap="round">
          <path d="M12 12V3" />
          <path d="M12 12 4.2 6.6" />
          <path d="M12 12l7.8-5.4" />
          <path d="M12 12v9" />
          <path d="M12 12 5.4 17.4" />
          <path d="M12 12l6.6 5.4" />
        </g>
      </svg>
      <span className="font-serif text-lg tracking-tight text-[var(--auth-ink)]">
        SCM 供应链管理平台
      </span>
    </div>
  );
}
