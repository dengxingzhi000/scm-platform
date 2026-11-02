"use client";

import { useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { CheckCheck, Search, Trash2 } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
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
import { useMounted } from "@/hooks/use-mounted";

import {
  useDeleteNotification,
  useDeleteNotifications,
  useMarkAllAsRead,
  useMarkAsRead,
  useNotificationList,
  useUnreadCount,
} from "../hooks";
import { TYPE_VARIANT } from "../lib/mappers";
import { notificationService } from "../services/notification.service";
import type { Notification } from "../types";

const PAGE_SIZE = 20;
const ALL = "__all__";

type ReadState = typeof ALL | "unread" | "read";

interface PendingDelete {
  ids: string[];
  title: string;
}

export default function NotificationList() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const [type, setType] = useState<string | undefined>(searchParams.get("type") ?? undefined);
  const [readState, setReadState] = useState<ReadState>(
    (searchParams.get("isRead") as ReadState) ?? ALL,
  );
  const [keyword, setKeyword] = useState(searchParams.get("keyword") ?? "");
  const [draft, setDraft] = useState(keyword);
  const [startTime, setStartTime] = useState(searchParams.get("startTime") ?? "");
  const [endTime, setEndTime] = useState(searchParams.get("endTime") ?? "");
  const [current, setCurrent] = useState(1);
  const [selected, setSelected] = useState<string[]>([]);
  const [pendingDelete, setPendingDelete] = useState<PendingDelete | null>(null);

  const { data, isLoading } = useNotificationList({
    current,
    pageSize: PAGE_SIZE,
    type,
    isRead: readState === ALL ? undefined : readState === "read",
    keyword: keyword || undefined,
    startTime: startTime || undefined,
    endTime: endTime || undefined,
  });
  const { data: unreadCount } = useUnreadCount();
  const markAsRead = useMarkAsRead();
  const markAllAsRead = useMarkAllAsRead();
  const deleteOne = useDeleteNotification();
  const deleteMany = useDeleteNotifications();
  const mounted = useMounted();
  const typeMap = notificationService.getTypeMap();

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const allSelected = rows.length > 0 && rows.every((row) => selected.includes(row.id));

  const replaceQuery = (mutate: (params: URLSearchParams) => void) => {
    const params = new URLSearchParams(searchParams.toString());
    mutate(params);
    const query = params.toString();
    router.replace(query ? `${pathname}?${query}` : pathname);
  };

  const applyFilter = (patch: Record<string, string | undefined>) => {
    setCurrent(1);
    setSelected([]);
    replaceQuery((params) => {
      for (const [key, value] of Object.entries(patch)) {
        if (value) params.set(key, value);
        else params.delete(key);
      }
    });
  };

  const applyType = (value: string) => {
    const next = value === ALL ? undefined : value;
    setType(next);
    applyFilter({ type: next });
  };

  const applyReadState = (value: ReadState) => {
    setReadState(value);
    applyFilter({ isRead: value === ALL ? undefined : value });
  };

  const applyKeyword = () => {
    const next = draft.trim();
    setKeyword(next);
    applyFilter({ keyword: next });
  };

  const applyRange = (from: string, to: string) => {
    setStartTime(from);
    setEndTime(to);
    applyFilter({ startTime: from, endTime: to });
  };

  const toggleRow = (id: string) => {
    setSelected((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id],
    );
  };

  const handleItemClick = (item: Notification) => {
    if (!item.isRead) markAsRead.mutate(item.id);
    if (item.link) router.push(item.link);
  };

  const confirmDelete = () => {
    if (!pendingDelete) return;
    const { ids } = pendingDelete;
    const done = () => {
      setPendingDelete(null);
      setSelected((prev) => prev.filter((id) => !ids.includes(id)));
      // Removing the last row of a page would otherwise leave an empty page.
      if (ids.length >= rows.length && current > 1) setCurrent(current - 1);
    };
    if (ids.length === 1) deleteOne.mutate(ids[0], { onSuccess: done });
    else deleteMany.mutate(ids, { onSuccess: done });
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="消息中心"
        title="通知管理"
        description="查看系统、订单、库存与安全通知"
        actions={
          <Button
            variant="outline"
            disabled={markAllAsRead.isPending || !mounted || !unreadCount}
            onClick={() => markAllAsRead.mutate()}
          >
            <CheckCheck />
            全部已读
            {mounted && unreadCount ? ` (${unreadCount})` : ""}
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
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              placeholder="搜索标题或内容"
              className="h-8 w-52 text-xs"
            />
            <Button type="submit" variant="outline" size="sm" className="h-8 text-[11px]">
              <Search />
              搜索
            </Button>
          </form>
          <TableToolbarEnd>
            <Select value={type ?? ALL} onValueChange={applyType}>
              <SelectTrigger size="sm" className="w-32 text-xs">
                <span className="text-muted-foreground">类型</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部类型</SelectItem>
                {Object.entries(typeMap).map(([key, value]) => (
                  <SelectItem key={key} value={key}>
                    {value.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select
              value={readState}
              onValueChange={(value) => applyReadState(value as ReadState)}
            >
              <SelectTrigger size="sm" className="w-28 text-xs">
                <span className="text-muted-foreground">状态</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部状态</SelectItem>
                <SelectItem value="unread">未读</SelectItem>
                <SelectItem value="read">已读</SelectItem>
              </SelectContent>
            </Select>
            <Input
              type="datetime-local"
              value={startTime}
              onChange={(event) => applyRange(event.target.value, endTime)}
              className="h-8 w-44 text-xs"
              aria-label="开始时间"
            />
            <Input
              type="datetime-local"
              value={endTime}
              onChange={(event) => applyRange(startTime, event.target.value)}
              className="h-8 w-44 text-xs"
              aria-label="结束时间"
            />
            {(startTime || endTime) && (
              <Button
                variant="ghost"
                size="sm"
                className="h-8 text-[11px]"
                onClick={() => applyRange("", "")}
              >
                清除时间
              </Button>
            )}
            <Button
              variant="outline"
              size="sm"
              className="h-8 text-[11px]"
              disabled={!selected.length || deleteMany.isPending}
              onClick={() =>
                setPendingDelete({ ids: selected, title: `删除选中的 ${selected.length} 条通知？` })
              }
            >
              <Trash2 />
              批量删除
            </Button>
            <span className="text-muted-foreground text-[11px]">共 {total} 条通知</span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无通知</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-10">
                  <Checkbox
                    checked={allSelected}
                    onCheckedChange={() =>
                      setSelected(allSelected ? [] : rows.map((row) => row.id))
                    }
                    aria-label="全选当前页"
                  />
                </TableHead>
                <TableHead className="min-w-[320px]">通知</TableHead>
                <TableHead>类型</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((item) => (
                <TableRow
                  key={item.id}
                  className={item.link ? "cursor-pointer" : undefined}
                  onClick={() => handleItemClick(item)}
                >
                  <TableCell onClick={(event) => event.stopPropagation()}>
                    <Checkbox
                      checked={selected.includes(item.id)}
                      onCheckedChange={() => toggleRow(item.id)}
                      aria-label={`选择 ${item.title}`}
                    />
                  </TableCell>
                  <TableCell>
                    <div
                      className="border-l-4 pl-2"
                      style={{
                        borderLeftColor: item.isRead
                          ? "var(--color-border)"
                          : "var(--color-primary)",
                      }}
                    >
                      <div
                        className={
                          item.isRead
                            ? "max-w-[420px] truncate text-[12px]"
                            : "max-w-[420px] truncate text-[12px] font-semibold"
                        }
                      >
                        {item.title}
                      </div>
                      <div className="text-muted-foreground mt-0.5 max-w-[520px] truncate text-[10px]">
                        {item.content}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant={TYPE_VARIANT[item.type]}>{item.typeLabel}</Badge>
                  </TableCell>
                  <TableCell className="text-center">
                    <Badge variant={item.isRead ? "secondary" : "default"}>
                      {item.isRead ? "已读" : "未读"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {item.createdAt}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-1">
                      {!item.isRead && (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-7 text-[11px]"
                          onClick={(event) => {
                            event.stopPropagation();
                            markAsRead.mutate(item.id);
                          }}
                        >
                          标为已读
                        </Button>
                      )}
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-7 text-[11px]"
                        onClick={(event) => {
                          event.stopPropagation();
                          setPendingDelete({ ids: [item.id], title: `删除「${item.title}」？` });
                        }}
                      >
                        删除
                      </Button>
                    </div>
                  </TableCell>
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
          onPageChange={(page) => {
            setCurrent(page);
            setSelected([]);
          }}
        />
      </TablePanel>

      <ConfirmDialog
        open={pendingDelete !== null}
        title={pendingDelete?.title ?? ""}
        description="删除后不可恢复。"
        confirmText="删除"
        destructive
        pending={deleteOne.isPending || deleteMany.isPending}
        onOpenChange={(open) => {
          if (!open) setPendingDelete(null);
        }}
        onConfirm={confirmDelete}
      />
    </div>
  );
}
