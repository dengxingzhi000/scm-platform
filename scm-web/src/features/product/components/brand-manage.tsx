"use client";

import { useCallback, useMemo, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useForm } from "react-hook-form";
import { Pencil, Plus, Search, Star, Trash2 } from "lucide-react";
import { z } from "zod";

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
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { PageHeader } from "@/components/ui/page-header";
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
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";
import type { ProdBrandDTO, ProdBrandPayload } from "@/lib/api/contracts/product";
import {
  useBrandList,
  useCreateBrand,
  useDeleteBrand,
  useUpdateBrand,
} from "../hooks/use-brand-crud";

const PAGE_SIZE = 20;

const NUMERIC_COLUMNS = new Set(["establishedYear"]);

const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, ProdBrandDTO>();
const emptyRows: ProdBrandDTO[] = [];

const brandFormSchema = z.object({
  brandCode: z.string().min(1, "品牌编码必填"),
  brandName: z.string().min(1, "品牌名称必填"),
  brandNameEn: z.string().optional(),
  logoUrl: z.string().optional(),
  description: z.string().optional(),
  website: z.string().optional(),
  country: z.string().optional(),
  establishedYear: z.string().optional(),
  sortOrder: z.string().optional(),
  featured: z.boolean(),
  enabled: z.boolean(),
});

type BrandFormValues = z.infer<typeof brandFormSchema>;

const blankForm: BrandFormValues = {
  brandCode: "",
  brandName: "",
  brandNameEn: "",
  logoUrl: "",
  description: "",
  website: "",
  country: "",
  establishedYear: "",
  sortOrder: "",
  featured: false,
  enabled: true,
};

function toFormValues(brand: ProdBrandDTO): BrandFormValues {
  return {
    brandCode: brand.brandCode ?? "",
    brandName: brand.brandName ?? "",
    brandNameEn: brand.brandNameEn ?? "",
    logoUrl: brand.logoUrl ?? "",
    description: brand.description ?? "",
    website: brand.website ?? "",
    country: brand.country ?? "",
    establishedYear: brand.establishedYear != null ? String(brand.establishedYear) : "",
    sortOrder: brand.sortOrder != null ? String(brand.sortOrder) : "",
    featured: brand.featured ?? false,
    enabled: brand.enabled ?? true,
  };
}

function toPayload(values: BrandFormValues): ProdBrandPayload {
  const year = Number.parseInt(values.establishedYear ?? "", 10);
  const order = Number.parseInt(values.sortOrder ?? "", 10);
  return {
    brandCode: values.brandCode.trim(),
    brandName: values.brandName.trim(),
    brandNameEn: values.brandNameEn?.trim() || undefined,
    logoUrl: values.logoUrl?.trim() || undefined,
    description: values.description?.trim() || undefined,
    website: values.website?.trim() || undefined,
    country: values.country?.trim() || undefined,
    establishedYear: Number.isNaN(year) ? undefined : year,
    sortOrder: Number.isNaN(order) ? undefined : order,
    featured: values.featured,
    enabled: values.enabled,
  };
}

