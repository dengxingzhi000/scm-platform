"use client";

import { useEffect, useMemo, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useForm } from "react-hook-form";
import { z } from "zod";
import {
  ArrowLeft,
  FolderPlus,
  Loader2,
  Pencil,
  Plus,
  Search,
  Trash2,
} from "lucide-react";

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
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
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
  TableEmpty,
  TableLoading,
  TablePanel,
  TablePagination,
  TableToolbar,
  TableToolbarEnd,
} from "@/components/ui/table-shell";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";
import type {
  ProdCategoryDTO,
  ProdCategoryPayload,
} from "@/lib/api/contracts/product";
import {
  ROOT_PARENT_ID,
  useCategoryChildren,
  useCategoryOptions,
  useCategoryPage,
  useCreateCategory,
  useDeleteCategory,
  useUpdateCategory,
} from "../hooks/use-category-crud";

const PAGE_SIZE = 20;

const NUMERIC_COLUMNS = new Set(["level", "sortOrder"]);

const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, ProdCategoryDTO>();
const emptyRows: ProdCategoryDTO[] = [];

interface DrillView {
  parentId: string;
  parentName: string;
}

type CategoryFormTarget =
  | { mode: "create"; parentId: string }
  | { mode: "edit"; category: ProdCategoryDTO };

/** Numeric form fields stay strings while editing, exactly like the product form. */
const categoryFormSchema = z
  .object({
    categoryCode: z
      .string()
      .trim()
      .min(1, "请输入分类编码")
      .max(50, "分类编码最多 50 个字符"),
    categoryName: z
      .string()
      .trim()
      .min(1, "请输入分类名称")
      .max(50, "分类名称最多 50 个字符"),
    parentId: z.string().min(1, "请选择父级分类"),
    level: z.string().optional(),
    sortOrder: z.string().optional(),
    iconUrl: z.string().optional(),
    imageUrl: z.string().optional(),
    description: z.string().max(500, "描述最多 500 个字符").optional(),
    isLeaf: z.boolean(),
    enabled: z.boolean(),
  })
  .refine((values) => isOptionalInteger(values.level), {
    message: "层级须为不小于 1 的整数",
    path: ["level"],
  })
  .refine((values) => isOptionalInteger(values.sortOrder, 0), {
    message: "排序须为非负整数",
    path: ["sortOrder"],
  });

type CategoryFormValues = z.infer<typeof categoryFormSchema>;

function isOptionalInteger(value: string | undefined, min = 1) {
  if (!value || !value.trim()) return true;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= min;
}

function optionalNumber(value: string | undefined, min = 1) {
  if (!value || !value.trim()) return undefined;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= min ? parsed : undefined;
}

const defaultValues: CategoryFormValues = {
  categoryCode: "",
  categoryName: "",
  parentId: ROOT_PARENT_ID,
  level: "1",
  sortOrder: "0",
  iconUrl: "",
  imageUrl: "",
  description: "",
  isLeaf: true,
  enabled: true,
};

function toFormValues(category: ProdCategoryDTO): CategoryFormValues {
  return {
    categoryCode: category.categoryCode ?? "",
    categoryName: category.categoryName ?? "",
    parentId: category.parentId || ROOT_PARENT_ID,
    level: category.level == null ? "" : String(category.level),
    sortOrder: category.sortOrder == null ? "" : String(category.sortOrder),
    iconUrl: category.iconUrl ?? "",
    imageUrl: category.imageUrl ?? "",
    description: category.description ?? "",
    isLeaf: category.isLeaf ?? true,
    enabled: category.enabled ?? true,
  };
}

function toPayload(values: CategoryFormValues): ProdCategoryPayload {
  return {
    categoryCode: values.categoryCode.trim(),
    categoryName: values.categoryName.trim(),
    parentId: values.parentId,
    level: optionalNumber(values.level),
    sortOrder: optionalNumber(values.sortOrder, 0),
    iconUrl: values.iconUrl?.trim() || undefined,
    imageUrl: values.imageUrl?.trim() || undefined,
    description: values.description?.trim() || undefined,
    isLeaf: values.isLeaf,
    enabled: values.enabled,
  };
}

