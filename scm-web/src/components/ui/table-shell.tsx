import * as React from "react";
import {
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

/**
 * The leaderboard-style data panel: one bordered surface holding the filter
 * toolbar, the table and a compact footer. Keeping all three in one panel
 * removes the stacked cards the list screens used to show.
 */
function TablePanel({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="table-panel"
      className={cn(
        "overflow-hidden rounded-lg border border-[var(--color-border-secondary)] bg-card",
        className,
      )}
      {...props}
    />
  );
}

function TableToolbar({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="table-toolbar"
      className={cn(
        "flex flex-wrap items-center gap-2 border-b border-[var(--color-border-secondary)] px-3 py-2",
        className,
      )}
      {...props}
    />
  );
}

/** Pushes the controls after it to the right edge of the toolbar. */
function TableToolbarEnd({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="table-toolbar-end"
      className={cn("ml-auto flex flex-wrap items-center gap-2", className)}
      {...props}
    />
  );
}

function TableEmpty({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="table-empty"
      className={cn(
        "text-muted-foreground py-16 text-center text-[13px]",
        className,
      )}
      {...props}
    />
  );
}

function TableLoading({ className }: { className?: string }) {
  return (
    <div className={cn("p-4", className)}>
      <Skeleton className="h-56 w-full rounded-lg" />
    </div>
  );
}

interface TablePaginationProps {
  page: number;
  pageSize: number;
  total: number;
  pageCount: number;
  onPageChange: (page: number) => void;
  className?: string;
}

function TablePagination({
  page,
  pageSize,
  total,
  pageCount,
  onPageChange,
  className,
}: TablePaginationProps) {
  const start = total === 0 ? 0 : (page - 1) * pageSize + 1;
  const end = Math.min(page * pageSize, total);
  const disabledPrev = page <= 1;
  const disabledNext = page >= pageCount;

  return (
    <div
      data-slot="table-pagination"
      className={cn(
        "text-muted-foreground flex flex-wrap items-center justify-between gap-3 border-t border-[var(--color-border-secondary)] px-3 py-2 text-[11px]",
        className,
      )}
    >
      <span className="tabular-nums">
        显示 {start}–{end} 条，共 {total} 条
      </span>
      <div className="flex items-center gap-0.5">
        <span className="mr-2 tabular-nums">
          第 {page} / {pageCount} 页
        </span>
        <Button
          variant="ghost"
          size="icon"
          className="size-7"
          aria-label="第一页"
          disabled={disabledPrev}
          onClick={() => onPageChange(1)}
        >
          <ChevronsLeft />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="size-7"
          aria-label="上一页"
          disabled={disabledPrev}
          onClick={() => onPageChange(page - 1)}
        >
          <ChevronLeft />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="size-7"
          aria-label="下一页"
          disabled={disabledNext}
          onClick={() => onPageChange(page + 1)}
        >
          <ChevronRight />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="size-7"
          aria-label="最后一页"
          disabled={disabledNext}
          onClick={() => onPageChange(pageCount)}
        >
          <ChevronsRight />
        </Button>
      </div>
    </div>
  );
}

export {
  TablePanel,
  TableToolbar,
  TableToolbarEnd,
  TableEmpty,
  TableLoading,
  TablePagination,
};
