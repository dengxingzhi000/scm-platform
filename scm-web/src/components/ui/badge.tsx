import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center justify-center rounded-full border px-2.5 py-0.5 text-xs font-medium w-fit whitespace-nowrap shrink-0 [&>svg]:size-3 gap-1 [&>svg]:pointer-events-none focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px] aria-invalid:ring-destructive/20 aria-invalid:border-destructive transition-[color,background-color] overflow-hidden",
  {
    variants: {
      // Same-hue tint with a darker text of that hue, so a chip never has to
      // carry white-on-colour contrast.
      variant: {
        default:
          "border-[var(--color-primary)]/25 bg-[var(--color-primary-soft)] text-[var(--color-primary-text)]",
        secondary:
          "border-border bg-sunken text-muted-foreground",
        destructive:
          "border-[var(--color-error)]/25 bg-[var(--color-error-soft)] text-[var(--color-error-text)]",
        outline:
          "border-border text-foreground [a&]:hover:bg-sunken [a&]:hover:text-foreground",
        success:
          "border-[var(--color-success)]/25 bg-[var(--color-success-soft)] text-[var(--color-success-text)]",
        warning:
          "border-[var(--color-warning)]/25 bg-[var(--color-warning-soft)] text-[var(--color-warning-text)]",
        info: "border-[var(--color-info)]/25 bg-[var(--color-info-soft)] text-[var(--color-info-text)]",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  },
);

function Badge({
  className,
  variant,
  asChild = false,
  ...props
}: React.ComponentProps<"span"> &
  VariantProps<typeof badgeVariants> & { asChild?: boolean }) {
  const Comp = asChild ? Slot : "span";

  return (
    <Comp
      data-slot="badge"
      className={cn(badgeVariants({ variant }), className)}
      {...props}
    />
  );
}

export { Badge, badgeVariants };
