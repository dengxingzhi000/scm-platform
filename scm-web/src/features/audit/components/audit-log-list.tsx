"use client";

import { useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
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
import { cn } from "@/lib/utils";
import {
  RISK_LEVEL,
  type SysAuditLogDTO,
  type SysSensitiveOperationLogDTO,
} from "@/lib/api/contracts/audit";

import { useAuditLogPage, useSensitiveLogPage } from "../hooks";

const PAGE_SIZE = 20;
const ALL = "__all__";

const formatDateTime = (value?: string | null) =>
  value ? value.replace("T", " ").slice(0, 19) : "-";

function DetailDialog({
  open,
  title,
  entries,
  onOpenChange,
}: {
  open: boolean;
  title: string;
  entries: Array<[string, unknown]>;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>原始审计字段，保留提交时的完整上下文。</DialogDescription>
        </DialogHeader>
        <div className="max-h-[60vh] space-y-2 overflow-y-auto pr-1">
          {entries.map(([label, value]) => {
            if (value === null || value === undefined || value === "") return null;
            const text =
              typeof value === "string" ? value : JSON.stringify(value, null, 2);
            return (
              <div key={label}>
                <div className="text-muted-foreground text-[10px] uppercase tracking-wide">
                  {label}
                </div>
                <pre className="bg-sunken mt-0.5 max-h-40 overflow-auto rounded-md p-2 text-[11px] whitespace-pre-wrap break-all">
                  {text}
                </pre>
              </div>
            );
          })}
        </div>
      </DialogContent>
    </Dialog>
  );
}

export function AuditLogList() {
  const [tab, setTab] = useState<"audit" | "sensitive">("audit");
  const [page, setPage] = useState(1);

  const [draft, setDraft] = useState({
    userId: "",
    operationType: "",
    module: "",
    businessType: "",
    riskLevel: ALL,
    riskScore: "",
  });
  const [applied, setApplied] = useState(draft);

  const [selected, setSelected] = useState<
    SysAuditLogDTO | SysSensitiveOperationLogDTO | null
  >(null);

  const auditQuery = useAuditLogPage({
    page,
    size: PAGE_SIZE,
    userId: applied.userId || undefined,
    operationType: applied.operationType || undefined,
    module: applied.module || undefined,
    businessType: applied.businessType || undefined,
    riskLevel:
      applied.riskLevel === ALL ? undefined : Number(applied.riskLevel),
  });

  const sensitiveQuery = useSensitiveLogPage({
    page,
    size: PAGE_SIZE,
    userId: applied.userId || undefined,
    operationType: applied.operationType || undefined,
    riskScore: applied.riskScore ? Number(applied.riskScore) : undefined,
  });

  const isLoading =
    tab === "audit" ? auditQuery.isLoading : sensitiveQuery.isLoading;
  const auditRows = auditQuery.data?.records ?? [];
  const sensitiveRows = sensitiveQuery.data?.records ?? [];
  const total =
    tab === "audit"
      ? (auditQuery.data?.total ?? 0)
      : (sensitiveQuery.data?.total ?? 0);
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const set = (key: keyof typeof draft, value: string) =>
    setDraft((prev) => ({ ...prev, [key]: value }));

  const apply = () => {
    setApplied(draft);
    setPage(1);
  };

  const reset = () => {
    const empty = {
      userId: "",
      operationType: "",
      module: "",
      businessType: "",
      riskLevel: ALL,
      riskScore: "",
    };
    setDraft(empty);
    setApplied(empty);
    setPage(1);
  };

  const detailEntries = (row: SysAuditLogDTO | SysSensitiveOperationLogDTO) =>
    Object.entries(row) as Array<[string, unknown]>;

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="安全审计"
        title="审计日志"
        description="追溯业务操作与敏感数据访问，支持按用户、模块与风险等级筛选"
      />

      <TablePanel>
        <TableToolbar>
          <div className="flex gap-1">
            {[
              { key: "audit" as const, label: "审计日志" },
              { key: "sensitive" as const, label: "敏感操作" },
            ].map((item) => (
              <button
                key={item.key}
                type="button"
                onClick={() => {
                  setTab(item.key);
                  setPage(1);
                }}
                className={cn(
                  "rounded-md px-2.5 py-1 text-[12px] transition-colors",
                  tab === item.key
                    ? "bg-[var(--color-primary-soft)] font-medium text-[var(--color-primary-text)]"
                    : "text-muted-foreground hover:bg-sunken",
                )}
              >
                {item.label}
              </button>
            ))}
          </div>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">共 {total} 条</span>
          </TableToolbarEnd>
        </TableToolbar>

        <TableToolbar>
          <Input
            className="h-7 w-44 text-xs"
            placeholder="用户 ID"
            value={draft.userId}
            onChange={(event) => set("userId", event.target.value)}
            onKeyDown={(event) => event.key === "Enter" && apply()}
          />
          <Input
            className="h-7 w-36 text-xs"
            placeholder="操作类型"
            value={draft.operationType}
            onChange={(event) => set("operationType", event.target.value)}
          />
          {tab === "audit" ? (
            <>
              <Input
                className="h-7 w-36 text-xs"
                placeholder="模块"
                value={draft.module}
                onChange={(event) => set("module", event.target.value)}
              />
              <Input
                className="h-7 w-36 text-xs"
                placeholder="业务类型"
                value={draft.businessType}
                onChange={(event) => set("businessType", event.target.value)}
              />
              <Select
                value={draft.riskLevel}
                onValueChange={(value) => set("riskLevel", value)}
              >
                <SelectTrigger size="sm" className="w-28 text-xs">
                  <SelectValue placeholder="风险等级" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>全部风险</SelectItem>
                  {Object.entries(RISK_LEVEL).map(([value, level]) => (
                    <SelectItem key={value} value={value}>
                      {level.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </>
          ) : (
            <Input
              className="h-7 w-32 text-xs"
              type="number"
              min={0}
              placeholder="风险分"
              value={draft.riskScore}
              onChange={(event) => set("riskScore", event.target.value)}
            />
          )}
          <Button size="sm" variant="outline" onClick={apply}>
            查询
          </Button>
          <Button size="sm" variant="ghost" onClick={reset}>
            重置
          </Button>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : tab === "audit" ? (
          auditRows.length === 0 ? (
            <TableEmpty>暂无审计日志</TableEmpty>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>时间</TableHead>
                  <TableHead>操作人</TableHead>
                  <TableHead>操作</TableHead>
                  <TableHead>模块</TableHead>
                  <TableHead className="min-w-[220px]">请求</TableHead>
                  <TableHead className="text-center">状态</TableHead>
                  <TableHead className="text-center">风险</TableHead>
                  <TableHead numeric>耗时</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {auditRows.map((row) => {
                  const risk = RISK_LEVEL[row.riskLevel ?? 0];
                  return (
                    <TableRow key={row.id}>
                      <TableCell>{formatDateTime(row.createTime)}</TableCell>
                      <TableCell>
                        {row.realName ?? row.username ?? row.userId ?? "-"}
                      </TableCell>
                      <TableCell>
                        <div>{row.operationType ?? "-"}</div>
                        <div className="text-muted-foreground max-w-[180px] truncate text-[10px]">
                          {row.operationDesc ?? ""}
                        </div>
                      </TableCell>
                      <TableCell>{row.operationModule ?? "-"}</TableCell>
                      <TableCell className="font-mono text-[10px]">
                        {row.requestMethod ?? "-"}{" "}
                        <span className="text-muted-foreground">
                          {row.requestUri ?? ""}
                        </span>
                      </TableCell>
                      <TableCell className="text-center">
                        <Badge
                          variant={
                            (row.responseStatus ?? 0) >= 200 &&
                            (row.responseStatus ?? 0) < 300
                              ? "success"
                              : "destructive"
                          }
                        >
                          {row.responseStatus ?? "-"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-center">
                        {risk ? (
                          <Badge variant={risk.variant}>{risk.label}</Badge>
                        ) : (
                          "-"
                        )}
                      </TableCell>
                      <TableCell numeric>
                        {row.executeTime === null || row.executeTime === undefined
                          ? "-"
                          : `${row.executeTime} ms`}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setSelected(row)}
                        >
                          详情
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )
        ) : sensitiveRows.length === 0 ? (
          <TableEmpty>暂无敏感操作日志</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>时间</TableHead>
                <TableHead>操作人</TableHead>
                <TableHead>操作类型</TableHead>
                <TableHead>敏感数据</TableHead>
                <TableHead numeric>影响行数</TableHead>
                <TableHead className="text-center">风险分</TableHead>
                <TableHead>IP</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {sensitiveRows.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>{formatDateTime(row.createTime)}</TableCell>
                  <TableCell>
                    <div>{row.realName ?? row.username ?? row.userId ?? "-"}</div>
                    <div className="text-muted-foreground text-[10px]">
                      {row.deptName ?? ""}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div>{row.operationType ?? "-"}</div>
                    <div className="text-muted-foreground text-[10px]">
                      {row.operationModule ?? ""}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div>{row.sensitiveDataType ?? "-"}</div>
                    <div className="text-muted-foreground text-[10px]">
                      {row.targetTable ?? ""}
                    </div>
                  </TableCell>
                  <TableCell numeric>{row.affectedCount ?? "-"}</TableCell>
                  <TableCell className="text-center">
                    <Badge
                      variant={
                        (row.riskScore ?? 0) >= 80
                          ? "destructive"
                          : (row.riskScore ?? 0) >= 50
                            ? "warning"
                            : "secondary"
                      }
                    >
                      {row.riskScore ?? "-"}
                    </Badge>
                  </TableCell>
                  <TableCell className="font-mono text-[10px]">
                    {row.ipAddress ?? "-"}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setSelected(row)}
                    >
                      详情
                    </Button>
                  </TableCell>
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

      <DetailDialog
        open={Boolean(selected)}
        title="审计记录详情"
        entries={selected ? detailEntries(selected) : []}
        onOpenChange={(open) => !open && setSelected(null)}
      />
    </div>
  );
}
