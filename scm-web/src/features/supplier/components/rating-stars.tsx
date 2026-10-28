"use client";

import { Star } from "lucide-react";

import { cn } from "@/lib/utils";

interface RatingStarsProps {
  value: number;
  max?: number;
  showValue?: boolean;
  className?: string;
}

/**
 * Compact 5-star readout for supplier ratings. Uses the neutral/warning
 * palette instead of Ant Design's default gold so it sits with the tables.
 */
export default function RatingStars({
  value,
  max = 5,
  showValue = false,
  className,
}: RatingStarsProps) {
  const filled = Math.round(Math.min(Math.max(value, 0), max));

  return (
    <span
      className={cn("inline-flex items-center gap-1.5", className)}
      aria-label={`评级 ${value} / ${max}`}
    >
      <span className="inline-flex items-center gap-0.5" aria-hidden>
        {Array.from({ length: max }, (_, index) => (
          <Star
            key={index}
            strokeWidth={1.5}
            className={cn(
              "size-3",
              index < filled
                ? "fill-[var(--color-warning)] text-[var(--color-warning)]"
                : "fill-transparent text-[var(--color-border)]",
            )}
          />
        ))}
      </span>
      {showValue && (
        <span className="text-muted-foreground text-[11px] tabular-nums">
          {value.toFixed(1)}
        </span>
      )}
    </span>
  );
}
