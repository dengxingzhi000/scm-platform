"use client";

import { useMemo, useState } from "react";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { message } from "antd";
import { Check, Eye, Plus, RefreshCw, Search } from "lucide-react";

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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
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
import { toApiError } from "@/lib/api/envelope";
import { promotionApi } from "@/lib/api/services/ecommerce";
import type { Coupon } from "@/lib/api/contracts/ecommerce";
import { cn } from "@/lib/utils";
import {
  couponStatusMap,
  formatDateTime,
  statusMeta,
} from "../lib/status";

const PAGE_SIZE = 20;
const ALL = "__all__";
const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, Coupon>();
const emptyRows: Coupon[] = [];

export default function CouponManage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [userIdDraft, setUserIdDraft] = useState("");
  const [userId, setUserId] = useState("");
  const [status, setStatus] = useState<number | undefined>();

  const [issueOpen, setIssueOpen] = useState(false);
  const [issueForm, setIssueForm] = useState({ userId: "", templateId: "" });

  const [useTarget, setUseTarget] = useState<Coupon | null>(null);
  const [useOrderNo, setUseOrderNo] = useState("");

  const [verifyTarget, setVerifyTarget] = useState<Coupon | null>(null);
  const [verifyForm, setVerifyForm] = useState({
    userId: "",
    orderAmount: "",
  });
  const [verified, setVerified] = useState<Coupon | null>(null);

  const [couponUser, setCouponUser] = useState<string | null>(null);

  const listQuery = useQuery({
    queryKey: ["ecommerce", "coupons", page, userId, status],
    queryFn: () =>
      promotionApi.coupons.page({
        page,
        size: PAGE_SIZE,
        userId: userId || undefined,
        status,
      }),
  });

  const userCouponsQuery = useQuery({
    queryKey: ["ecommerce", "coupons", "user", couponUser],
    queryFn: () => promotionApi.coupons.listByUser(couponUser!),
    enabled: !!couponUser,
  });

  const invalidateCoupons = () =>
    queryClient.invalidateQueries({ queryKey: ["ecommerce", "coupons"] });

  const issueCoupon = useMutation({
    mutationFn: () =>
      promotionApi.coupons.issue({
        userId: issueForm.userId.trim(),
        templateId: Number(issueForm.templateId),
      }),
    onSuccess: (coupon) => {
      message.success(`优惠券已发放：${coupon.couponCode}`);
      setIssueOpen(false);
      setIssueForm({ userId: "", templateId: "" });
      void invalidateCoupons();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const useCoupon = useMutation({
    mutationFn: () =>
      promotionApi.coupons.use({
        couponCode: useTarget!.couponCode,
        orderNo: useOrderNo.trim(),
      }),
    onSuccess: () => {
      message.success("优惠券已核销");
      setUseTarget(null);
      setUseOrderNo("");
      void invalidateCoupons();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const verifyCoupon = useMutation({
    mutationFn: () =>
      promotionApi.coupons.verify({
        userId: verifyForm.userId.trim(),
        couponCode: verifyTarget!.couponCode,
        orderAmount: Number(verifyForm.orderAmount),
      }),
    onSuccess: (coupon) => {
      message.success("优惠券校验通过");
      setVerified(coupon);
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor("couponCode", {
          header: "优惠券码",
          cell: (info) => (
            <span className="block max-w-[200px] truncate font-mono text-[11px] font-semibold">
              {info.getValue()}
            </span>
          ),
        }),
        columnHelper.accessor("templateId", {
          header: "模板 ID",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("userId", {
          header: "用户 ID",
          cell: (info) => (
            <span className="block max-w-[160px] truncate font-mono text-[11px]">
              {info.getValue()}
            </span>
          ),
        }),
        columnHelper.accessor("status", {
          header: "状态",
          cell: (info) => {
            const meta = statusMeta(couponStatusMap, info.getValue());
            return <Badge variant={meta.tone}>{meta.label}</Badge>;
          },
        }),
        columnHelper.accessor("orderNo", {
          header: "订单号",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue() || "—"}</span>
          ),
        }),
        columnHelper.accessor("usedAt", {
          header: "使用时间",
          cell: (info) => (
            <span className="text-muted-foreground text-[11px]">
              {formatDateTime(info.getValue())}
            </span>
          ),
        }),
        columnHelper.accessor("createdAt", {
          header: "发放时间",
          cell: (info) => (
            <span className="text-muted-foreground text-[11px]">
              {formatDateTime(info.getValue())}
            </span>
          ),
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
                onClick={() => {
                  setUseTarget(row.original);
                  setUseOrderNo("");
                }}
              >
                <Check />
                核销
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 px-2 text-xs"
                onClick={() => {
                  setVerifyTarget(row.original);
                  setVerifyForm({ userId: row.original.userId, orderAmount: "" });
                  setVerified(null);
                }}
              >
                <Check />
                校验
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 px-2 text-xs"
                onClick={() => setCouponUser(row.original.userId)}
              >
                <Eye />
                用户券
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
    data: listQuery.data?.records ?? emptyRows,
  });

  const total = listQuery.data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const numericColumns = new Set(["templateId"]);

  const applyUserId = () => {
    setUserId(userIdDraft.trim());
    setPage(1);
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="电商中心"
        title="优惠券"
        description="查询、发放、校验与核销用户优惠券"
        actions={
          <Button onClick={() => setIssueOpen(true)}>
            <Plus />
            发放优惠券
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <form
            className="flex items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault();
              applyUserId();
            }}
          >
            <Input
              value={userIdDraft}
              onChange={(event) => setUserIdDraft(event.target.value)}
              placeholder="用户 ID"
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
            <Button
              type="button"
              variant="outline"
              className="h-8 px-2.5 text-xs"
              onClick={() => void listQuery.refetch()}
              disabled={listQuery.isFetching}
            >
              <RefreshCw />
              刷新
            </Button>
          </form>

          <TableToolbarEnd>
            <Select
              value={status === undefined ? ALL : String(status)}
              onValueChange={(value) => {
                setStatus(value === ALL ? undefined : Number(value));
                setPage(1);
              }}
            >
              <SelectTrigger size="sm" className="w-32 text-xs">
                <span className="text-muted-foreground">状态</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部</SelectItem>
                <SelectItem value="1">未使用</SelectItem>
                <SelectItem value="2">已使用</SelectItem>
                <SelectItem value="3">已过期</SelectItem>
              </SelectContent>
            </Select>
          </TableToolbarEnd>
        </TableToolbar>

        {listQuery.isLoading ? (
          <TableLoading />
        ) : table.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无优惠券数据</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
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
              {table.getRowModel().rows.map((row) => (
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

        <TablePagination
          page={page}
          pageSize={PAGE_SIZE}
          total={total}
          pageCount={pageCount}
          onPageChange={setPage}
        />
      </TablePanel>

      <Dialog open={issueOpen} onOpenChange={setIssueOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>发放优惠券</DialogTitle>
            <DialogDescription>按用户与模板发放一张优惠券</DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1">
              <Label>用户 ID</Label>
              <Input
                value={issueForm.userId}
                onChange={(event) =>
                  setIssueForm((prev) => ({
                    ...prev,
                    userId: event.target.value,
                  }))
                }
                placeholder="请输入用户 ID"
              />
            </div>
            <div className="space-y-1">
              <Label>模板 ID</Label>
              <Input
                type="number"
                min={1}
                value={issueForm.templateId}
                onChange={(event) =>
                  setIssueForm((prev) => ({
                    ...prev,
                    templateId: event.target.value,
                  }))
                }
                placeholder="请输入优惠券模板 ID"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIssueOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                issueCoupon.isPending ||
                !issueForm.userId.trim() ||
                !issueForm.templateId.trim()
              }
              onClick={() => issueCoupon.mutate()}
            >
              发放
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!useTarget}
        onOpenChange={(open) => !open && setUseTarget(null)}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>核销优惠券</DialogTitle>
            <DialogDescription>{useTarget?.couponCode}</DialogDescription>
          </DialogHeader>
          <div className="space-y-1">
            <Label>订单号</Label>
            <Input
              value={useOrderNo}
              onChange={(event) => setUseOrderNo(event.target.value)}
              placeholder="请输入核销订单号"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setUseTarget(null)}>
              取消
            </Button>
            <Button
              disabled={useCoupon.isPending || !useOrderNo.trim()}
              onClick={() => useCoupon.mutate()}
            >
              核销
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!verifyTarget}
        onOpenChange={(open) => {
          if (!open) {
            setVerifyTarget(null);
            setVerified(null);
          }
        }}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>校验优惠券</DialogTitle>
            <DialogDescription>{verifyTarget?.couponCode}</DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1">
              <Label>用户 ID</Label>
              <Input
                value={verifyForm.userId}
                onChange={(event) =>
                  setVerifyForm((prev) => ({
                    ...prev,
                    userId: event.target.value,
                  }))
                }
                placeholder="请输入用户 ID"
              />
            </div>
            <div className="space-y-1">
              <Label>订单金额</Label>
              <Input
                type="number"
                min={0}
                value={verifyForm.orderAmount}
                onChange={(event) =>
                  setVerifyForm((prev) => ({
                    ...prev,
                    orderAmount: event.target.value,
                  }))
                }
                placeholder="请输入订单金额"
              />
            </div>
          </div>

          {verified && (
            <>
              <Separator />
              <div className="rounded-lg border border-[var(--color-border-secondary)] px-3 py-2 text-xs">
                <div className="flex items-center justify-between">
                  <span className="text-muted-foreground">校验结果</span>
                  <Badge
                    variant={
                      statusMeta(couponStatusMap, verified.status).tone
                    }
                  >
                    {statusMeta(couponStatusMap, verified.status).label}
                  </Badge>
                </div>
                <div className="mt-1.5 grid gap-1">
                  <span className="font-mono">{verified.couponCode}</span>
                  <span className="text-muted-foreground">
                    用户 {verified.userId} · 模板 {verified.templateId}
                  </span>
                  <span className="text-muted-foreground">
                    {verified.orderNo ? `订单 ${verified.orderNo}` : "未绑定订单"}
                  </span>
                </div>
              </div>
            </>
          )}

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                setVerifyTarget(null);
                setVerified(null);
              }}
            >
              关闭
            </Button>
            <Button
              disabled={
                verifyCoupon.isPending ||
                !verifyForm.userId.trim() ||
                !verifyForm.orderAmount.trim()
              }
              onClick={() => verifyCoupon.mutate()}
            >
              校验
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!couponUser}
        onOpenChange={(open) => !open && setCouponUser(null)}
      >
        <DialogContent className="max-h-[86vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>用户优惠券</DialogTitle>
            <DialogDescription>{couponUser}</DialogDescription>
          </DialogHeader>

          {userCouponsQuery.isLoading ? (
            <p className="text-muted-foreground text-xs">加载中…</p>
          ) : (userCouponsQuery.data?.length ?? 0) === 0 ? (
            <p className="text-muted-foreground text-xs">该用户暂无优惠券</p>
          ) : (
            <div className="space-y-2">
              {userCouponsQuery.data!.map((coupon) => {
                const meta = statusMeta(couponStatusMap, coupon.status);
                return (
                  <div
                    key={coupon.id}
                    className="flex items-center justify-between gap-3 rounded-lg border border-[var(--color-border-secondary)] px-3 py-2 text-xs"
                  >
                    <div className="min-w-0">
                      <span className="block truncate font-mono font-semibold">
                        {coupon.couponCode}
                      </span>
                      <span className="text-muted-foreground">
                        模板 {coupon.templateId} · 发放 {formatDateTime(coupon.createdAt)}
                      </span>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                      {coupon.orderNo && (
                        <span className="text-muted-foreground font-mono text-[10px]">
                          {coupon.orderNo}
                        </span>
                      )}
                      <Badge variant={meta.tone}>{meta.label}</Badge>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setCouponUser(null)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
