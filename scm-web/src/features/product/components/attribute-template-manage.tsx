"use client";

import { useEffect, useMemo, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { Loader2, Pencil, Plus, Trash2 } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
  ProdAttributeTemplateDTO,
  ProdAttributeTemplatePayload,
} from "@/lib/api/contracts/product";
import { useCategoryOptions } from "../hooks/use-category-crud";
import {
  getAttributesJsonError,
  useAttributeTemplatePage,
  useCreateAttributeTemplate,
  useDeleteAttributeTemplate,
  useUpdateAttributeTemplate,
} from "../hooks/use-attribute-templates";

const PAGE_SIZE = 20;

const NUMERIC_COLUMNS = new Set(["attributes"]);
const ALL = "__all__";
const EMPTY_ATTRIBUTES = "[]";

const features = tableFeatures({});
const columnHelper = createColumnHelper<
  typeof features,
  ProdAttributeTemplateDTO
>();
const emptyRows: ProdAttributeTemplateDTO[] = [];

const templateFormSchema = z
  .object({
    templateName: z
      .string()
      .trim()
      .min(1, "请输入模板名称")
      .max(100, "模板名称最多 100 个字符"),
    categoryId: z.string().min(1, "请选择所属分类"),
    attributes: z.string(),
    remark: z.string().max(500, "备注最多 500 个字符").optional(),
  })
  .refine(
    (values) =>
      values.attributes.trim() === "" ||
      getAttributesJsonError(values.attributes) === null,
    { message: "属性不是合法的 JSON，请检查括号与引号", path: ["attributes"] },
  );

type TemplateFormValues = z.infer<typeof templateFormSchema>;

const defaultValues: TemplateFormValues = {
  templateName: "",
  categoryId: "",
  attributes: "",
  remark: "",
};

function toFormValues(template: ProdAttributeTemplateDTO): TemplateFormValues {
  return {
    templateName: template.templateName ?? "",
    categoryId: template.categoryId ?? "",
    attributes: template.attributes ?? "",
    remark: template.remark ?? "",
  };
}

function toPayload(values: TemplateFormValues): ProdAttributeTemplatePayload {
  return {
    templateName: values.templateName.trim(),
    categoryId: values.categoryId,
    // The backend stores this opaquely — keep it a JSON string, never an object.
    attributes: values.attributes.trim() || EMPTY_ATTRIBUTES,
    remark: values.remark?.trim() || undefined,
  };
}

/** Attribute count for the list column; unparseable payloads show as "-". */
function attributeCount(attributes: string | null | undefined) {
  if (!attributes) return null;
  try {
    const parsed = JSON.parse(attributes) as unknown;
    if (Array.isArray(parsed)) return parsed.length;
    if (parsed && typeof parsed === "object") return Object.keys(parsed).length;
    return null;
  } catch {
    return null;
  }
}

