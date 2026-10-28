"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { CheckCheck } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
import {
  useMarkAllAsRead,
  useMarkAsRead,
  useNotificationList,
  useUnreadCount,
} from "../hooks";
import { notificationService } from "../services/notification.service";
import type { Notification } from "../types";

const PAGE_SIZE = 20;
const ALL = "__all__";

const typeVariant: Record<
  Notification["type"],
  "default" | "success" | "warning" | "info" | "destructive" | "secondary"
> = {
  system: "secondary",
  order: "info",
  inventory: "warning",
  approval: "default",
  security: "destructive",
};

export default function NotificationList() {
  const router = useRouter();
  const [type, setType] = useState<string>();
  const [current, setCurrent] = useState(1);
  const { data, isLoading } = useNotificationList({ current, type });
  const { data: unreadCount } = useUnreadCount();
  const markAsRead = useMarkAsRead();
  const markAllAsRead = useMarkAllAsRead();
  const typeMap = notificationService.getTypeMap();

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const handleItemClick = (item: Notification) => {
    if (!item.isRead) markAsRead.mutate(item.id);
    if (item.link) router.push(item.link);
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
            disabled={markAllAsRead.isPending || !unreadCount}
            onClick={() => markAllAsRead.mutate()}
          >
            <CheckCheck />
            全部已读
            {unreadCount ? ` (${unreadCount})` : ""}
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <span className="text-muted-foreground text-[11px]">
            点击通知可标记已读并跳转到对应页面
          </span>
          <TableToolbarEnd>
            <Select
              value={type ?? ALL}
              onValueChange={(value) => {
                setType(value === ALL ? undefined : value);
                setCurrent(1);
              }}
            >
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
            <span className="text-muted-foreground text-[11px]">
              共 {total} 条通知
            </span>
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
                    <Badge variant={typeVariant[item.type]}>
                      {item.typeLabel}
                    </Badge>
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
          onPageChange={setCurrent}
        />
      </TablePanel>
    </div>
  );
}
