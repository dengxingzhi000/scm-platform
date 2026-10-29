"use client";

import { useMemo, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useForm } from "react-hook-form";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { z } from "zod";

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
  ProdSkuDTO,
  ProdSkuPayload,
  ProductStatus,
} from "@/lib/api/contracts/product";
import {
  useCreateSku,
  useDeleteSku,
  useSkuList,
  useSpuOptions,
  useUpdateSku,
} from "../hooks/use-sku-crud";

const PAGE_SIZE = 20;
const ALL = "__all__";
const SPU_OPTION_SIZE = 200;

const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, ProdSkuDTO>();
const emptyRows: ProdSkuDTO[] = [];

const statusConfig: Record<
  number,
  { label: string; variant: "success" | "destructive" | "secondary" | "outline" }
> = {
  0: { label: "草稿", variant: "secondary" },
  1: { label: "在售", variant: "success" },
  2: { label: "下架", variant: "destructive" },
  3: { label: "已删除", variant: "outline" },
};

const currency = (value?: number | null) =>
  value == null ? "-" : `¥${value.toLocaleString()}`;

const statusAccent: Record<number, string> = {
  0: "var(--color-border-strong)",
  1: "var(--color-success)",
  2: "var(--color-error)",
  3: "var(--color-border-strong)",
};

const NUMERIC_COLUMNS = new Set([
  "sellingPrice",
  "originalPrice",
  "availableStock",
  "lockedStock",
]);

const skuFormSchema = z.object({
  spuId: z.string().min(1, "请选择所属 SPU"),
  skuCode: z.string().min(1, "SKU 编码必填"),
  skuName: z.string().min(1, "SKU 名称必填"),
  attributes: z.string().optional(),
  originalPrice: z.string().optional(),
  sellingPrice: z
    .string()
    .refine((v) => v.trim() !== "" && !Number.isNaN(Number(v)), "售价必填且需为数字"),
  costPrice: z.string().optional(),
  stock: z.string().optional(),
  barcode: z.string().optional(),
  imageUrl: z.string().optional(),
  images: z.string().optional(),
  weight: z.string().optional(),
  volume: z.string().optional(),
  status: z.string(),
});

type SkuFormValues = z.infer<typeof skuFormSchema>;

const blankForm: SkuFormValues = {
  spuId: "",
  skuCode: "",
  skuName: "",
  attributes: "",
  originalPrice: "",
  sellingPrice: "",
  costPrice: "",
  stock: "",
  barcode: "",
  imageUrl: "",
  images: "",
  weight: "",
  volume: "",
  status: "0",
};

const numStr = (value?: number | null) => (value == null ? "" : String(value));

function toFormValues(sku: ProdSkuDTO): SkuFormValues {
  return {
    spuId: sku.spuId ?? "",
    skuCode: sku.skuCode ?? "",
    skuName: sku.skuName ?? "",
    attributes: sku.attributes ?? "",
    originalPrice: numStr(sku.originalPrice),
    sellingPrice: numStr(sku.sellingPrice),
    costPrice: numStr(sku.costPrice),
    stock: numStr(sku.stock),
    barcode: sku.barcode ?? "",
    imageUrl: sku.imageUrl ?? "",
    images: sku.images ?? "",
    weight: numStr(sku.weight),
    volume: numStr(sku.volume),
    status: String(sku.status ?? 0),
  };
}

function toPayload(values: SkuFormValues): ProdSkuPayload {
  const toNumber = (raw?: string) => {
    if (raw == null || raw.trim() === "") return undefined;
    const parsed = Number(raw);
    return Number.isNaN(parsed) ? undefined : parsed;
  };
  return {
    spuId: values.spuId,
    skuCode: values.skuCode.trim(),
    skuName: values.skuName.trim(),
    attributes: values.attributes?.trim() || undefined,
    originalPrice: toNumber(values.originalPrice),
    sellingPrice: Number(values.sellingPrice),
    costPrice: toNumber(values.costPrice),
    stock: toNumber(values.stock),
    barcode: values.barcode?.trim() || undefined,
    imageUrl: values.imageUrl?.trim() || undefined,
    images: values.images?.trim() || undefined,
    weight: toNumber(values.weight),
    volume: toNumber(values.volume),
    status: (Number(values.status) as ProductStatus) ?? 0,
  };
}

