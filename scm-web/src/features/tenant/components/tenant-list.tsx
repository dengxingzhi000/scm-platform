"use client";

import { useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
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
import type { TenantDTO } from "@/lib/api/contracts/tenant";

import {
  useDeleteTenant,
  useTenantList,
  useToggleTenant,
} from "../hooks";
import { formatDate, TENANT_TYPE, tenantStatus } from "../lib/format";
import { TenantDetailDialog } from "./tenant-detail-dialog";
import { TenantFormDialog } from "./tenant-form-dialog";

const PAGE_SIZE = 20;
const ALL = "__all__";

export function TenantList() {
  const [page, setPage] = useState(1);
  const [nameDraft, setNameDraft] = useState("");
  const [name, setName] = useState("");
  const [type, setType] = useState<number | undefined>();
  const [status, setStatus] = useState<number | undefined>();

  const { data, isLoading } = useTenantList({
    page,
    size: PAGE_SIZE,
    tenantName: name || undefined,
    tenantType: type,
    status,
  });
  const toggle = useToggleTenant();
  const remove = useDeleteTenant();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<TenantDTO | null>(null);
  const [detail, setDetail] = useState<TenantDTO | null>(null);
  const [deleting, setDeleting] = useState<TenantDTO | null>(null);

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };

  const openEdit = (tenant: TenantDTO) => {
    setEditing(tenant);
    setFormOpen(true);
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="租户管理"
        title="租户列表"
        description="管理平台租户档案、订阅与资源配额"
        actions={
          <Button onClick={openCreate}>开通租户</Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <Input
            className="h-7 w-52 text-xs"
            placeholder="租户名称"
            value={nameDraft}
            onChange={(event) => setNameDraft(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                setName(nameDraft.trim());
                setPage(1);
              }
            }}
          />
          <Select
            value={type === undefined ? ALL : String(type)}
            onValueChange={(value) => {
              setType(value === ALL ? undefined : Number(value));
              setPage(1);
            }}
          >
            <SelectTrigger size="sm" className="w-32 text-xs">
              <SelectValue placeholder="类型" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部类型</SelectItem>
              {Object.entries(TENANT_TYPE).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={status === undefined ? ALL : String(status)}
            onValueChange={(value) => {
              setStatus(value === ALL ? undefined : Number(value));
              setPage(1);
            }}
          >
            <SelectTrigger size="sm" className="w-32 text-xs">
              <SelectValue placeholder="状态" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部状态</SelectItem>
              <SelectItem value="0">试用中</SelectItem>
              <SelectItem value="1">正式</SelectItem>
              <SelectItem value="2">已暂停</SelectItem>
              <SelectItem value="3">已过期</SelectItem>
            </SelectContent>
          </Select>
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setName(nameDraft.trim());
              setPage(1);
            }}
          >
            查询
          </Button>
          <Button
            size="sm"
            variant="ghost"
            onClick={() => {
              setNameDraft("");
              setName("");
              setType(undefined);
              setStatus(undefined);
              setPage(1);
            }}
          >
            重置
          </Button>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 个租户
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无租户</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[200px]">租户</TableHead>
                <TableHead>类型</TableHead>
                <TableHead>联系人</TableHead>
                <TableHead>有效期</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((tenant) => {
                const statusInfo = tenantStatus(tenant.status);
                const active = tenant.status === 1 || tenant.status === 0;
                return (
                  <TableRow key={tenant.id}>
                    <TableCell>
                      <div className="font-medium">{tenant.tenantName}</div>
                      <div className="text-muted-foreground font-mono text-[10px]">
                        {tenant.tenantCode}
                      </div>
                    </TableCell>
                    <TableCell>
                      {TENANT_TYPE[tenant.tenantType ?? 1] ?? "-"}
                    </TableCell>
                    <TableCell>
                      {tenant.contactName ?? "-"}
                      {tenant.contactPhone && (
                        <div className="text-muted-foreground text-[10px]">
                          {tenant.contactPhone}
                        </div>
                      )}
                    </TableCell>
                    <TableCell>
                      {tenant.contractEndDate
                        ? formatDate(tenant.contractEndDate)
                        : formatDate(tenant.trialEndDate)}
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={statusInfo.variant}>
                        {statusInfo.label}
                      </Badge>
                    </TableCell>
                    <TableCell>{formatDate(tenant.createTime)}</TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setDetail(tenant)}
                      >
                        详情
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => openEdit(tenant)}
                      >
                        编辑
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        disabled={toggle.isPending}
                        onClick={() =>
                          toggle.mutate({ id: tenant.id, enable: !active })
                        }
                      >
                        {active ? "停用" : "启用"}
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive"
                        onClick={() => setDeleting(tenant)}
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

      {formOpen && (
        <TenantFormDialog
          key={editing?.id ?? "create"}
          open={formOpen}
          tenant={editing}
          onOpenChange={setFormOpen}
        />
      )}

      <TenantDetailDialog
        key={detail?.id ?? "tenant-detail"}
        open={Boolean(detail)}
        tenant={detail}
        onOpenChange={(open) => !open && setDetail(null)}
      />

      <ConfirmDialog
        open={Boolean(deleting)}
        title="删除租户"
        description={
          deleting
            ? `确定删除「${deleting.tenantName}」吗？删除后该租户的数据将不再可访问。`
            : undefined
        }
        confirmText="删除"
        destructive
        pending={remove.isPending}
        onOpenChange={(open) => !open && setDeleting(null)}
        onConfirm={async () => {
          if (!deleting) return;
          await remove.mutateAsync(deleting.id);
          setDeleting(null);
        }}
      />
    </div>
  );
}
