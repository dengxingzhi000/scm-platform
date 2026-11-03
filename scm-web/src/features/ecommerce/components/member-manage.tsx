"use client";

import { useMemo, useState } from "react";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { message } from "antd";
import {
  Coins,
  Eye,
  MapPin,
  Plus,
  RefreshCw,
  Search,
  Star,
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
import { Textarea } from "@/components/ui/textarea";
import { toApiError } from "@/lib/api/envelope";
import { memberApi } from "@/lib/api/services/ecommerce";
import type {
  EcomMember,
  MemberAddress,
  MemberAddressPayload,
} from "@/lib/api/contracts/ecommerce";
import { cn } from "@/lib/utils";
import {
  formatAmount,
  formatDate,
  formatDateTime,
  memberStatusMap,
  statusMeta,
} from "../lib/status";

const PAGE_SIZE = 20;
const ALL = "__all__";
const features = tableFeatures({});
const columnHelper = createColumnHelper<typeof features, EcomMember>();
const emptyRows: EcomMember[] = [];

const GENDER_LABELS: Record<number, string> = { 0: "未知", 1: "男", 2: "女" };

interface AddressFormState {
  receiverName: string;
  phone: string;
  province: string;
  city: string;
  district: string;
  detailAddress: string;
  isDefault: boolean;
}

const emptyAddressForm: AddressFormState = {
  receiverName: "",
  phone: "",
  province: "",
  city: "",
  district: "",
  detailAddress: "",
  isDefault: false,
};

export default function MemberManage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState<number | undefined>();
  const [detail, setDetail] = useState<EcomMember | null>(null);

  const [levelTarget, setLevelTarget] = useState<EcomMember | null>(null);
  const [levelValue, setLevelValue] = useState("1");

  const [pointsTarget, setPointsTarget] = useState<EcomMember | null>(null);
  const [pointsMode, setPointsMode] = useState<"add" | "deduct">("add");
  const [pointsForm, setPointsForm] = useState({
    points: "",
    source: "",
    orderNo: "",
    description: "",
  });

  const [addressFormOpen, setAddressFormOpen] = useState(false);
  const [addressForm, setAddressForm] =
    useState<AddressFormState>(emptyAddressForm);

  const listQuery = useQuery({
    queryKey: ["ecommerce", "members", page, keyword, status],
    queryFn: () =>
      memberApi.page({
        page,
        size: PAGE_SIZE,
        keyword: keyword || undefined,
        status,
      }),
  });

  const addressesQuery = useQuery({
    queryKey: ["ecommerce", "members", detail?.userId, "addresses"],
    queryFn: () => memberApi.addresses.list(detail!.userId),
    enabled: !!detail,
  });

  const pointsBalanceQuery = useQuery({
    queryKey: ["ecommerce", "members", detail?.userId, "points"],
    queryFn: () => memberApi.points.balance(detail!.userId),
    enabled: !!detail,
  });

  const pointsLogQuery = useQuery({
    queryKey: ["ecommerce", "members", detail?.userId, "points", "log"],
    queryFn: () => memberApi.points.log(detail!.userId),
    enabled: !!detail,
  });

  const invalidateMembers = () =>
    queryClient.invalidateQueries({ queryKey: ["ecommerce", "members"] });

  const updateLevel = useMutation({
    mutationFn: () => memberApi.updateLevel(levelTarget!.userId, Number(levelValue)),
    onSuccess: () => {
      message.success("会员等级已更新");
      setLevelTarget(null);
      void invalidateMembers();
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const changePoints = useMutation({
    mutationFn: () => {
      const payload = {
        points: Number(pointsForm.points),
        source: pointsForm.source || undefined,
        orderNo: pointsForm.orderNo || undefined,
        description: pointsForm.description || undefined,
      };
      return pointsMode === "add"
        ? memberApi.points.add(pointsTarget!.userId, payload)
        : memberApi.points.deduct(pointsTarget!.userId, payload);
    },
    onSuccess: () => {
      message.success(pointsMode === "add" ? "积分已增加" : "积分已扣减");
      setPointsTarget(null);
      setPointsForm({ points: "", source: "", orderNo: "", description: "" });
      void invalidateMembers();
      void queryClient.invalidateQueries({
        queryKey: ["ecommerce", "members", detail?.userId],
      });
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const createAddress = useMutation({
    mutationFn: (payload: MemberAddressPayload) =>
      memberApi.addresses.create(detail!.userId, payload),
    onSuccess: () => {
      message.success("收货地址已新增");
      setAddressFormOpen(false);
      setAddressForm(emptyAddressForm);
      void queryClient.invalidateQueries({
        queryKey: ["ecommerce", "members", detail?.userId, "addresses"],
      });
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const removeAddress = useMutation({
    mutationFn: (addressId: number) =>
      memberApi.addresses.remove(detail!.userId, addressId),
    onSuccess: () => {
      message.success("收货地址已删除");
      void queryClient.invalidateQueries({
        queryKey: ["ecommerce", "members", detail?.userId, "addresses"],
      });
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const setDefaultAddress = useMutation({
    mutationFn: (addressId: number) =>
      memberApi.addresses.setDefault(detail!.userId, addressId),
    onSuccess: () => {
      message.success("默认地址已更新");
      void queryClient.invalidateQueries({
        queryKey: ["ecommerce", "members", detail?.userId, "addresses"],
      });
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.display({
          id: "member",
          header: "会员",
          cell: ({ row }) => {
            const member = row.original;
            return (
              <div className="min-w-[180px]">
                <span className="text-foreground block max-w-[240px] truncate text-[12px] font-semibold">
                  {member.nickname || member.memberNo}
                </span>
                <span className="text-muted-foreground mt-0.5 block font-mono text-[10px]">
                  {member.memberNo}
                </span>
              </div>
            );
          },
        }),
        columnHelper.accessor("userId", {
          header: "用户 ID",
          cell: (info) => (
            <span className="font-mono text-[11px]">{info.getValue()}</span>
          ),
        }),
        columnHelper.accessor("memberLevel", {
          header: "等级",
          cell: (info) => (
            <Badge variant="info">V{info.getValue() ?? 1}</Badge>
          ),
        }),
        columnHelper.accessor("points", {
          header: "积分",
          cell: (info) => (info.getValue() ?? 0).toLocaleString(),
        }),
        columnHelper.accessor("totalSpent", {
          header: "累计消费",
          cell: (info) => formatAmount(info.getValue()),
        }),
        columnHelper.accessor("status", {
          header: "状态",
          cell: (info) => {
            const meta = statusMeta(memberStatusMap, info.getValue());
            return <Badge variant={meta.tone}>{meta.label}</Badge>;
          },
        }),
        columnHelper.accessor("createdAt", {
          header: "注册时间",
          cell: (info) => (
            <span className="text-muted-foreground text-[11px]">
              {formatDate(info.getValue())}
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
                onClick={() => setDetail(row.original)}
              >
                <Eye />
                详情
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 px-2 text-xs"
                onClick={() => {
                  setLevelTarget(row.original);
                  setLevelValue(String(row.original.memberLevel ?? 1));
                }}
              >
                <Star />
                等级
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="h-7 px-2 text-xs"
                onClick={() => {
                  setPointsTarget(row.original);
                  setPointsMode("add");
                }}
              >
                <Coins />
                积分
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
  const numericColumns = new Set(["points", "totalSpent"]);

  const applyKeyword = () => {
    setKeyword(keywordDraft.trim());
    setPage(1);
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="电商中心"
        title="会员管理"
        description="维护会员档案、等级、积分与收货地址"
        actions={
          <Button
            variant="outline"
            onClick={() => void listQuery.refetch()}
            disabled={listQuery.isFetching}
          >
            <RefreshCw />
            刷新
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <form
            className="flex items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault();
              applyKeyword();
            }}
          >
            <Input
              value={keywordDraft}
              onChange={(event) => setKeywordDraft(event.target.value)}
              placeholder="昵称 / 会员号 / 用户 ID"
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
                <SelectItem value={ALL}>全部状态</SelectItem>
                <SelectItem value="1">正常</SelectItem>
                <SelectItem value="0">停用</SelectItem>
              </SelectContent>
            </Select>
          </TableToolbarEnd>
        </TableToolbar>

        {listQuery.isLoading ? (
          <TableLoading />
        ) : table.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无会员数据</TableEmpty>
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

      <Dialog open={!!detail} onOpenChange={(open) => !open && setDetail(null)}>
        <DialogContent className="max-h-[86vh] max-w-3xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>会员详情</DialogTitle>
            <DialogDescription>
              {detail?.nickname || detail?.memberNo} · {detail?.userId}
            </DialogDescription>
          </DialogHeader>

          {detail && (
            <div className="space-y-5">
              <div className="grid gap-3 sm:grid-cols-3">
                <Info label="会员号" value={detail.memberNo} />
                <Info label="会员等级" value={`V${detail.memberLevel ?? 1}`} />
                <Info label="状态" value={statusMeta(memberStatusMap, detail.status).label} />
                <Info label="性别" value={GENDER_LABELS[detail.gender ?? 0] ?? "未知"} />
                <Info label="生日" value={formatDate(detail.birthday)} />
                <Info label="注册时间" value={formatDateTime(detail.createdAt)} />
                <Info label="累计消费" value={formatAmount(detail.totalSpent)} />
                <Info label="积分余额" value={(pointsBalanceQuery.data ?? detail.points ?? 0).toLocaleString()} />
              </div>

              <Separator />

              <section className="space-y-2">
                <div className="flex items-center justify-between">
                  <h3 className="flex items-center gap-1.5 text-[13px] font-semibold">
                    <MapPin className="size-4" />
                    收货地址
                  </h3>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => {
                      setAddressForm(emptyAddressForm);
                      setAddressFormOpen(true);
                    }}
                  >
                    <Plus />
                    新增地址
                  </Button>
                </div>
                {addressesQuery.isLoading ? (
                  <p className="text-muted-foreground text-xs">加载中…</p>
                ) : (addressesQuery.data?.length ?? 0) === 0 ? (
                  <p className="text-muted-foreground text-xs">暂无收货地址</p>
                ) : (
                  <div className="space-y-2">
                    {addressesQuery.data!.map((address) => (
                      <AddressRow
                        key={address.id}
                        address={address}
                        onDelete={() => removeAddress.mutate(address.id)}
                        onDefault={() => setDefaultAddress.mutate(address.id)}
                        busy={
                          removeAddress.isPending || setDefaultAddress.isPending
                        }
                      />
                    ))}
                  </div>
                )}
              </section>

              <Separator />

              <section className="space-y-2">
                <div className="flex items-center justify-between">
                  <h3 className="flex items-center gap-1.5 text-[13px] font-semibold">
                    <Coins className="size-4" />
                    积分流水
                  </h3>
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => {
                        setPointsTarget(detail);
                        setPointsMode("add");
                      }}
                    >
                      增加积分
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => {
                        setPointsTarget(detail);
                        setPointsMode("deduct");
                      }}
                    >
                      扣减积分
                    </Button>
                  </div>
                </div>
                {pointsLogQuery.isLoading ? (
                  <p className="text-muted-foreground text-xs">加载中…</p>
                ) : (pointsLogQuery.data?.length ?? 0) === 0 ? (
                  <p className="text-muted-foreground text-xs">暂无积分流水</p>
                ) : (
                  <div className="max-h-56 space-y-1 overflow-y-auto rounded-lg border border-[var(--color-border-secondary)] p-2">
                    {pointsLogQuery.data!.map((log) => (
                      <div
                        key={log.id}
                        className="flex items-center justify-between gap-3 rounded-md px-2 py-1.5 text-xs hover:bg-[var(--color-bg-sunken)]"
                      >
                        <span
                          className={cn(
                            "font-mono font-semibold",
                            log.points >= 0
                              ? "text-[var(--color-success-text)]"
                              : "text-[var(--color-error-text)]"
                          )}
                        >
                          {log.points >= 0 ? "+" : ""}
                          {log.points}
                        </span>
                        <span className="text-muted-foreground min-w-0 flex-1 truncate">
                          {log.description || log.source || "—"}
                        </span>
                        <span className="text-muted-foreground text-[10px]">
                          {formatDateTime(log.createdAt)}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </section>
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setDetail(null)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!levelTarget}
        onOpenChange={(open) => !open && setLevelTarget(null)}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>调整会员等级</DialogTitle>
            <DialogDescription>
              {levelTarget?.nickname || levelTarget?.memberNo}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label>目标等级</Label>
            <Input
              type="number"
              min={1}
              value={levelValue}
              onChange={(event) => setLevelValue(event.target.value)}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setLevelTarget(null)}>
              取消
            </Button>
            <Button
              disabled={updateLevel.isPending}
              onClick={() => updateLevel.mutate()}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!pointsTarget}
        onOpenChange={(open) => !open && setPointsTarget(null)}
      >
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>
              {pointsMode === "add" ? "增加积分" : "扣减积分"}
            </DialogTitle>
            <DialogDescription>
              {pointsTarget?.nickname || pointsTarget?.memberNo}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1">
              <Label>积分数量</Label>
              <Input
                type="number"
                min={1}
                value={pointsForm.points}
                onChange={(event) =>
                  setPointsForm((prev) => ({
                    ...prev,
                    points: event.target.value,
                  }))
                }
                placeholder="请输入积分数量"
              />
            </div>
            <div className="space-y-1">
              <Label>来源</Label>
              <Input
                value={pointsForm.source}
                onChange={(event) =>
                  setPointsForm((prev) => ({
                    ...prev,
                    source: event.target.value,
                  }))
                }
                placeholder="如：手工调整 / 订单奖励"
              />
            </div>
            <div className="space-y-1">
              <Label>关联订单号</Label>
              <Input
                value={pointsForm.orderNo}
                onChange={(event) =>
                  setPointsForm((prev) => ({
                    ...prev,
                    orderNo: event.target.value,
                  }))
                }
                placeholder="可选"
              />
            </div>
            <div className="space-y-1">
              <Label>说明</Label>
              <Textarea
                value={pointsForm.description}
                onChange={(event) =>
                  setPointsForm((prev) => ({
                    ...prev,
                    description: event.target.value,
                  }))
                }
                rows={3}
                placeholder="可选"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setPointsTarget(null)}>
              取消
            </Button>
            <Button
              disabled={
                changePoints.isPending || !pointsForm.points.trim()
              }
              onClick={() => changePoints.mutate()}
            >
              提交
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={addressFormOpen} onOpenChange={setAddressFormOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>新增收货地址</DialogTitle>
            <DialogDescription>{detail?.userId}</DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="收货人">
              <Input
                value={addressForm.receiverName}
                onChange={(event) =>
                  setAddressForm((prev) => ({
                    ...prev,
                    receiverName: event.target.value,
                  }))
                }
              />
            </Field>
            <Field label="手机号">
              <Input
                value={addressForm.phone}
                onChange={(event) =>
                  setAddressForm((prev) => ({
                    ...prev,
                    phone: event.target.value,
                  }))
                }
              />
            </Field>
            <Field label="省份">
              <Input
                value={addressForm.province}
                onChange={(event) =>
                  setAddressForm((prev) => ({
                    ...prev,
                    province: event.target.value,
                  }))
                }
              />
            </Field>
            <Field label="城市">
              <Input
                value={addressForm.city}
                onChange={(event) =>
                  setAddressForm((prev) => ({
                    ...prev,
                    city: event.target.value,
                  }))
                }
              />
            </Field>
            <Field label="区县">
              <Input
                value={addressForm.district}
                onChange={(event) =>
                  setAddressForm((prev) => ({
                    ...prev,
                    district: event.target.value,
                  }))
                }
              />
            </Field>
            <Field label="详细地址">
              <Input
                value={addressForm.detailAddress}
                onChange={(event) =>
                  setAddressForm((prev) => ({
                    ...prev,
                    detailAddress: event.target.value,
                  }))
                }
              />
            </Field>
          </div>
          <label className="flex items-center gap-2 text-xs">
            <Checkbox
              checked={addressForm.isDefault}
              onCheckedChange={(checked) =>
                setAddressForm((prev) => ({
                  ...prev,
                  isDefault: checked === true,
                }))
              }
            />
            设为默认地址
          </label>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAddressFormOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                createAddress.isPending ||
                !addressForm.receiverName.trim() ||
                !addressForm.phone.trim()
              }
              onClick={() => createAddress.mutate(addressForm)}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-[var(--color-border-secondary)] px-3 py-2">
      <p className="text-muted-foreground text-[10px] tracking-[0.08em] uppercase">
        {label}
      </p>
      <p className="mt-0.5 truncate text-[12px] font-medium">{value}</p>
    </div>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-1">
      <Label className="text-xs">{label}</Label>
      {children}
    </div>
  );
}

function AddressRow({
  address,
  onDelete,
  onDefault,
  busy,
}: {
  address: MemberAddress;
  onDelete: () => void;
  onDefault: () => void;
  busy: boolean;
}) {
  return (
    <div className="flex items-start justify-between gap-3 rounded-lg border border-[var(--color-border-secondary)] px-3 py-2">
      <div className="min-w-0 text-xs">
        <div className="flex items-center gap-2">
          <span className="font-semibold">{address.receiverName}</span>
          <span className="text-muted-foreground">{address.phone}</span>
          {address.isDefault && <Badge variant="success">默认</Badge>}
        </div>
        <p className="text-muted-foreground mt-1">
          {[address.province, address.city, address.district, address.detailAddress]
            .filter(Boolean)
            .join(" ")}
        </p>
      </div>
      <div className="flex shrink-0 gap-1">
        {!address.isDefault && (
          <Button size="sm" variant="ghost" disabled={busy} onClick={onDefault}>
            设为默认
          </Button>
        )}
        <Button
          size="sm"
          variant="ghost"
          className="text-destructive"
          disabled={busy}
          onClick={onDelete}
        >
          删除
        </Button>
      </div>
    </div>
  );
}

