"use client";

import { useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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
  TablePagination,
  TablePanel,
  TableToolbar,
  TableToolbarEnd,
} from "@/components/ui/table-shell";
import { Textarea } from "@/components/ui/textarea";
import type { WmsWavePickingDTO } from "@/lib/api/contracts/warehouse";
import { useAuthStore } from "@/stores/useAuthStore";

import {
  useCancelWavePicking,
  useCompletePicking,
  useCreateWavePicking,
  useRemoveWavePicking,
  useStartPicking,
  useWarehouseList,
  useWavePickingPage,
} from "../hooks";

const PAGE_SIZE = 20;
const ALL = "__all__";

const STATUS: Record<
  number,
  { label: string; variant: "warning" | "info" | "success" | "secondary" }
> = {
  0: { label: "待拣货", variant: "warning" },
  1: { label: "拣货中", variant: "info" },
  2: { label: "已完成", variant: "success" },
  3: { label: "已取消", variant: "secondary" },
};

const formatDateTime = (value?: string | null) =>
  value ? value.replace("T", " ").slice(0, 16) : "-";

export function WavePickingList() {
  const storeUser = useAuthStore((state) => state.user);
  const [operatorId, setOperatorId] = useState("");
  const effectiveOperator = operatorId || storeUser?.id || "";

  const [page, setPage] = useState(1);
  const [warehouseId, setWarehouseId] = useState(ALL);
  const [status, setStatus] = useState(ALL);

  const { data, isLoading } = useWavePickingPage({
    page,
    size: PAGE_SIZE,
    warehouseId: warehouseId === ALL ? undefined : warehouseId,
    status: status === ALL ? undefined : Number(status),
  });
  const { data: warehouses } = useWarehouseList();

  const create = useCreateWavePicking();
  const start = useStartPicking();
  const complete = useCompletePicking();
  const cancel = useCancelWavePicking();
  const remove = useRemoveWavePicking();

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState({
    warehouseId: "",
    orderCount: "",
    outboundIds: "",
    remark: "",
  });
  const [startTarget, setStartTarget] = useState<WmsWavePickingDTO | null>(null);
  const [picker, setPicker] = useState({
    pickerId: storeUser?.id ?? "",
    pickerName: storeUser?.displayName ?? storeUser?.username ?? "",
  });
  const [confirm, setConfirm] = useState<{
    action: "cancel" | "delete";
    row: WmsWavePickingDTO;
  } | null>(null);

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const warehouseName = (id?: string | null) =>
    warehouses?.find((item) => item.id === id)?.name ?? id?.slice(0, 8) ?? "-";

  const submitCreate = async () => {
    await create.mutateAsync({
      warehouseId: createForm.warehouseId,
      orderCount: createForm.orderCount ? Number(createForm.orderCount) : null,
      outboundIds: createForm.outboundIds.trim() || null,
      remark: createForm.remark.trim() || null,
      status: 0,
    });
    setCreateOpen(false);
    setCreateForm({ warehouseId: "", orderCount: "", outboundIds: "", remark: "" });
  };

  const submitStart = async () => {
    if (!startTarget) return;
    await start.mutateAsync({
      id: startTarget.id,
      pickerId: picker.pickerId.trim(),
      pickerName: picker.pickerName.trim(),
    });
    setStartTarget(null);
  };

  const confirmAction = async () => {
    if (!confirm) return;
    if (confirm.action === "cancel") {
      await cancel.mutateAsync({
        id: confirm.row.id,
        operatorId: effectiveOperator,
      });
    } else {
      await remove.mutateAsync(confirm.row.id);
    }
    setConfirm(null);
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="仓库管理"
        title="波次拣货"
        description="按仓库聚合出库单生成拣货波次，指派拣货员并跟踪作业进度"
        actions={<Button onClick={() => setCreateOpen(true)}>新建波次</Button>}
      />

      <TablePanel>
        <TableToolbar>
          <Select
            value={warehouseId}
            onValueChange={(value) => {
              setWarehouseId(value);
              setPage(1);
            }}
          >
            <SelectTrigger size="sm" className="w-44 text-xs">
              <SelectValue placeholder="全部仓库" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部仓库</SelectItem>
              {(warehouses ?? []).map((item) => (
                <SelectItem key={item.id} value={item.id}>
                  {item.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={status}
            onValueChange={(value) => {
              setStatus(value);
              setPage(1);
            }}
          >
            <SelectTrigger size="sm" className="w-32 text-xs">
              <SelectValue placeholder="全部状态" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部状态</SelectItem>
              <SelectItem value="0">待拣货</SelectItem>
              <SelectItem value="1">拣货中</SelectItem>
              <SelectItem value="2">已完成</SelectItem>
              <SelectItem value="3">已取消</SelectItem>
            </SelectContent>
          </Select>
          <div className="flex items-center gap-1.5">
            <span className="text-muted-foreground text-[11px]">操作人</span>
            <Input
              className="h-7 w-56 text-xs"
              value={operatorId}
              placeholder={storeUser?.id ?? "未登录，请填写用户 ID"}
              onChange={(event) => setOperatorId(event.target.value)}
            />
          </div>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 个波次
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无波次拣货单</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[150px]">波次号</TableHead>
                <TableHead>仓库</TableHead>
                <TableHead numeric>订单数</TableHead>
                <TableHead numeric>商品数</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>拣货员</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead>完成时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => {
                const info = STATUS[row.status ?? 0] ?? STATUS[0];
                return (
                  <TableRow key={row.id}>
                    <TableCell className="font-mono text-[11px] font-semibold">
                      {row.waveNo ?? "-"}
                    </TableCell>
                    <TableCell>{warehouseName(row.warehouseId)}</TableCell>
                    <TableCell numeric>{row.orderCount ?? "-"}</TableCell>
                    <TableCell numeric>{row.totalItems ?? "-"}</TableCell>
                    <TableCell className="text-center">
                      <Badge variant={info.variant}>{info.label}</Badge>
                    </TableCell>
                    <TableCell>{row.pickerName ?? "-"}</TableCell>
                    <TableCell>{formatDateTime(row.createTime)}</TableCell>
                    <TableCell>{formatDateTime(row.completedAt)}</TableCell>
                    <TableCell className="text-right">
                      {row.status === 0 && (
                        <>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setStartTarget(row)}
                          >
                            开始拣货
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-destructive"
                            onClick={() => setConfirm({ action: "cancel", row })}
                          >
                            取消
                          </Button>
                        </>
                      )}
                      {row.status === 1 && (
                        <>
                          <Button
                            variant="ghost"
                            size="sm"
                            disabled={!effectiveOperator || complete.isPending}
                            onClick={() =>
                              complete.mutate({
                                id: row.id,
                                operatorId: effectiveOperator,
                              })
                            }
                          >
                            完成拣货
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-destructive"
                            onClick={() => setConfirm({ action: "cancel", row })}
                          >
                            取消
                          </Button>
                        </>
                      )}
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive"
                        onClick={() => setConfirm({ action: "delete", row })}
                      >
                        删除
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}

        <TablePagination
          page={page}
          pageSize={PAGE_SIZE}
          total={total}
          pageCount={pageCount}
          onPageChange={setPage}
        />
      </TablePanel>

      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>新建波次拣货单</DialogTitle>
            <DialogDescription>
              选择仓库并填写聚合的订单数量，波次号由后端生成。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3">
            <div>
              <Label className="mb-1.5 text-[12px]">仓库</Label>
              <Select
                value={createForm.warehouseId}
                onValueChange={(value) =>
                  setCreateForm({ ...createForm, warehouseId: value })
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="选择仓库" />
                </SelectTrigger>
                <SelectContent>
                  {(warehouses ?? []).map((item) => (
                    <SelectItem key={item.id} value={item.id}>
                      {item.name}（{item.code}）
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <Label className="mb-1.5 text-[12px]">订单数量</Label>
                <Input
                  type="number"
                  min={0}
                  value={createForm.orderCount}
                  onChange={(event) =>
                    setCreateForm({ ...createForm, orderCount: event.target.value })
                  }
                />
              </div>
              <div>
                <Label className="mb-1.5 text-[12px]">出库单 ID</Label>
                <Input
                  value={createForm.outboundIds}
                  placeholder="多个用逗号分隔"
                  onChange={(event) =>
                    setCreateForm({ ...createForm, outboundIds: event.target.value })
                  }
                />
              </div>
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">备注</Label>
              <Textarea
                rows={2}
                value={createForm.remark}
                onChange={(event) =>
                  setCreateForm({ ...createForm, remark: event.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              取消
            </Button>
            <Button
              disabled={create.isPending || !createForm.warehouseId}
              onClick={submitCreate}
            >
              {create.isPending ? "创建中…" : "创建"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={Boolean(startTarget)}
        onOpenChange={(open) => !open && setStartTarget(null)}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>开始拣货</DialogTitle>
            <DialogDescription>
              为波次 {startTarget?.waveNo} 指派拣货员，拣货开始后波次内容不可修改。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3">
            <div>
              <Label className="mb-1.5 text-[12px]">拣货员 ID</Label>
              <Input
                value={picker.pickerId}
                onChange={(event) =>
                  setPicker({ ...picker, pickerId: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">拣货员姓名</Label>
              <Input
                value={picker.pickerName}
                onChange={(event) =>
                  setPicker({ ...picker, pickerName: event.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setStartTarget(null)}>
              取消
            </Button>
            <Button
              disabled={
                start.isPending ||
                !picker.pickerId.trim() ||
                !picker.pickerName.trim()
              }
              onClick={submitStart}
            >
              {start.isPending ? "处理中…" : "开始拣货"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={Boolean(confirm)}
        title={confirm?.action === "cancel" ? "取消波次" : "删除波次"}
        description={
          confirm
            ? `${confirm.action === "cancel" ? "取消" : "删除"}波次「${confirm.row.waveNo}」吗？`
            : undefined
        }
        confirmText={confirm?.action === "cancel" ? "取消波次" : "删除"}
        destructive
        pending={cancel.isPending || remove.isPending}
        onOpenChange={(open) => !open && setConfirm(null)}
        onConfirm={confirmAction}
      />
    </div>
  );
}
