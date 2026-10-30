import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  UserNotificationPage,
  UserNotificationQuery,
} from "../contracts/notification";

const NOTIFICATION_BASE = "/scm-notify/sys-user-notification";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const put = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.put(url, undefined, { params }));

export const notificationApiService = {
  page: (query: UserNotificationQuery) =>
    get<UserNotificationPage>(`${NOTIFICATION_BASE}/page`, query),
  unreadCount: () => get<number>(`${NOTIFICATION_BASE}/unread-count`),
  markAsRead: (id: string) =>
    put<boolean>(`${NOTIFICATION_BASE}/${id}/read`),
  markAllAsRead: () => put<boolean>(`${NOTIFICATION_BASE}/read-all`),
};
