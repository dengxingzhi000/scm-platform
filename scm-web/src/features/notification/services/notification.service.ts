import { notificationApiService } from "@/lib/api/services/notification";

import {
  toNotificationPage,
} from "../lib/mappers";
import type { NotificationSearchParams } from "../types";

const typeMap: Record<string, { label: string; color: string }> = {
  system: { label: "系统", color: "blue" },
  order: { label: "订单", color: "green" },
  inventory: { label: "库存", color: "orange" },
  approval: { label: "审批", color: "purple" },
  security: { label: "安全", color: "red" },
};

const TYPE_TO_API: Record<string, string> = {
  system: "SYSTEM",
  order: "ORDER",
  inventory: "INVENTORY",
  approval: "APPROVAL",
  security: "SECURITY",
};

export const notificationService = {
  list: async (params?: NotificationSearchParams) => {
    const page = await notificationApiService.page({
      page: params?.current ?? 1,
      size: params?.pageSize ?? 20,
      type: params?.type ? TYPE_TO_API[params.type] : undefined,
      isRead: params?.isRead,
      keyword: params?.keyword || undefined,
      startTime: params?.startTime,
      endTime: params?.endTime,
    });
    return toNotificationPage(page);
  },

  getUnreadCount: (): Promise<number> => notificationApiService.unreadCount(),

  markAsRead: async (id: string): Promise<void> => {
    await notificationApiService.markAsRead(id);
  },

  markAllAsRead: async (): Promise<void> => {
    await notificationApiService.markAllAsRead();
  },

  remove: async (id: string): Promise<void> => {
    await notificationApiService.remove(id);
  },

  removeMany: async (ids: string[]): Promise<number> =>
    notificationApiService.removeMany(ids),

  getTypeMap: () => typeMap,
};
