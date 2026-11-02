"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
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
import { useAuthStore } from "@/stores/useAuthStore";

import { useCreateTracking, useTrackingPage } from "../hooks";

const PAGE_SIZE = 20;

const formatDateTime = (value?: string | null) =>
  value ? value.replace("T", " ").slice(0, 16) : "-";

export function TrackingList() {
  const storeUser = useAuthStore((state) => state.user);
  const [page, setPage] = useState(1);
  const [waybillNo, setWaybillNo] = useState("");
  const [trackStatus, setTrackStatus] = useState("");

  const { data, isLoading } = useTrackingPage({
    page,
    size: PAGE_SIZE,
    waybillNo: waybillNo.trim() || undefined,
    trackStatus: trackStatus.trim() || undefined,
  });
  const create = useCreateTracking();

  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({
    waybillId: "",
    waybillNo: "",
    trackTime: "",
    trackStatus: "",
    location: "",
    description: "",
    operator: "",
  });

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const submit = async () => {
    await create.mutateAsync({
      waybillId: form.waybillId.trim(),
      waybillNo: form.waybillNo.trim() || null,
      trackTime: form.trackTime ? `${form.trackTime}:00` : null,
      trackStatus: form.trackStatus.trim() || null,
      location: form.location.trim() || null,
      description: form.description.trim() || null,
      operator:
        form.operator.trim() ||
        storeUser?.displayName ||
        storeUser?.username ||
        null,
      source: "MANUAL",
    });
    setOpen(false);
    setForm({
      waybillId: "",
      waybillNo: "",
      trackTime: "",
      trackStatus: "",
      location: "",
      description: "",
      operator: "",
    });
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="物流管理"
        title="物流跟踪"
        description="按运单号查询轨迹，或手工补录物流节点"
        actions={<Button onClick={() => setOpen(true)}>上报轨迹</Button>}
      />

      <TablePanel>
        <TableToolbar>
          <Input
            className="h-7 w-48 text-xs"
            placeholder="运单号"
            value={waybillNo}
            onChange={(event) => setWaybillNo(event.target.value)}
            onKeyDown={(event) => event.key === "Enter" && setPage(1)}
          />
          <Input
            className="h-7 w-40 text-xs"
            placeholder="轨迹状态"
            value={trackStatus}
            onChange={(event) => setTrackStatus(event.target.value)}
            onKeyDown={(event) => event.key === "Enter" && setPage(1)}
          />
          <Button size="sm" variant="outline" onClick={() => setPage(1)}>
            查询
          </Button>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 条轨迹
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无物流轨迹</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>时间</TableHead>
                <TableHead>运单号</TableHead>
                <TableHead>状态</TableHead>
                <TableHead>位置</TableHead>
                <TableHead className="min-w-[240px]">描述</TableHead>
                <TableHead>操作人</TableHead>
                <TableHead>来源</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>{formatDateTime(row.trackTime)}</TableCell>
                  <TableCell className="font-mono text-[11px]">
                    {row.waybillNo ?? row.waybillId ?? "-"}
                  </TableCell>
                  <TableCell>{row.trackStatus ?? "-"}</TableCell>
                  <TableCell>{row.location ?? "-"}</TableCell>
                  <TableCell className="max-w-[360px] truncate">
                    {row.description ?? "-"}
                  </TableCell>
                  <TableCell>{row.operator ?? "-"}</TableCell>
                  <TableCell>{row.source ?? "-"}</TableCell>
                </TableRow>
              ))}
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

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>上报物流轨迹</DialogTitle>
            <DialogDescription>
              运单主键 ID 为必填（后端按该字段关联运单），运单号仅作展示。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label className="mb-1.5 text-[12px]">运单 ID</Label>
              <Input
                value={form.waybillId}
                onChange={(event) =>
                  setForm({ ...form, waybillId: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">运单号</Label>
              <Input
                value={form.waybillNo}
                onChange={(event) =>
                  setForm({ ...form, waybillNo: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">轨迹时间</Label>
              <Input
                type="datetime-local"
                value={form.trackTime}
                onChange={(event) =>
                  setForm({ ...form, trackTime: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">轨迹状态</Label>
              <Input
                value={form.trackStatus}
                placeholder="例如 IN_TRANSIT"
                onChange={(event) =>
                  setForm({ ...form, trackStatus: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">位置</Label>
              <Input
                value={form.location}
                onChange={(event) =>
                  setForm({ ...form, location: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">操作人</Label>
              <Input
                value={form.operator}
                placeholder={storeUser?.displayName ?? "默认当前用户"}
                onChange={(event) =>
                  setForm({ ...form, operator: event.target.value })
                }
              />
            </div>
            <div className="sm:col-span-2">
              <Label className="mb-1.5 text-[12px]">描述</Label>
              <Textarea
                rows={2}
                value={form.description}
                onChange={(event) =>
                  setForm({ ...form, description: event.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                create.isPending ||
                !form.waybillId.trim() ||
                !form.description.trim()
              }
              onClick={submit}
            >
              {create.isPending ? "提交中…" : "提交"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