export default function CategoryManage() {
  const [current, setCurrent] = useState(1);
  const [nameDraft, setNameDraft] = useState("");
  const [name, setName] = useState("");
  const [drill, setDrill] = useState<DrillView | null>(null);
  const [formTarget, setFormTarget] = useState<CategoryFormTarget>({
    mode: "create",
    parentId: ROOT_PARENT_ID,
  });
  const [formOpen, setFormOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ProdCategoryDTO | null>(null);

  const listQuery = {
    page: current,
    size: PAGE_SIZE,
    name: name || undefined,
  };
  const { data: page, isLoading: listLoading } = useCategoryPage(listQuery);
  const { data: childRows, isLoading: childrenLoading } = useCategoryChildren(
    drill?.parentId ?? null,
  );
  const deleteCategory = useDeleteCategory();

  const rows = drill ? (childRows ?? emptyRows) : (page?.records ?? emptyRows);
  const total = drill ? rows.length : (page?.total ?? 0);
  const isLoading = drill ? childrenLoading : listLoading;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));
  // The children endpoint is not paged, so pagination only applies to the full list.
  const isPaged = !drill;

  const openCreate = (parentId: string) => {
    setFormTarget({ mode: "create", parentId });
    setFormOpen(true);
  };

  const openEdit = (category: ProdCategoryDTO) => {
    setFormTarget({ mode: "edit", category });
    setFormOpen(true);
  };

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("categoryCode", {
          header: "分类编码",
          cell: (info) => (
            <span className="font-mono text-xs">{info.getValue() || "-"}</span>
          ),
        }),
        columnHelper.accessor("categoryName", {
          header: "分类名称",
          cell: (info) => (
            <button
              type="button"
              className="text-primary text-left hover:underline"
              onClick={() =>
                setDrill({
                  parentId: info.row.original.id,
                  parentName: info.getValue() ?? "",
                })
              }
            >
              {info.getValue() || "-"}
            </button>
          ),
        }),
        columnHelper.accessor("level", {
          header: "层级",
          cell: (info) =>
            info.getValue() == null ? (
              "-"
            ) : (
              <Badge variant="secondary">{info.getValue()} 级</Badge>
            ),
        }),
        columnHelper.accessor("sortOrder", {
          header: "排序",
          cell: (info) => info.getValue() ?? "-",
        }),
        columnHelper.accessor("isLeaf", {
          header: "节点",
          cell: (info) =>
            info.getValue() ? (
              <Badge variant="outline">叶子</Badge>
            ) : (
              <Badge variant="secondary">含子分类</Badge>
            ),
        }),
        columnHelper.accessor("enabled", {
          header: "状态",
          cell: (info) =>
            info.getValue() ? (
              <Badge variant="success">启用</Badge>
            ) : (
              <Badge variant="destructive">停用</Badge>
            ),
        }),
        columnHelper.display({
          id: "actions",
          header: "操作",
          cell: ({ row }) => (
            <div className="flex items-center justify-end gap-1">
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label="添加子分类"
                    onClick={() => openCreate(row.original.id)}
                  >
                    <FolderPlus />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>添加子分类</TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label="编辑"
                    onClick={() => openEdit(row.original)}
                  >
                    <Pencil />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>编辑</TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label="删除"
                    className="text-destructive hover:text-destructive"
                    onClick={() => setDeleteTarget(row.original)}
                  >
                    <Trash2 />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>删除</TooltipContent>
              </Tooltip>
            </div>
          ),
        }),
      ]),
    [],
  );

  const table = useTable({ features, columns, data: rows });

  const applyKeyword = (value: string) => {
    setName(value.trim());
    setCurrent(1);
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="商品中心"
        title="分类管理"
        description="按层级维护商品分类树，支持逐级下钻"
        actions={
          <Button onClick={() => openCreate(ROOT_PARENT_ID)}>
            <Plus />
            新建顶级分类
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          {drill ? (
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setDrill(null)}
              >
                <ArrowLeft />
                返回全部
              </Button>
              <span className="text-muted-foreground text-[11px]">
                {drill.parentName} 的子分类
              </span>
            </div>
          ) : (
            <span className="text-muted-foreground text-[11px]">
              点击分类名称可下钻查看子分类
            </span>
          )}

          <TableToolbarEnd>
            {isPaged && (
              <form
                onSubmit={(event) => {
                  event.preventDefault();
                  applyKeyword(nameDraft);
                }}
                className="flex items-center gap-2"
              >
                <Input
                  value={nameDraft}
                  onChange={(event) => setNameDraft(event.target.value)}
                  placeholder="搜索分类名称"
                  className="h-8 w-56 text-xs"
                />
                <Button
                  type="submit"
                  variant="outline"
                  size="icon"
                  className="size-8"
                  aria-label="搜索"
                >
                  <Search />
                </Button>
              </form>
            )}
            <span className="text-muted-foreground text-[11px]">
              {isPaged ? `共 ${total} 条` : `共 ${total} 个子分类`}
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : table.getRowModel().rows.length === 0 ? (
          <TableEmpty>
            {drill ? "该分类下暂无子分类" : "暂无分类"}
          </TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              {table.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      numeric={NUMERIC_COLUMNS.has(header.column.id)}
                      className={cn(
                        header.column.id === "isLeaf" && "text-center",
                        header.column.id === "enabled" && "text-center",
                        header.column.id === "actions" && "text-right",
                      )}
                    >
                      {header.isPlaceholder ? null : (
                        <table.FlexRender header={header} />
                      )}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {table.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      numeric={NUMERIC_COLUMNS.has(cell.column.id)}
                      className={cn(
                        cell.column.id === "isLeaf" && "text-center",
                        cell.column.id === "enabled" && "text-center",
                        cell.column.id === "actions" && "text-right",
                      )}
                    >
                      <table.FlexRender cell={cell} />
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        {isPaged && (
          <TablePagination
            page={current}
            pageSize={PAGE_SIZE}
            total={total}
            pageCount={pageCount}
            onPageChange={setCurrent}
          />
        )}
      </TablePanel>

      <CategoryFormDialog
        open={formOpen}
        target={formTarget}
        onOpenChange={setFormOpen}
      />

      <Dialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>删除分类</DialogTitle>
            <DialogDescription>
              确定删除「{deleteTarget?.categoryName}」？含子分类或已挂属性模板的分类会被后端拒绝。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={deleteCategory.isPending}
              onClick={() => {
                if (!deleteTarget) return;
                deleteCategory.mutate(deleteTarget.id, {
                  // The backend owns the "has children / has templates"
                  // rule — a rejected delete keeps the dialog open so the
                  // error toast stays with the action.
                  onSuccess: () => setDeleteTarget(null),
                });
              }}
            >
              删除
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

interface CategoryFormDialogProps {
  open: boolean;
  target: CategoryFormTarget;
  onOpenChange: (open: boolean) => void;
}

function CategoryFormDialog({
  open,
  target,
  onOpenChange,
}: CategoryFormDialogProps) {
  const { data: categories } = useCategoryOptions();
  const createCategory = useCreateCategory();
  const updateCategory = useUpdateCategory();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const isEdit = target.mode === "edit";
  const editingId = isEdit ? target.category.id : undefined;

  const form = useForm<CategoryFormValues>({
    resolver: zodResolver(categoryFormSchema),
    defaultValues,
    mode: "onBlur",
  });

  // Re-seed the form every time the dialog opens: edits load the row, creates
  // inherit the drilled parent (or the "0" root sentinel) and its next level.
  useEffect(() => {
    if (!open) return;
    if (target.mode === "edit") {
      form.reset(toFormValues(target.category));
      return;
    }
    const parentLevel =
      target.parentId === ROOT_PARENT_ID
        ? 0
        : (categories?.find((item) => item.id === target.parentId)?.level ?? 1);
    form.reset({
      ...defaultValues,
      parentId: target.parentId,
      level: String(parentLevel + 1),
    });
  }, [open, target, form, categories]);

  const parentOptions = useMemo(
    () =>
      (categories ?? [])
        .filter((category) => category.id !== editingId)
        .map((category) => ({
          label: `${category.categoryName ?? ""}（${category.categoryCode ?? "-"}）`,
          value: category.id,
        })),
    [categories, editingId],
  );

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitError(null);
    const payload = toPayload(values);
    try {
      if (target.mode === "edit") {
        await updateCategory.mutateAsync({ id: target.category.id, payload });
      } else {
        await createCategory.mutateAsync(payload);
      }
      onOpenChange(false);
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : "保存失败");
    }
  });

  const isPending = createCategory.isPending || updateCategory.isPending;

  // Clearing the API error on close keeps a stale failure from showing the next
  // time the dialog opens (an effect would only re-run after the render).
  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) setSubmitError(null);
    onOpenChange(nextOpen);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? "编辑分类" : "新建分类"}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? "修改分类的基础信息与展示顺序。"
              : "顶级分类的父级为「顶级分类」，子分类会自动带入父级。"}
          </DialogDescription>
        </DialogHeader>

        {submitError && (
          <p className="border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm">
            {submitError}
          </p>
        )}

        <Form {...form}>
          <form onSubmit={onSubmit} className="grid gap-4 md:grid-cols-2">
            <FormField
              control={form.control}
              name="categoryCode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>分类编码</FormLabel>
                  <FormControl>
                    <Input placeholder="CAT-001" disabled={isEdit} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="categoryName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>分类名称</FormLabel>
                  <FormControl>
                    <Input placeholder="例如：手机数码" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="parentId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>父级分类</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="选择父级分类" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value={ROOT_PARENT_ID}>顶级分类</SelectItem>
                      {parentOptions.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                          {option.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="level"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>层级</FormLabel>
                  <FormControl>
                    <Input type="number" min="1" step="1" {...field} />
                  </FormControl>
                  <FormDescription>顶级分类为 1 级</FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="sortOrder"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>排序值</FormLabel>
                  <FormControl>
                    <Input type="number" min="0" step="1" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="iconUrl"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>图标地址</FormLabel>
                  <FormControl>
                    <Input placeholder="https://…" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="imageUrl"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>banner 图片地址</FormLabel>
                  <FormControl>
                    <Input placeholder="https://…" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem className="md:col-span-2">
                  <FormLabel>描述</FormLabel>
                  <FormControl>
                    <Textarea rows={3} placeholder="可选" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="flex items-center gap-6 md:col-span-2">
              <FormField
                control={form.control}
                name="isLeaf"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-center gap-2 space-y-0">
                    <FormControl>
                      <Checkbox
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                    <FormLabel className="cursor-pointer">叶子节点</FormLabel>
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="enabled"
                render={({ field }) => (
                  <FormItem className="flex flex-row items-center gap-2 space-y-0">
                    <FormControl>
                      <Checkbox
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                    <FormLabel className="cursor-pointer">启用</FormLabel>
                  </FormItem>
                )}
              />
            </div>

            <DialogFooter className="md:col-span-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
              >
                取消
              </Button>
              <Button type="submit" disabled={isPending}>
                {isPending && <Loader2 className="size-4 animate-spin" />}
                保存
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