export default function BrandManage() {
  const [current, setCurrent] = useState(1);
  const [nameDraft, setNameDraft] = useState("");
  const [name, setName] = useState("");
  const [editing, setEditing] = useState<ProdBrandDTO | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ProdBrandDTO | null>(null);

  const { data, isLoading } = useBrandList({
    page: current,
    size: PAGE_SIZE,
    name: name || undefined,
  });
  const createBrand = useCreateBrand();
  const updateBrand = useUpdateBrand();
  const deleteBrand = useDeleteBrand();

  const form = useForm<BrandFormValues>({
    resolver: zodResolver(brandFormSchema),
    defaultValues: blankForm,
    mode: "onBlur",
  });

  const openDialog = useCallback(
    (brand?: ProdBrandDTO) => {
      setEditing(brand ?? null);
      form.reset(brand ? toFormValues(brand) : blankForm);
      setDialogOpen(true);
    },
    [form],
  );

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("logoUrl", {
          header: "Logo",
          cell: (info) => {
            const url = info.getValue();
            return url ? (
              <img
                src={url}
                alt=""
                className="bg-muted size-10 rounded-md border object-contain p-0.5"
              />
            ) : (
              <div className="bg-muted size-10 rounded-md border" />
            );
          },
        }),
        columnHelper.accessor("brandCode", {
          header: "品牌编码",
          cell: (info) => (
            <span className="font-mono text-xs">{info.getValue() || "-"}</span>
          ),
        }),
        columnHelper.accessor("brandName", {
          header: "品牌名称",
          cell: (info) => info.getValue() || "-",
        }),
        columnHelper.accessor("brandNameEn", {
          header: "英文名",
          cell: (info) => info.getValue() || "-",
        }),
        columnHelper.accessor("country", {
          header: "国家",
          cell: (info) => info.getValue() || "-",
        }),
        columnHelper.accessor("establishedYear", {
          header: "成立年份",
          cell: (info) => info.getValue() ?? "-",
        }),
        columnHelper.accessor("featured", {
          header: "推荐",
          cell: (info) =>
            info.getValue() ? (
              <Badge variant="warning">
                <Star />
                推荐
              </Badge>
            ) : (
              <span className="text-muted-foreground text-xs">-</span>
            ),
        }),
        columnHelper.accessor("enabled", {
          header: "状态",
          cell: (info) =>
            info.getValue() ? (
              <Badge variant="success">启用</Badge>
            ) : (
              <Badge variant="secondary">停用</Badge>
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
                    onClick={() => openDialog(row.original)}
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
    [openDialog],
  );

  const brandTable = useTable({
    features,
    columns,
    data: data?.records ?? emptyRows,
  });

  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const applyName = (value: string) => {
    setName(value.trim());
    setCurrent(1);
  };

  const onSubmit = form.handleSubmit(async (values) => {
    const payload = toPayload(values);
    if (editing) {
      await updateBrand.mutateAsync({ id: editing.id, data: payload });
    } else {
      await createBrand.mutateAsync(payload);
    }
    setDialogOpen(false);
  });

  const isPending = createBrand.isPending || updateBrand.isPending;

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="商品中心"
        title="品牌管理"
        description="维护品牌档案、标识与启用状态"
        actions={
          <Button onClick={() => openDialog()}>
            <Plus />
            新建品牌
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <form
            onSubmit={(event) => {
              event.preventDefault();
              applyName(nameDraft);
            }}
            className="flex items-center gap-2"
          >
            <Input
              value={nameDraft}
              onChange={(event) => setNameDraft(event.target.value)}
              placeholder="搜索品牌名称"
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
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 个品牌
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : brandTable.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无品牌</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              {brandTable.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      numeric={NUMERIC_COLUMNS.has(header.column.id)}
                      className={cn(
                        header.column.id === "logoUrl" && "w-16",
                        header.column.id === "enabled" && "text-center",
                        header.column.id === "actions" && "text-right",
                      )}
                    >
                      {header.isPlaceholder ? null : (
                        <brandTable.FlexRender header={header} />
                      )}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {brandTable.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      numeric={NUMERIC_COLUMNS.has(cell.column.id)}
                      className={cn(
                        cell.column.id === "logoUrl" && "w-16",
                        cell.column.id === "enabled" && "text-center",
                        cell.column.id === "actions" && "text-right",
                      )}
                    >
                      <brandTable.FlexRender cell={cell} />
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

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{editing ? "编辑品牌" : "新建品牌"}</DialogTitle>
            <DialogDescription>
              填写品牌信息，带 * 的为必填项。
            </DialogDescription>
          </DialogHeader>

          <Form {...form}>
            <form onSubmit={onSubmit} className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <FormField
                  control={form.control}
                  name="brandCode"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>品牌编码 *</FormLabel>
                      <FormControl>
                        <Input placeholder="BRD-001" disabled={!!editing} {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="brandName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>品牌名称 *</FormLabel>
                      <FormControl>
                        <Input placeholder="例如：华为" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="brandNameEn"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>英文名称</FormLabel>
                      <FormControl>
                        <Input placeholder="HUAWEI" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="country"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>国家/地区</FormLabel>
                      <FormControl>
                        <Input placeholder="中国" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="establishedYear"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>成立年份</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          min="0"
                          step="1"
                          placeholder="2000"
                          {...field}
                        />
                      </FormControl>
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
                        <Input
                          type="number"
                          step="1"
                          placeholder="0"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="logoUrl"
                  render={({ field }) => (
                    <FormItem className="md:col-span-2">
                      <FormLabel>Logo 地址</FormLabel>
                      <FormControl>
                        <Input placeholder="https://..." {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="website"
                  render={({ field }) => (
                    <FormItem className="md:col-span-2">
                      <FormLabel>官网</FormLabel>
                      <FormControl>
                        <Input placeholder="https://..." {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>品牌描述</FormLabel>
                    <FormControl>
                      <Input placeholder="可选" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="flex items-center gap-6">
                <FormField
                  control={form.control}
                  name="featured"
                  render={({ field }) => (
                    <FormItem className="flex flex-row items-center gap-2 space-y-0">
                      <FormControl>
                        <Checkbox
                          checked={field.value}
                          onCheckedChange={field.onChange}
                        />
                      </FormControl>
                      <FormLabel className="cursor-pointer">设为推荐品牌</FormLabel>
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

              <DialogFooter>
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setDialogOpen(false)}
                >
                  取消
                </Button>
                <Button type="submit" disabled={isPending}>
                  保存
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>删除品牌</DialogTitle>
            <DialogDescription>
              确定删除「{deleteTarget?.brandName}」？此操作不可撤销。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={deleteBrand.isPending}
              onClick={() => {
                if (!deleteTarget) return;
                deleteBrand.mutate(deleteTarget.id, {
                  onSettled: () => setDeleteTarget(null),
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