export default function AttributeTemplateManage() {
  const [current, setCurrent] = useState(1);
  const [categoryId, setCategoryId] = useState<string>();
  const [formOpen, setFormOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<ProdAttributeTemplateDTO | null>(
    null,
  );
  const [deleteTarget, setDeleteTarget] = useState<ProdAttributeTemplateDTO | null>(
    null,
  );

  const { data, isLoading } = useAttributeTemplatePage({
    page: current,
    size: PAGE_SIZE,
    categoryId,
  });
  // Mirrors `listCategories`: the category catalog comes from one large page.
  const { data: categories } = useCategoryOptions();
  const deleteTemplate = useDeleteAttributeTemplate();

  const categoryNameById = useMemo(
    () =>
      new Map(
        (categories ?? []).map((category) => [
          category.id,
          category.categoryName ?? "",
        ]),
      ),
    [categories],
  );

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("templateName", {
          header: "模板名称",
          cell: (info) => info.getValue() || "-",
        }),
        columnHelper.accessor("categoryId", {
          header: "所属分类",
          cell: (info) =>
            categoryNameById.get(info.getValue() ?? "") || info.getValue() || "-",
        }),
        columnHelper.accessor("attributes", {
          header: "属性数",
          cell: (info) => {
            const count = attributeCount(info.getValue());
            return count === null ? (
              <Badge variant="warning">JSON 异常</Badge>
            ) : (
              count
            );
          },
        }),
        columnHelper.accessor("remark", {
          header: "备注",
          cell: (info) => info.getValue() || "-",
        }),
        columnHelper.accessor("updateTime", {
          header: "更新时间",
          cell: (info) => (
            <span className="text-muted-foreground text-xs">
              {info.getValue() || "-"}
            </span>
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
                    aria-label="编辑"
                    onClick={() => {
                      setEditTarget(row.original);
                      setFormOpen(true);
                    }}
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
    [categoryNameById],
  );

  const table = useTable({
    features,
    columns,
    data: data?.records ?? emptyRows,
  });

  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="商品中心"
        title="属性模板管理"
        description="为分类定义 SKU 属性字段与录入规则"
        actions={
          <Button
            onClick={() => {
              setEditTarget(null);
              setFormOpen(true);
            }}
          >
            <Plus />
            新建模板
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <span className="text-muted-foreground text-[11px]">
            按分类筛选属性模板
          </span>
          <TableToolbarEnd>
            <Select
              value={categoryId ?? ALL}
              onValueChange={(value) => {
                setCategoryId(value === ALL ? undefined : value);
                setCurrent(1);
              }}
            >
              <SelectTrigger size="sm" className="w-56 text-xs">
                <span className="text-muted-foreground">分类</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部分类</SelectItem>
                {(categories ?? []).map((category) => (
                  <SelectItem key={category.id} value={category.id}>
                    {category.categoryName}
                    {category.categoryCode ? `（${category.categoryCode}）` : ""}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : table.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无属性模板</TableEmpty>
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

        <TablePagination
          page={current}
          pageSize={PAGE_SIZE}
          total={total}
          pageCount={pageCount}
          onPageChange={setCurrent}
        />
      </TablePanel>

      <TemplateFormDialog
        open={formOpen}
        template={editTarget}
        categories={categories ?? []}
        onOpenChange={setFormOpen}
      />

      <Dialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>删除属性模板</DialogTitle>
            <DialogDescription>
              确定删除「{deleteTarget?.templateName}」？此操作不可撤销。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={deleteTemplate.isPending}
              onClick={() => {
                if (!deleteTarget) return;
                deleteTemplate.mutate(deleteTarget.id, {
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

interface TemplateFormDialogProps {
  open: boolean;
  template: ProdAttributeTemplateDTO | null;
  categories: { id: string; categoryName?: string | null }[];
  onOpenChange: (open: boolean) => void;
}

function TemplateFormDialog({
  open,
  template,
  categories,
  onOpenChange,
}: TemplateFormDialogProps) {
  const createTemplate = useCreateAttributeTemplate();
  const updateTemplate = useUpdateAttributeTemplate();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const form = useForm<TemplateFormValues>({
    resolver: zodResolver(templateFormSchema),
    defaultValues,
    mode: "onBlur",
  });

  useEffect(() => {
    if (!open) return;
    form.reset(template ? toFormValues(template) : defaultValues);
  }, [open, template, form]);

  const attributesDraft = useWatch({
    control: form.control,
    name: "attributes",
  });
  const liveJsonError =
    attributesDraft && attributesDraft.trim()
      ? getAttributesJsonError(attributesDraft)
      : null;

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitError(null);
    const payload = toPayload(values);
    try {
      if (template) {
        await updateTemplate.mutateAsync({ id: template.id, payload });
      } else {
        await createTemplate.mutateAsync(payload);
      }
      onOpenChange(false);
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : "保存失败");
    }
  });

  const isPending = createTemplate.isPending || updateTemplate.isPending;

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
          <DialogTitle>{template ? "编辑属性模板" : "新建属性模板"}</DialogTitle>
          <DialogDescription>
            属性以 JSON 字符串保存，后端原样存储、不做解析，请确保格式正确。
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
              name="templateName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>模板名称</FormLabel>
                  <FormControl>
                    <Input placeholder="例如：手机规格模板" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="categoryId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>所属分类</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="选择分类" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {categories.map((category) => (
                        <SelectItem key={category.id} value={category.id}>
                          {category.categoryName}
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
              name="attributes"
              render={({ field }) => (
                <FormItem className="md:col-span-2">
                  <FormLabel>属性定义（JSON）</FormLabel>
                  <FormControl>
                    <Textarea
                      rows={8}
                      className="font-mono text-xs"
                      placeholder={'[\n  { "name": "颜色", "type": "string", "required": true, "options": ["黑色", "白色"] }\n]'}
                      {...field}
                    />
                  </FormControl>
                  <FormDescription>
                    预期为 {`{name, type, required, options}`} 描述符组成的数组。
                    {liveJsonError && (
                      <span className="text-destructive block">
                        JSON 解析失败：{liveJsonError}
                      </span>
                    )}
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="remark"
              render={({ field }) => (
                <FormItem className="md:col-span-2">
                  <FormLabel>备注</FormLabel>
                  <FormControl>
                    <Textarea rows={2} placeholder="可选" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

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
