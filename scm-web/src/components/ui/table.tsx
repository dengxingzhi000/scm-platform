import * as React from "react";

import { cn } from "@/lib/utils";

/**
 * Data-table primitives modelled on artificialanalysis.ai's leaderboard:
 * a 36px sticky header band in brand-blue-light, 11px black column labels,
 * 12px tabular values, hairline column rules and a pinned first column.
 */
function Table({
  className,
  containerClassName,
  ...props
}: React.ComponentProps<"table"> & { containerClassName?: string }) {
  return (
    <div
      data-slot="table-container"
      className={cn("relative w-full overflow-x-auto", containerClassName)}
    >
      <table
        data-slot="table"
        className={cn(
          "w-full caption-bottom border-separate border-spacing-0 text-xs",
          className,
        )}
        {...props}
      />
    </div>
  );
}

function TableHeader({ className, ...props }: React.ComponentProps<"thead">) {
  return (
    <thead
      data-slot="table-header"
      className={cn("[&_tr]:border-0", className)}
      {...props}
    />
  );
}

function TableBody({ className, ...props }: React.ComponentProps<"tbody">) {
  return (
    <tbody
      data-slot="table-body"
      className={cn("[&_tr:last-child>td]:border-b-0", className)}
      {...props}
    />
  );
}

function TableFooter({ className, ...props }: React.ComponentProps<"tfoot">) {
  return (
    <tfoot
      data-slot="table-footer"
      className={cn(
        "bg-sunken border-t font-medium [&>tr]:last:border-b-0",
        className,
      )}
      {...props}
    />
  );
}

function TableRow({ className, ...props }: React.ComponentProps<"tr">) {
  return (
    <tr
      data-slot="table-row"
      className={cn(
        "group transition-colors hover:bg-[var(--color-bg-hover)] data-[state=selected]:bg-[var(--color-brand-blue-light)]",
        className,
      )}
      {...props}
    />
  );
}

function TableHead({
  className,
  numeric = false,
  ...props
}: React.ComponentProps<"th"> & { numeric?: boolean }) {
  return (
    <th
      data-slot="table-head"
      data-numeric={numeric || undefined}
      className={cn(
        "sticky top-0 z-20 h-9 border-r border-b border-[var(--color-border)] bg-[var(--color-brand-blue-light)] px-3 text-left align-middle text-[11px] font-medium tracking-[0.02em] whitespace-nowrap text-foreground last:border-r-0 first:sticky first:left-0 first:z-30",
        "[&:has([role=checkbox])]:pr-0 [&>[role=checkbox]]:translate-y-[2px]",
        numeric && "text-center tabular-nums",
        className,
      )}
      {...props}
    />
  );
}

function TableCell({
  className,
  numeric = false,
  ...props
}: React.ComponentProps<"td"> & { numeric?: boolean }) {
  return (
    <td
      data-slot="table-cell"
      data-numeric={numeric || undefined}
      className={cn(
        "border-r border-b border-[var(--color-border)] px-2.5 py-1.5 align-middle text-xs whitespace-nowrap tabular-nums last:border-r-0",
        "first:sticky first:left-0 first:z-10 first:bg-card group-hover:first:bg-[var(--color-bg-hover)]",
        "[&:has([role=checkbox])]:pr-0 [&>[role=checkbox]]:translate-y-[2px]",
        numeric && "text-center",
        className,
      )}
      {...props}
    />
  );
}

function TableCaption({
  className,
  ...props
}: React.ComponentProps<"caption">) {
  return (
    <caption
      data-slot="table-caption"
      className={cn("text-muted-foreground mt-4 text-xs", className)}
      {...props}
    />
  );
}

export {
  Table,
  TableHeader,
  TableBody,
  TableFooter,
  TableHead,
  TableRow,
  TableCell,
  TableCaption,
};
