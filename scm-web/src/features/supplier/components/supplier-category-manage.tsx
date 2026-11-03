"use client";

import { useMemo, useState } from "react";
import { Pencil, Plus } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { PageHeader } from "@/components/ui/page-header";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  TableEmpty,
  TableLoading,
  TablePanel,
  TableToolbar,
  TableToolbarEnd,
} from "@/components/ui/table-shell";
import { cn } from "@/lib/utils";
import {
  useCreateSupplierCategory,
  useSupplierCategories,
  useUpdateSupplierCategory,
} from "../hooks";
import { errorMessage } from "../lib/api-error";
import type { SupplierCategory } from "../types";
import SupplierField from "./supplier-field";

/**
 * 树的节点。`SupplierCategory` 本身没有 `children` —— 这里补上，
 * 只在展示层使用，不参与后端请求体。
 */
export interface SupplierCategoryNode extends SupplierCategory {
  children?: SupplierCategoryNode[];
}

/** 缺失值的统一占位 —— 与其他分区一致。 */
const MISSING = "-";

/** 行内错误条。 */
const ERROR_STRIP =
  "border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm";

/** 下拉里「无上级」的哨兵值 —— Radix Select 不接受空串。 */
const NO_PARENT = "__none__";

/** 每层的缩进像素 —— 子分类相对父分类右移。 */
const INDENT = 20;

/** 同一层里的排序：先 sortOrder，再名称兜底。 */
function compareCategories(a: SupplierCategory, b: SupplierCategory) {
  return (
    (a.sortOrder ?? 0) - (b.sortOrder ?? 0) ||
    (a.categoryName ?? "").localeCompare(b.categoryName ?? "")
  );
}

/**
 * 把扁平分类列表按 `parentId` 组成树。父分类不存在的孤儿当根节点，
 * 避免字典里的一条脏数据让整个子树消失。
 */
export function buildCategoryTree(
  categories: SupplierCategory[],
): SupplierCategoryNode[] {
  const nodes = new Map<string, SupplierCategoryNode>();
  for (const category of categories) {
    nodes.set(category.id, { ...category });
  }

  const roots: SupplierCategoryNode[] = [];
  for (const category of categories) {
    const node = nodes.get(category.id);
    if (!node) continue;
    const parent =
      category.parentId && category.parentId !== category.id
        ? nodes.get(category.parentId)
        : undefined;
    if (parent) {
      const children = parent.children ?? (parent.children = []);
      children.push(node);
    } else {
      roots.push(node);
    }
  }

  const sortLevel = (level: SupplierCategoryNode[]) => {
    level.sort(compareCategories);
    for (const node of level) {
      if (node.children?.length) sortLevel(node.children);
    }
  };
  sortLevel(roots);
  return roots;
}

/** 树的先序展开 —— 表格按顺序渲染每一行，同时带上缩进深度。 */
interface FlatCategory {
  category: SupplierCategoryNode;
  depth: number;
}

function flattenTree(
  nodes: SupplierCategoryNode[],
  depth = 0,
): FlatCategory[] {
  return nodes.flatMap((category) => [
    { category, depth },
    ...flattenTree(category.children ?? [], depth + 1),
  ]);
}

/** 节点自身 + 全部后代的 id —— 编辑时用来排除会造成环的上级选项。 */
function collectIds(node: SupplierCategoryNode): string[] {
  return [
    node.id,
    ...(node.children ?? []).flatMap((child) => collectIds(child)),
  ];
}

function findNode(
  nodes: SupplierCategoryNode[],
  id: string,
): SupplierCategoryNode | undefined {
  for (const node of nodes) {
    if (node.id === id) return node;
    const hit = findNode(node.children ?? [], id);
    if (hit) return hit;
  }
  return undefined;
}

interface CategoryFormDialogProps {
  open: boolean;
  /** null 表示新增。 */
  category: SupplierCategory | null;
  /** 从「＋子分类」进入时预选的上级分类。 */
  presetParentId: string | null;
  /** 完整分类列表 —— 上级下拉的选项来源。 */
  categories: SupplierCategory[];
  onOpenChange: (open: boolean) => void;
}

/**
 * 新增 / 编辑弹窗。分类编码与名称必填（后端 `validateCategory` 要求非空），
 * 层级由服务端 `resolveLevel` 按上级分类重算，所以表单里没有「层级」字段。
 */
