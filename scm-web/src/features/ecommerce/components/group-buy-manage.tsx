"use client";

import { useMemo, useState } from "react";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useMutation, useQuery } from "@tanstack/react-query";
import { message } from "antd";
import { Plus, Search, UserPlus, Users } from "lucide-react";

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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/ui/page-header";
import {
  TableEmpty,
  TableLoading,
  TablePanel,
  TableToolbar,
} from "@/components/ui/table-shell";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { toApiError } from "@/lib/api/envelope";
import { promotionApi } from "@/lib/api/services/ecommerce";
import type {
  CreateGroupBuyPayload,
  GroupBuy,
} from "@/lib/api/contracts/ecommerce";
import { cn } from "@/lib/utils";
import {
  formatAmount,
  formatDateTime,
  groupBuyStatusMap,
  statusMeta,
  toNumberOrUndefined,
} from "../lib/status";

const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, GroupBuy>();

const emptyCreateForm = {
  userId: "",
  activityId: "",
  productId: "",
  skuId: "",
};

export default function GroupBuyManage() {
  const [groups, setGroups] = useState<GroupBuy[]>([]);
  const [lookupId, setLookupId] = useState("");

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState(emptyCreateForm);

  const [membersTarget, setMembersTarget] = useState<GroupBuy | null>(null);

  const [joinTarget, setJoinTarget] = useState<GroupBuy | null>(null);
  const [joinUserId, setJoinUserId] = useState("");

  const membersQuery = useQuery({
    queryKey: ["ecommerce", "group-buys", membersTarget?.id, "members"],
    queryFn: () => promotionApi.groups.members(membersTarget!.id),
    enabled: !!membersTarget,
  });

  const upsertGroup = (group: GroupBuy) => {
    setGroups((prev) => {
      const exists = prev.some((item) => item.id === group.id);
      return exists
        ? prev.map((item) => (item.id === group.id ? group : item))
        : [group, ...prev];
    });
  };

  const createGroup = useMutation({
    mutationFn: (payload: CreateGroupBuyPayload) =>
      promotionApi.groups.create(payload),
    onSuccess: (group) => {
      message.success("拼团已发起");
      setCreateOpen(false);
      setCreateForm(emptyCreateForm);
      upsertGroup(group);
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const lookupGroup = useMutation({
    mutationFn: (groupBuyId: number) => promotionApi.groups.get(groupBuyId),
    onSuccess: (group) => {
      message.success(`已加载拼团 ${group.id}`);
      upsertGroup(group);
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const joinGroup = useMutation({
    mutationFn: (userId: number) =>
      promotionApi.groups.join(joinTarget!.id, userId),
    onSuccess: (group) => {
      message.success("参团成功");
      setJoinTarget(null);
      setJoinUserId("");
      upsertGroup(group);
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("id", {
          header: "拼团 ID",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("activityId", {
          header: "活动 ID",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("productId", {
          header: "商品 ID",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("skuId", {
          header: "SKU ID",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("groupPrice", {
          header: "拼团价",
          cell: (info) => (
            <span className="font-semibold">{formatAmount(info.getValue())}</span>
          ),
        }),
        columnHelper.display({
          id: "size",
          header: "成团人数",
          cell: ({ row }) => (
            <span className="tabular-nums">
              {row.original.currentSize ?? 0} / {row.original.groupSize ?? 0}
            </span>
          ),
        }),
        columnHelper.accessor("expireTime", {
          header: "失效时间",
          cell: (info) => (
            <span className="text-muted-foreground text-[11px]">
              {formatDateTime(info.getValue())}
            </span>
          ),
        }),
        columnHelper.accessor("status", {
          header: "状态",
          cell: (info) => {
            const meta = statusMeta(groupBuyStatusMap, info.getValue());
            return <Badge variant={meta.tone}>{meta.label}</Badge>;
          },
        }),
        columnHelper.display({
          id: "actions",
          header: "操作",
          cell: ({ row }) => (
            <div className="flex items-center justify-end gap-1">
              <Button
                variant="ghost"
                size="sm"
                className="h-7 px-2 text-xs"
                onClick={() => setMembersTarget(row.original)}
              >
                <Users />
                查看成员
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 px-2 text-xs"
                onClick={() => {
                  setJoinTarget(row.original);
                  setJoinUserId("");
                }}
              >
                <UserPlus />
                参团
              </Button>
            </div>
          ),
        }),
      ]),
    []
  );

  const table = useTable({
    features,
    columns,
    data: groups,
  });

  const rows = table.getRowModel().rows;
  const numericColumns = new Set(["id", "activityId", "productId", "skuId"]);

  const submitLookup = () => {
    const groupBuyId = toNumberOrUndefined(lookupId);
    if (groupBuyId === undefined) {
      message.error("请输入有效的拼团 ID");
      return;
    }
    lookupGroup.mutate(groupBuyId);
  };

  const submitCreate = () => {
    const payload: CreateGroupBuyPayload = {
      userId: Number(createForm.userId),
      activityId: Number(createForm.activityId),
      productId: Number(createForm.productId),
      skuId: Number(createForm.skuId),
    };
    createGroup.mutate(payload);
  };

  const canCreate = [
    createForm.userId,
    createForm.activityId,
    createForm.productId,
    createForm.skuId,
  ].every((value) => toNumberOrUndefined(value) !== undefined);

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="电商中心"
        title="拼团活动"
        description="发起拼团、按 ID 查询拼团并维护参团成员"
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus />
            发起拼团
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <form
            className="flex items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault();
              submitLookup();
            }}
          >
            <Input
              value={lookupId}
              onChange={(event) => setLookupId(event.target.value)}
              placeholder="输入拼团 ID 查询"
              className="h-8 w-56 text-xs"
            />
            <Button
              type="submit"
              variant="outline"
              className="h-8 px-2.5 text-xs"
              disabled={lookupGroup.isPending}
            >
              <Search />
              查询
            </Button>
          </form>
        </TableToolbar>

        {rows.length === 0 ? (
          <TableEmpty>暂无拼团数据，需先发起或按 ID 查询</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-300px)] overflow-y-auto">
            <TableHeader>
              {table.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      numeric={numericColumns.has(header.column.id)}
                      className={cn(
                        header.column.id === "status" && "text-center",
                        header.column.id === "actions" && "text-right"
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
              {rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      numeric={numericColumns.has(cell.column.id)}
                      className={cn(
                        cell.column.id === "status" && "text-center",
                        cell.column.id === "actions" && "text-right"
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
      </TablePanel>

      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>发起拼团</DialogTitle>
            <DialogDescription>
              填写用户与商品信息后创建新的拼团
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-1">
              <Label>用户 ID</Label>
              <Input
                type="number"
                value={createForm.userId}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    userId: event.target.value,
                  }))
                }
                placeholder="数字用户 ID"
              />
            </div>
            <div className="space-y-1">
              <Label>活动 ID</Label>
              <Input
                type="number"
                value={createForm.activityId}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    activityId: event.target.value,
                  }))
                }
              />
            </div>
            <div className="space-y-1">
              <Label>商品 ID</Label>
              <Input
                type="number"
                value={createForm.productId}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    productId: event.target.value,
                  }))
                }
              />
            </div>
            <div className="space-y-1">
              <Label>SKU ID</Label>
              <Input
                type="number"
                value={createForm.skuId}
                onChange={(event) =>
                  setCreateForm((prev) => ({
                    ...prev,
                    skuId: event.target.value,
                  }))
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              取消
            </Button>
            <Button
              disabled={createGroup.isPending || !canCreate}
              onClick={submitCreate}
            >
              发起
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!joinTarget}
        onOpenChange={(open) => !open && setJoinTarget(null)}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>参与拼团</DialogTitle>
            <DialogDescription>
              拼团 ID {joinTarget?.id} · 成团 {joinTarget?.currentSize ?? 0} /
              {joinTarget?.groupSize ?? 0}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-1">
            <Label>用户 ID</Label>
            <Input
              type="number"
              value={joinUserId}
              onChange={(event) => setJoinUserId(event.target.value)}
              placeholder="请输入数字用户 ID"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setJoinTarget(null)}>
              取消
            </Button>
            <Button
              disabled={
                joinGroup.isPending ||
                toNumberOrUndefined(joinUserId) === undefined
              }
              onClick={() => joinGroup.mutate(Number(joinUserId))}
            >
              参团
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!membersTarget}
        onOpenChange={(open) => !open && setMembersTarget(null)}
      >
        <DialogContent className="max-h-[86vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>拼团成员</DialogTitle>
            <DialogDescription>
              拼团 ID {membersTarget?.id} · 商品 {membersTarget?.productId}
            </DialogDescription>
          </DialogHeader>

          {membersQuery.isLoading ? (
            <TableLoading />
          ) : (membersQuery.data?.length ?? 0) === 0 ? (
            <TableEmpty className="py-10">暂无参团成员</TableEmpty>
          ) : (
            <Table containerClassName="max-h-72 overflow-y-auto">
              <TableHeader>
                <TableRow>
                  <TableHead>用户 ID</TableHead>
                  <TableHead className="text-center">状态</TableHead>
                  <TableHead>订单号</TableHead>
                  <TableHead>参团时间</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {membersQuery.data!.map((member) => (
                  <TableRow key={member.id}>
                    <TableCell className="font-mono text-[11px]">
                      {member.userId}
                    </TableCell>
                    <TableCell className="text-center tabular-nums">
                      {member.status ?? "—"}
                    </TableCell>
                    <TableCell className="font-mono text-[11px]">
                      {member.orderNo || "—"}
                    </TableCell>
                    <TableCell className="text-muted-foreground text-[11px]">
                      {formatDateTime(member.joinedAt)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setMembersTarget(null)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
