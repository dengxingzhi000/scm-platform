"use client";

import { useEffect, useMemo, useState } from "react";

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
import {
  useAssignSupplierCategories,
  useSupplierAssignedCategories,
  useSupplierCategories,
} from "../hooks";
import { errorMessage } from "../lib/api-error";

interface SupplierCategoryPanelProps {
  supplierId: string;
}

/** 缺失值的统一占位 —— 与概览分区一致。 */
const MISSING = "-";

/** 行内错误条。 */
const ERROR_STRIP =
  "border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm";

/** 两个 id 集合是否一致（顺序无关）。 */
const sameIds = (a: string[], b: string[]): boolean =>
  a.length === b.length && a.every((id) => b.includes(id));

export default function SupplierCategoryPanel({
  supplierId,
}: SupplierCategoryPanelProps) {
  // 已关联的分类（GET /suppliers/{id}/categories）与分类字典是两回事。
  const { data: assigned } = useSupplierAssignedCategories(supplierId);
  const { data: dictionary } = useSupplierCategories();
  const assign = useAssignSupplierCategories();

  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);

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
   * 已关联的分类是服务端的事实：弹窗打开、或服务端返回的集合变化时
   * （保存后刷新、后台重新拉取），把勾选状态对齐到服务端返回的集合。
   * 集合内容没变时保持 state 不动 —— 否则每次渲染都会白写一遍。
   */
  useEffect(() => {
    if (!open) return;
    setSelected((prev) => (sameIds(prev, assignedIds) ? prev : assignedIds));
  }, [open, assignedIds]);

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
   * 提交的是完整的 id 集合，清空所有勾选等于解除全部关联。
   */
  const handleSave = async () => {
    setError(null);
    try {
      await assign.mutateAsync({ id: supplierId, categoryIds: selected });
      setOpen(false);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

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
            onClick={() => {
              setError(null);
              setOpen(true);
            }}
          >
            设置分类
          </Button>
        </CardAction>
      </CardHeader>

      <CardContent>
        {assignedCategories.length === 0 ? (
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

      <Dialog open={open} onOpenChange={setOpen}>
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
            <Button variant="outline" onClick={() => setOpen(false)}>
              取消
            </Button>
            <Button disabled={assign.isPending} onClick={handleSave}>
              {assign.isPending ? "保存中…" : "保存"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  );
}