function CategoryFormDialog({
  open,
  category,
  presetParentId,
  categories,
  onOpenChange,
}: CategoryFormDialogProps) {
  const create = useCreateSupplierCategory();
  const update = useUpdateSupplierCategory();

  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState(() => ({
    categoryCode: category?.categoryCode ?? "",
    categoryName: category?.categoryName ?? "",
    parentId: category?.parentId ?? presetParentId ?? NO_PARENT,
    sortOrder: String(category?.sortOrder ?? 0),
    enabled: category?.enabled ?? true,
  }));

  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  /**
   * 上级候选：编辑时排除自身与全部后代 —— 让一个分类成为自己的祖先，
   * 表单就无法再恢复成合法结构。
   */
  const parentOptions = useMemo(() => {
    const tree = buildCategoryTree(categories);
    const excluded = new Set<string>();
    if (category) {
      const node = findNode(tree, category.id);
      if (node) {
        for (const id of collectIds(node)) excluded.add(id);
      }
    }
    return flattenTree(tree).filter(
      ({ category: node }) => !excluded.has(node.id),
    );
  }, [categories, category]);

  const pending = create.isPending || update.isPending;
  const canSubmit =
    form.categoryCode.trim().length > 0 &&
    form.categoryName.trim().length > 0;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setError(null);
    const data = {
      categoryCode: form.categoryCode.trim(),
      categoryName: form.categoryName.trim(),
      parentId: form.parentId === NO_PARENT ? null : form.parentId,
      sortOrder: Number(form.sortOrder) || 0,
      enabled: form.enabled,
    };
    try {
      if (category) {
        // 更新必须带上已有的 level —— 服务端只在上级变化时重算。
        await update.mutateAsync({
          id: category.id,
          data: { ...data, id: category.id, level: category.level ?? null },
        });
      } else {
        await create.mutateAsync({ data });
      }
      onOpenChange(false);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{category ? "编辑分类" : "新增分类"}</DialogTitle>
          <DialogDescription>
            分类编码与名称必填；层级由服务端按上级分类自动计算。
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-3 sm:grid-cols-2">
          <SupplierField label="分类编码" required htmlFor="category-code">
            <Input
              id="category-code"
              value={form.categoryCode}
              onChange={(event) => set("categoryCode", event.target.value)}
              placeholder="例如：RAW_ELEC"
              className="font-mono"
            />
          </SupplierField>
          <SupplierField label="分类名称" required htmlFor="category-name">
            <Input
              id="category-name"
              value={form.categoryName}
              onChange={(event) => set("categoryName", event.target.value)}
              placeholder="例如：电子元器件"
            />
          </SupplierField>
          <SupplierField
            label="上级分类"
            htmlFor="category-parent"
            className="sm:col-span-2"
          >
            <Select
              value={form.parentId}
              onValueChange={(value) => set("parentId", value)}
            >
              <SelectTrigger id="category-parent" className="w-full">
                <SelectValue placeholder="无（作为一级分类）" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NO_PARENT}>无（作为一级分类）</SelectItem>
                {parentOptions.map(({ category: node, depth }) => (
                  <SelectItem key={node.id} value={node.id}>
                    {"　".repeat(depth)}
                    {node.categoryName ?? node.categoryCode ?? node.id}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </SupplierField>
          <SupplierField label="排序" htmlFor="category-sort">
            <Input
              id="category-sort"
              type="number"
              min={0}
              value={form.sortOrder}
              onChange={(event) => set("sortOrder", event.target.value)}
              className="tabular-nums"
            />
          </SupplierField>
          <SupplierField label="启用" htmlFor="category-enabled">
            <div className="flex h-9 items-center gap-2">
              <Checkbox
                id="category-enabled"
                aria-label="启用该分类"
                checked={form.enabled}
                onCheckedChange={(checked) => set("enabled", checked === true)}
              />
              <span className="text-muted-foreground text-[12px]">
                启用后可被供应商关联
              </span>
            </div>
          </SupplierField>
        </div>

        {error && <div className={ERROR_STRIP}>{error}</div>}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button disabled={!canSubmit || pending} onClick={handleSubmit}>
            {pending ? "保存中…" : "保存"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/** 当前弹窗状态：编辑某个分类，或新增（可预选上级）。 */
interface DialogState {
  category: SupplierCategory | null;
  parentId: string | null;
}

export default function SupplierCategoryManage() {
  const { data, isLoading, isError, error: queryError, refetch } =
    useSupplierCategories();
  const update = useUpdateSupplierCategory();

  const [actionError, setActionError] = useState<string | null>(null);
  const [dialog, setDialog] = useState<DialogState | null>(null);

  const categories = useMemo(() => data ?? [], [data]);
  const rows = useMemo(
    () => flattenTree(buildCategoryTree(categories)),
    [categories],
  );

  /**
   * 启停开关。后端 `validateCategory` 要求编码与名称非空，所以整体回传，
   * 不能只发 `{ enabled }`。
   */
  const handleToggle = async (category: SupplierCategory, next: boolean) => {
    setActionError(null);
    try {
      await update.mutateAsync({
        id: category.id,
        data: {
          id: category.id,
          categoryCode: category.categoryCode,
          categoryName: category.categoryName,
          parentId: category.parentId ?? null,
          level: category.level ?? null,
          sortOrder: category.sortOrder ?? 0,
          enabled: next,
        },
      });
    } catch (e) {
      setActionError(errorMessage(e));
    }
  };

  const openCreate = () => {
    setActionError(null);
    setDialog({ category: null, parentId: null });
  };

  const openEdit = (category: SupplierCategory) => {
    setActionError(null);
    setDialog({ category, parentId: null });
  };

  const openCreateChild = (parent: SupplierCategory) => {
    setActionError(null);
    setDialog({ category: null, parentId: parent.id });
  };

  const pending = update.isPending;

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="供应商管理"
        title="分类字典"
        description="平台统一的供应商分类树；层级由上级分类自动推导，停用的分类不可再被关联"
        actions={
          <Button onClick={openCreate}>
            <Plus />
            新增分类
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <span className="text-muted-foreground text-[11px]">
            共 {categories.length} 个分类
          </span>

          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              分类编码与名称由平台统一维护
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {actionError && (
          <div className={cn(ERROR_STRIP, "m-3")}>{actionError}</div>
        )}

        {isLoading ? (
          <TableLoading />
        ) : isError ? (
          <div className="flex flex-wrap items-center gap-3 p-4">
            <div className={ERROR_STRIP}>
              {errorMessage(queryError) || "分类字典加载失败"}
            </div>
            <Button variant="outline" size="sm" onClick={() => void refetch()}>
              重试
            </Button>
          </div>
        ) : rows.length === 0 ? (
          <TableEmpty>暂无分类</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-260px)] overflow-y-auto">
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[240px]">分类名称</TableHead>
                <TableHead>分类编码</TableHead>
                <TableHead numeric>层级</TableHead>
                <TableHead numeric>排序</TableHead>
                <TableHead>启用</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map(({ category, depth }) => {
                const name = category.categoryName ?? category.id;
                const level = category.level ?? depth + 1;
                return (
                  <TableRow key={category.id}>
                    <TableCell className="min-w-[240px]">
                      <div
                        className="flex items-center gap-2"
                        style={{ paddingLeft: depth * INDENT }}
                      >
                        <Badge
                          variant="secondary"
                          className="h-5 min-w-7 rounded px-1 text-[10px] tabular-nums"
                        >
                          L{level}
                        </Badge>
                        <span className="text-foreground text-[12px] font-semibold">
                          {category.categoryName ?? MISSING}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="text-muted-foreground font-mono text-[11px]">
                      {category.categoryCode ?? MISSING}
                    </TableCell>
                    <TableCell numeric>{level}</TableCell>
                    <TableCell numeric>{category.sortOrder ?? 0}</TableCell>
                    <TableCell>
                      <Checkbox
                        aria-label={name}
                        checked={category.enabled === true}
                        disabled={pending}
                        onCheckedChange={(checked) =>
                          void handleToggle(category, checked === true)
                        }
                      />
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`编辑${name}`}
                          disabled={pending}
                          onClick={() => openEdit(category)}
                        >
                          <Pencil />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`${name}新增子分类`}
                          disabled={pending}
                          onClick={() => openCreateChild(category)}
                        >
                          <Plus />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </TablePanel>

      {dialog && (
        <CategoryFormDialog
          key={
            dialog.category
              ? `edit-${dialog.category.id}`
              : `new-${dialog.parentId ?? "root"}`
          }
          open
          category={dialog.category}
          presetParentId={dialog.parentId}
          categories={categories}
          onOpenChange={(open) => {
            if (!open) setDialog(null);
          }}
        />
      )}
    </div>
  );
}