export default function SkuManage() {
  const [current, setCurrent] = useState(1);
  const [spuId, setSpuId] = useState<string>();
  const [editing, setEditing] = useState<ProdSkuDTO | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ProdSkuDTO | null>(null);

  const { data, isLoading } = useSkuList({
    page: current,
    size: PAGE_SIZE,
    spuId,
  });
  const { data: spuPage } = useSpuOptions({ page: 1, size: SPU_OPTION_SIZE });
  const createSku = useCreateSku();
  const updateSku = useUpdateSku();
  const deleteSku = useDeleteSku();

  const spuList = useMemo(() => spuPage?.records ?? [], [spuPage]);
  const spuMap = useMemo(
    () => new Map(spuList.map((spu) => [spu.id, spu.spuName || spu.spuCode || spu.id])),
    [spuList],
  );

  const form = useForm<SkuFormValues>({
    resolver: zodResolver(skuFormSchema),
    defaultValues: blankForm,
    mode: "onBlur",
  });

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.display({
          id: "sku",
          header: "SKU",
          cell: ({ row }) => {
            const sku = row.original;
            return (
              <div
                className="border-l-4 pl-2"
                style={{
                  borderLeftColor:
                    statusAccent[sku.status ?? -1] ??
                    "var(--color-border-strong)",
                }}
              >
                <div className="max-w-[280px] truncate text-[12px] font-semibold">
                  {sku.skuName || "-"}
                </div>
                <div className="text-muted-foreground mt-0.5 font-mono text-[10px]">
                  {sku.skuCode || "-"}
                </div>
              </div>
            );
          },
        }),
        columnHelper.accessor("spuId", {
          header: "所属 SPU",
          cell: (info) => spuMap.get(info.getValue() ?? "") ?? "-",
        }),
        columnHelper.accessor("sellingPrice", {
          header: "售价",
          cell: (info) => currency(info.getValue()),
        }),
        columnHelper.accessor("originalPrice", {
          header: "原价",
          cell: (info) => currency(info.getValue()),
        }),
        columnHelper.accessor("availableStock", {
          header: "可用库存",
          cell: (info) => info.getValue() ?? 0,
        }),
        columnHelper.accessor("lockedStock", {
          header: "锁定库存",
          cell: (info) => info.getValue() ?? 0,
        }),
        columnHelper.accessor("status", {
          header: "状态",
          cell: (info) => {
            const config = statusConfig[info.getValue() ?? -1];
            return config ? (
              <Badge variant={config.variant}>{config.label}</Badge>
            ) : (
              "-"
            );
          },
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
    [spuMap],
  );

  const skuTable = useTable({
    features,
    columns,
    data: data?.records ?? emptyRows,
  });

  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const openDialog = (sku?: ProdSkuDTO) => {
    setEditing(sku ?? null);
    form.reset(sku ? toFormValues(sku) : { ...blankForm, spuId: spuId ?? "" });
    setDialogOpen(true);
  };

  const onSubmit = form.handleSubmit(async (values) => {
    const payload = toPayload(values);
    if (editing) {
      await updateSku.mutateAsync({ id: editing.id, data: payload });
    } else {
      await createSku.mutateAsync(payload);
    }
    setDialogOpen(false);
  });

  const isPending = createSku.isPending || updateSku.isPending;

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="商品中心"
        title="SKU 管理"
        description="按 SPU 维护规格、价格与可售库存"
        actions={
          <Button onClick={() => openDialog()}>
            <Plus />
            新建 SKU
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <span className="text-muted-foreground text-[11px]">
            按 SPU 筛选规格
          </span>
          <TableToolbarEnd>
            <Select
              value={spuId ?? ALL}
              onValueChange={(value) => {
                setSpuId(value === ALL ? undefined : value);
                setCurrent(1);
              }}
            >
              <SelectTrigger size="sm" className="w-64 text-xs">
                <span className="text-muted-foreground">SPU</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部 SPU</SelectItem>
                {spuList.map((spu) => (
                  <SelectItem key={spu.id} value={spu.id}>
                    {spu.spuName || spu.spuCode || spu.id}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : skuTable.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无 SKU</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              {skuTable.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      numeric={NUMERIC_COLUMNS.has(header.column.id)}
                      className={cn(
                        header.column.id === "sku" && "min-w-[240px]",
                        header.column.id === "status" && "text-center",
                        header.column.id === "actions" && "text-right",
                      )}
                    >
                      {header.isPlaceholder ? null : (
                        <skuTable.FlexRender header={header} />
                      )}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {skuTable.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      numeric={NUMERIC_COLUMNS.has(cell.column.id)}
                      className={cn(
                        cell.column.id === "sku" && "min-w-[240px]",
                        cell.column.id === "status" && "text-center",
                        cell.column.id === "actions" && "text-right",
                      )}
                    >
                      <skuTable.FlexRender cell={cell} />
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
            <DialogTitle>{editing ? "编辑 SKU" : "新建 SKU"}</DialogTitle>
            <DialogDescription>
              填写 SKU 信息，带 * 的为必填项。
            </DialogDescription>
          </DialogHeader>

          <Form {...form}>
            <form onSubmit={onSubmit} className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <FormField
                  control={form.control}
                  name="spuId"
                  render={({ field }) => (
                    <FormItem className="md:col-span-2">
                      <FormLabel>所属 SPU *</FormLabel>
                      <Select
                        value={field.value || undefined}
                        onValueChange={field.onChange}
                      >
                        <FormControl>
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="选择 SPU" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {spuList.map((spu) => (
                            <SelectItem key={spu.id} value={spu.id}>
                              {spu.spuName || spu.spuCode || spu.id}
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
                  name="skuCode"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>SKU 编码 *</FormLabel>
                      <FormControl>
                        <Input placeholder="SKU-001" disabled={!!editing} {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="skuName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>SKU 名称 *</FormLabel>
                      <FormControl>
                        <Input placeholder="例如：华为 Mate 60 曜金黑 256GB" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="sellingPrice"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>售价（元）*</FormLabel>
                      <FormControl>
                        <Input type="number" min="0" step="0.01" placeholder="0.00" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="originalPrice"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>原价（元）</FormLabel>
                      <FormControl>
                        <Input type="number" min="0" step="0.01" placeholder="0.00" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="costPrice"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>成本价（元）</FormLabel>
                      <FormControl>
                        <Input type="number" min="0" step="0.01" placeholder="0.00" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="stock"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>库存</FormLabel>
                      <FormControl>
                        <Input type="number" min="0" step="1" placeholder="0" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="barcode"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>条码</FormLabel>
                      <FormControl>
                        <Input placeholder="6900000000000" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="status"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>状态</FormLabel>
                      <Select
                        value={field.value}
                        onValueChange={field.onChange}
                      >
                        <FormControl>
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="选择状态" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="0">草稿</SelectItem>
                          <SelectItem value="1">在售</SelectItem>
                          <SelectItem value="2">下架</SelectItem>
                          <SelectItem value="3">已删除</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="weight"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>重量</FormLabel>
                      <FormControl>
                        <Input type="number" min="0" step="0.01" placeholder="0" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="volume"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>体积</FormLabel>
                      <FormControl>
                        <Input type="number" min="0" step="0.01" placeholder="0" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="imageUrl"
                  render={({ field }) => (
                    <FormItem className="md:col-span-2">
                      <FormLabel>主图地址</FormLabel>
                      <FormControl>
                        <Input placeholder="https://..." {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="images"
                  render={({ field }) => (
                    <FormItem className="md:col-span-2">
                      <FormLabel>图片列表（逗号分隔 URL）</FormLabel>
                      <FormControl>
                        <Input placeholder="https://a.jpg,https://b.jpg" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="attributes"
                  render={({ field }) => (
                    <FormItem className="md:col-span-2">
                      <FormLabel>规格属性（JSON 字符串）</FormLabel>
                      <FormControl>
                        <Textarea
                          rows={3}
                          placeholder='{"颜色":"曜金黑","内存":"256GB"}'
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
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
            <DialogTitle>删除 SKU</DialogTitle>
            <DialogDescription>
              确定删除「{deleteTarget?.skuName}」？此操作不可撤销。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={deleteSku.isPending}
              onClick={() => {
                if (!deleteTarget) return;
                deleteSku.mutate(deleteTarget.id, {
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
