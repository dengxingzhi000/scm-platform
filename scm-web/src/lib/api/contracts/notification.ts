import type { MpPage } from "./page";

export type NotificationTypeCode =
  | "SYSTEM"
  | "ORDER"
  | "INVENTORY"
  | "APPROVAL"
  | "SECURITY";

/** `com.cognora.platform.notify.domain.entity.SysUserNotification`. */
export interface UserNotificationDTO {
  id: string;
  userId?: string | null;
  title?: string | null;
  content?: string | null;
  type?: NotificationTypeCode | string | null;
  isRead?: boolean | null;
  link?: string | null;
  readAt?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export type UserNotificationPage = MpPage<UserNotificationDTO>;

export interface UserNotificationQuery {
  page: number;
  size: number;
  userId?: string;
  type?: string;
  isRead?: boolean;
  /** Matches the title or the content. */
  keyword?: string;
  /** ISO-8601 local date-time, inclusive. */
  startTime?: string;
  endTime?: string;
}
