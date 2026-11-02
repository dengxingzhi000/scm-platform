"use client";

import { useMemo, useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useAssignSupplierCategories,
  useSupplierAssignedCategories,
  useSupplierCategories,
} from "../hooks";
import { errorMessage } from "../lib/api-error";
import type { SupplierCategory } from "../types";

interface SupplierCategoryPanelProps {
  supplierId: string;
}

/** 缺失值的统一占位 —— 与概览分区一致。 */
const MISSING = "-";

/** 行内错误条。 */
const ERROR_STRIP =
  "border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm";

interface CategoryDialogProps {
  open: boolean;
  supplierId: string;
  /** 分类字典 —— 勾选列表。 */
  categories: SupplierCategory[];
  /** 已关联的分类 id —— 勾选初值。 */
  assignedIds: string[];
  onOpenChange: (open: boolean) => void;
}

/**
 * 勾选弹窗。只在打开时挂载：勾选状态在挂载那一刻用「已关联的分类」初始化，
 * 之后服务端集合再变也不会动用户正在做的选择。
 */
function CategoryDialog({
  open,
  supplierId,
  categories,
  assignedIds,
  onOpenChange,
}: CategoryDialogProps) {
  const assign = useAssignSupplierCategories();
  const [selected, setSelected] = useState<string[]>(assignedIds);
  const [error, setError] = useState<string | null>(null);

  const toggle = (id: string, checked: boolean) => {
    setSelected((prev) =>
      checked
        ? prev.includes(id)
          ? prev
          : [...prev, id]
        : prev.filter((item) => item !== id),
    );
  };

  /**
   * PUT /suppliers/{id}/categories 是整体替换，不是增量 ——
   * 提交的是完整的 id 集合，取消勾选就是解除关联。
   */
  const handleSave = async () => {
    setError(null);
    try {
      await assign.mutateAsync({ id: supplierId, categoryIds: selected });
      onOpenChange(false);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>设置分类</DialogTitle>
          <DialogDescription>
            勾选该供应商供应的分类；保存后整体替换当前的关联关系。
          </DialogDescription>
        </DialogHeader>

        <div className="max-h-[50vh] space-y-0.5 overflow-y-auto pr-1">
          {categories.length === 0 ? (
            <p className="text-muted-foreground text-xs">分类字典为空。</p>
          ) : (
            categories.map((category) => (
              <label
                key={category.id}
                className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 hover:bg-[var(--color-bg-hover)]"
              >
                <Checkbox
                  checked={selected.includes(category.id)}
                  onCheckedChange={(checked) =>
                    toggle(category.id, checked === true)
                  }
                  aria-label={category.categoryName ?? category.id}
                />
                <span className="text-[12px]">
                  {/* 子分类用全角空格缩进，与商品分类的下拉写法一致。 */}
                  {category.parentId ? "　" : ""}
                  {category.categoryName ?? MISSING}
                </span>
              </label>
            ))
          )}
        </div>

        {error && <div className={ERROR_STRIP}>{error}</div>}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button disabled={assign.isPending} onClick={handleSave}>
            {assign.isPending ? "保存中…" : "保存"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default function SupplierCategoryPanel({
  supplierId,
}: SupplierCategoryPanelProps) {
  // 已关联的分类（GET /suppliers/{id}/categories）与分类字典是两回事。
  const { data: assigned, isLoading: assignedLoading } =
    useSupplierAssignedCategories(supplierId);
  const { data: dictionary } = useSupplierCategories();

  const [open, setOpen] = useState(false);

  const assignedCategories = assigned ?? [];
  const assignedIds = useMemo(
    () => (assigned ?? []).map((category) => category.id),
    [assigned],
  );

  /**
   * 分类字典是一棵树：先按层级排，父分类才会排在子分类前面；
   * 同一层里按 sortOrder、再按名称排 —— 与列表筛选里的分类下拉一致。
   */
  const categories = useMemo(
    () =>
      [...(dictionary ?? [])].sort(
        (a, b) =>
          (a.level ?? 0) - (b.level ?? 0) ||
          (a.sortOrder ?? 0) - (b.sortOrder ?? 0) ||
          (a.categoryName ?? "").localeCompare(b.categoryName ?? ""),
      ),
    [dictionary],
  );

  /**
   * 已关联的集合还没到手时不挂弹窗：勾选初值在挂载那一刻就得是对的，
   * 否则「先开弹窗、后拿到数据」会把已有关联当成空的，保存等于清空。
   */
  const assignedReady = assigned !== undefined;

  return (
    <Card>
      <CardHeader>
        <CardTitle>供应商分类</CardTitle>
        <CardDescription>
          该供应商已关联的分类；分类字典由平台统一维护。
        </CardDescription>
        <CardAction>
          <Button
            variant="outline"
            disabled={!assignedReady}
            onClick={() => setOpen(true)}
          >
            设置分类
          </Button>
        </CardAction>
      </CardHeader>

      <CardContent>
        {assignedLoading ? (
          <Skeleton className="h-5 w-32" />
        ) : assignedCategories.length === 0 ? (
          <p className="text-muted-foreground text-xs">尚未关联分类。</p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {assignedCategories.map((category) => (
              <Badge key={category.id} variant="secondary">
                {category.categoryName ?? MISSING}
              </Badge>
            ))}
          </div>
        )}
      </CardContent>

      {open && assignedReady && (
        <CategoryDialog
          open={open}
          supplierId={supplierId}
          categories={categories}
          assignedIds={assignedIds}
          onOpenChange={setOpen}
        />
      )}
    </Card>
  );
}
