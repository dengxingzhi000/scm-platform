"use client";

import { useState } from "react";
import {
  Form,
  Input as AntInput,
  InputNumber,
  Modal,
  Select as AntSelect,
} from "antd";
import { Search, SlidersHorizontal } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
import { cn } from "@/lib/utils";
import { useAdjustStock, useInventoryList } from "../hooks";
import type { InventoryItem, StockLevel } from "../types";

const PAGE_SIZE = 20;
const ALL = "__all__";

const statusConfig: Record<
  StockLevel,
  { label: string; variant: "success" | "warning" | "secondary" }
> = {
  normal: { label: "正常", variant: "success" },
  low: { label: "偏低", variant: "warning" },
  out: { label: "缺货", variant: "secondary" },
};

const statusAccent: Record<StockLevel, string> = {
  normal: "var(--color-success)",
  low: "var(--color-warning)",
  out: "var(--color-border-strong)",
};

const statusBar: Record<StockLevel, string> = {
  normal: "bg-[var(--color-success)]",
  low: "bg-[var(--color-warning)]",
  out: "bg-muted-foreground",
};

const ADJUST_TYPE_LABELS: Record<number, string> = {
  1: "入库",
  2: "出库",
  6: "盘点调整",
  7: "调拨",
};

interface AdjustForm {
  quantity: number;
  adjustType: 1 | 2 | 6 | 7;
  remark?: string;
}

export default function InventoryList() {
  const [current, setCurrent] = useState(1);
  const [skuIdDraft, setSkuIdDraft] = useState("");
  const [skuId, setSkuId] = useState("");
  const [status, setStatus] = useState<StockLevel>();
  const [adjustTarget, setAdjustTarget] = useState<InventoryItem | null>(null);
  const [form] = Form.useForm<AdjustForm>();
  const adjustStock = useAdjustStock();

  const skuIds = skuId ? [skuId] : undefined;
  const { data, isLoading } = useInventoryList({
    current,
    pageSize: PAGE_SIZE,
    skuIds,
    status,
  });

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const submitAdjust = async () => {
    if (!adjustTarget) return;
    const values = await form.validateFields();
    await adjustStock.mutateAsync({
      skuId: adjustTarget.skuId,
      warehouseId: adjustTarget.warehouseId,
      quantity: values.quantity,
      adjustType: values.adjustType,
      remark: values.remark,
    });
    setAdjustTarget(null);
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="库存运营"
        title="库存管理"
        description="查询 SKU 库存水位并执行入库、出库与盘点调整"
      />

      <TablePanel>
        <TableToolbar>
          <form
            onSubmit={(event) => {
              event.preventDefault();
              setSkuId(skuIdDraft.trim());
              setCurrent(1);
            }}
            className="flex items-center gap-2"
          >
            {/* The query API filters by exact SKU id; there is no name or barcode keyword. */}
            <Input
              value={skuIdDraft}
              onChange={(event) => setSkuIdDraft(event.target.value)}
              placeholder="按 SKU ID 精确查询"
              className="h-8 w-64 text-xs"
            />
            <Button
              type="submit"
              variant="outline"
              size="icon"
              className="size-8"
              aria-label="查询"
            >
              <Search />
            </Button>
          </form>

          <TableToolbarEnd>
            <Select
              value={status ?? ALL}
              onValueChange={(value) => {
                setStatus(
                  value === ALL ? undefined : (value as StockLevel),
                );
                setCurrent(1);
              }}
            >
              <SelectTrigger size="sm" className="w-32 text-xs">
                <span className="text-muted-foreground">状态</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部状态</SelectItem>
                <SelectItem value="normal">正常</SelectItem>
                <SelectItem value="low">偏低</SelectItem>
                <SelectItem value="out">缺货</SelectItem>
              </SelectContent>
            </Select>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 条库存
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无库存记录</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[240px]">SKU</TableHead>
                <TableHead>库位</TableHead>
                <TableHead numeric>总库存</TableHead>
                <TableHead numeric>可用</TableHead>
                <TableHead numeric>锁定</TableHead>
                <TableHead numeric>安全库存</TableHead>
                <TableHead>水位</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>更新时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((item) => {
                const config = statusConfig[item.status];
                const percent =
                  item.safetyStock > 0
                    ? Math.round(
                        (item.availableStock / item.safetyStock) * 100,
                      )
                    : 100;

                return (
                  <TableRow key={item.id}>
                    <TableCell>
                      <div
                        className="border-l-4 pl-2"
                        style={{
                          borderLeftColor: statusAccent[item.status],
                        }}
                      >
                        <div className="font-mono text-[11px] font-semibold">
                          {item.skuId}
                        </div>
                        <div className="text-muted-foreground mt-0.5 font-mono text-[10px]">
                          {item.warehouseId}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>{item.locationCode || "-"}</TableCell>
                    <TableCell numeric>{item.totalStock}</TableCell>
                    <TableCell numeric>{item.availableStock}</TableCell>
                    <TableCell numeric>{item.lockedStock}</TableCell>
                    <TableCell numeric>{item.safetyStock}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <div className="bg-muted h-1.5 w-20 overflow-hidden rounded-full">
                          <div
                            className={cn(
                              "h-full rounded-full",
                              statusBar[item.status],
                            )}
                            style={{ width: `${Math.min(percent, 100)}%` }}
                          />
                        </div>
                        <span className="text-muted-foreground text-[10px] tabular-nums">
                          {percent}%
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={config.variant}>{config.label}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {item.updatedAt}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        className="h-7 gap-1 px-2 text-[11px]"
                        onClick={() => {
                          setAdjustTarget(item);
                          form.resetFields();
                        }}
                      >
                        <SlidersHorizontal />
                        调整
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
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

      <Modal
        title={`调整库存 · ${adjustTarget?.skuId ?? ""}`}
        open={!!adjustTarget}
        okText="提交"
        confirmLoading={adjustStock.isPending}
        onOk={submitAdjust}
        onCancel={() => setAdjustTarget(null)}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ adjustType: 6, quantity: 0 }}
        >
          <Form.Item
            name="adjustType"
            label="调整类型"
            rules={[{ required: true }]}
          >
            <AntSelect
              options={Object.entries(ADJUST_TYPE_LABELS).map(
                ([value, label]) => ({ label, value: Number(value) }),
              )}
            />
          </Form.Item>
          {/* The API takes a signed quantity: positive increases, negative decreases. */}
          <Form.Item
            name="quantity"
            label="变动数量（正数增加 / 负数减少）"
            rules={[{ required: true, message: "请输入变动数量" }]}
          >
            <InputNumber
              style={{ width: "100%" }}
              min={-1000000}
              max={1000000}
              precision={0}
            />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <AntInput.TextArea rows={2} maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
