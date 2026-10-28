import type * as React from "react";

import { cn } from "@/lib/utils";

interface PageHeaderProps {
  title: string;
  description?: string;
  /** Small uppercase label above the title, e.g. the module name. */
  eyebrow?: string;
  actions?: React.ReactNode;
  className?: string;
}

/**
 * Page title block for data screens. Titles are sans and tight like the
 * leaderboard pages; the description sits on one muted 13px line.
 */
export function PageHeader({
  title,
  description,
  eyebrow,
  actions,
  className,
}: PageHeaderProps) {
  return (
    <div
      className={cn(
        "flex flex-wrap items-end justify-between gap-x-6 gap-y-3",
        className,
      )}
    >
      <div className="min-w-0">
        {eyebrow && (
          <p className="text-muted-foreground mb-1 text-[10px] font-medium tracking-[0.14em] uppercase">
            {eyebrow}
          </p>
        )}
        <h1 className="text-[22px] leading-tight font-semibold tracking-[-0.02em] text-foreground">
          {title}
        </h1>
        {description && (
          <p className="text-muted-foreground mt-1 text-[13px] leading-5">
            {description}
          </p>
        )}
      </div>
      {actions && (
        <div className="flex shrink-0 flex-wrap items-center gap-2">
          {actions}
        </div>
      )}
    </div>
  );
}
