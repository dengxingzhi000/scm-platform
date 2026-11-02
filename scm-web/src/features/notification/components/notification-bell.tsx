"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight, Bell, CheckCheck, Inbox } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton";
import { formatRelativeTime } from "@/lib/format/date";

import {
  useMarkAllAsRead,
  useMarkAsRead,
  useNotificationList,
  useUnreadCount,
} from "../hooks";
import { TYPE_VARIANT } from "../lib/mappers";
import type { Notification } from "../types";

const PANEL_ROWS = 6;

export default function NotificationBell() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const { data: unreadCount = 0 } = useUnreadCount();
  // The panel lists unread only, so its rows and the badge always answer to the same number.
  const { data, isFetching, isError } = useNotificationList(
    { current: 1, pageSize: PANEL_ROWS, isRead: false },
    { enabled: open, staleTime: 0 },
  );
  const markAsRead = useMarkAsRead();
  const markAllAsRead = useMarkAllAsRead();

  const rows = data?.records ?? [];

  const openNotification = (item: Notification) => {
    markAsRead.mutate(item.id);
    if (item.link) {
      setOpen(false);
      router.push(item.link);
    }
  };

  const goToList = () => {
    setOpen(false);
    router.push("/notification");
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="relative"
          aria-label={`通知，${unreadCount} 条未读`}
        >
          <Bell className="size-4" />
          {unreadCount > 0 && (
            <Badge
              variant="destructive"
              className="absolute -top-0.5 -right-1 h-4 min-w-4 justify-center px-1 text-[10px] font-semibold"
            >
              {unreadCount > 99 ? "99+" : unreadCount}
            </Badge>
          )}
        </Button>
      </PopoverTrigger>

      <PopoverContent align="end" className="w-96 p-0">
        <div className="flex items-center justify-between gap-2 border-b px-4 py-2.5">
          <span className="text-[13px] font-medium">
            通知
            {unreadCount > 0 && (
              <span className="text-muted-foreground"> · {unreadCount} 条未读</span>
            )}
          </span>
          <Button
            variant="ghost"
            size="sm"
            className="h-7 text-[11px]"
            disabled={!unreadCount || markAllAsRead.isPending}
            onClick={() => markAllAsRead.mutate()}
          >
            <CheckCheck />
            全部已读
          </Button>
        </div>

        {isFetching && rows.length === 0 ? (
          <div className="space-y-3 p-4">
            {[0, 1, 2].map((index) => (
              <Skeleton key={index} className="h-10 w-full" />
            ))}
          </div>
        ) : isError ? (
          <p className="text-muted-foreground px-4 py-8 text-center text-xs">
            通知加载失败，请稍后重试
          </p>
        ) : rows.length === 0 ? (
          <div className="text-muted-foreground flex flex-col items-center gap-1.5 px-4 py-8">
            <Inbox className="size-6" />
            <span className="text-xs">没有未读通知</span>
          </div>
        ) : (
          <ScrollArea className="max-h-80">
            <div>
              {rows.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => openNotification(item)}
                  className="hover:bg-sunken flex w-full flex-col gap-1 border-b px-4 py-2.5 text-left transition-colors"
                >
                  <div className="flex items-center gap-2">
                    <Badge
                      variant={TYPE_VARIANT[item.type]}
                      className="shrink-0 px-1.5 py-0 text-[10px]"
                    >
                      {item.typeLabel}
                    </Badge>
                    <span className="truncate text-[13px] font-medium">{item.title}</span>
                  </div>
                  <p className="text-muted-foreground truncate text-[11px]">{item.content}</p>
                  <span
                    className="text-muted-foreground text-[10px]"
                    title={item.createdAt}
                  >
                    {formatRelativeTime(item.createTime)}
                  </span>
                </button>
              ))}
            </div>
          </ScrollArea>
        )}

        <Button
          variant="ghost"
          size="sm"
          className="text-muted-foreground h-9 w-full justify-center rounded-none border-t text-[11px] hover:text-foreground"
          onClick={goToList}
        >
          查看全部通知
          <ArrowRight />
        </Button>
      </PopoverContent>
    </Popover>
  );
}
