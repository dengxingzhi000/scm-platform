import type { VariantProps } from "class-variance-authority";

import { badgeVariants } from "@/components/ui/badge";
import { formatDateTime } from "@/lib/format/date";

type BadgeVariant = NonNullable<VariantProps<typeof badgeVariants>["variant"]>;

import type {
  NotificationTypeCode,
  UserNotificationDTO,
  UserNotificationPage,
} from "@/lib/api/contracts/notification";
import type { Notification } from "../types";

const TYPE_TO_UI: Record<
  NotificationTypeCode,
  { type: Notification["type"]; label: string }
> = {
  SYSTEM: { type: "system", label: "系统" },
  ORDER: { type: "order", label: "订单" },
  INVENTORY: { type: "inventory", label: "库存" },
  APPROVAL: { type: "approval", label: "审批" },
  SECURITY: { type: "security", label: "安全" },
};

/** Shared by the bell panel and the notification table so a type always reads the same colour. */
export const TYPE_VARIANT: Record<Notification["type"], BadgeVariant> = {
  system: "secondary",
  order: "info",
  inventory: "warning",
  approval: "default",
  security: "destructive",
};

export const toNotification = (dto: UserNotificationDTO): Notification => {
  const mapped = TYPE_TO_UI[dto.type as NotificationTypeCode] ?? TYPE_TO_UI.SYSTEM;
  return {
    id: dto.id,
    title: dto.title ?? "-",
    content: dto.content ?? "",
    type: mapped.type,
    typeLabel: mapped.label,
    isRead: dto.isRead === true,
    link: dto.link ?? undefined,
    createdAt: formatDateTime(dto.createTime),
    createTime: dto.createTime ?? null,
  };
};

export const toNotificationPage = (
  page: UserNotificationPage | null | undefined
): { records: Notification[]; total: number; current: number; pageSize: number } => ({
  records: (page?.records ?? []).map(toNotification),
  total: page?.total ?? 0,
  current: page?.current ?? 1,
  pageSize: page?.size ?? 0,
});
