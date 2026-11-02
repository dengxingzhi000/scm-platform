import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { Notification } from "../types";
import NotificationBell from "./notification-bell";

const h = vi.hoisted(() => ({
  unread: 3,
  records: [] as Notification[],
  listParams: undefined as { isRead?: boolean; pageSize?: number } | undefined,
  push: vi.fn(),
  markAsRead: vi.fn(),
  markAllAsRead: vi.fn(),
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: h.push }) }));

// The panel is a cache-key consumer, not a producer: stub the hooks so the test only
// judges what the bell asks for and how it renders the answer.
vi.mock("../hooks", () => ({
  useUnreadCount: () => ({ data: h.unread }),
  useNotificationList: (
    params: { isRead?: boolean; pageSize?: number },
    options: { enabled?: boolean },
  ) => {
    h.listParams = { ...params, ...options };
    return { data: { records: h.records }, isFetching: false, isError: false };
  },
  useMarkAsRead: () => ({ mutate: h.markAsRead }),
  useMarkAllAsRead: () => ({ mutate: h.markAllAsRead, isPending: false }),
}));

const notification = (overrides: Partial<Notification> = {}): Notification => ({
  id: "n-1",
  title: "库存缺货提醒",
  content: "华为 Mate 60 Pro 256G 可用库存为 0，请及时补货",
  type: "inventory",
  typeLabel: "库存",
  isRead: false,
  link: "/inventory/alerts",
  createdAt: "2026-10-30 14:48",
  createTime: "2026-10-30T14:48:00",
  ...overrides,
});

const openPanel = async (trigger = "通知，3 条未读") => {
  render(<NotificationBell />);
  await userEvent.click(screen.getByRole("button", { name: trigger }));
};

describe("NotificationBell", () => {
  beforeEach(() => {
    h.unread = 3;
    h.records = [notification()];
    h.listParams = undefined;
    h.push.mockReset();
    h.markAsRead.mockReset();
    h.markAllAsRead.mockReset();
  });

  it("asks for unread rows so the list matches the badge count", async () => {
    await openPanel();

    expect(await screen.findByText("库存缺货提醒")).toBeInTheDocument();
    expect(h.listParams).toMatchObject({ isRead: false, enabled: true });
  });

  it("shows the type, summary and timestamp of each unread notification", async () => {
    h.records = [
      notification(),
      notification({
        id: "n-2",
        title: "订单待发货",
        content: "订单 ORD-2026-1023 已支付，等待仓库发货",
        type: "order",
        typeLabel: "订单",
        link: "/order",
      }),
    ];
    await openPanel();

    expect(await screen.findByText("库存缺货提醒")).toBeInTheDocument();
    expect(screen.getByText("订单待发货")).toBeInTheDocument();
    expect(screen.getByText("库存")).toBeInTheDocument();
    expect(screen.getByText("订单")).toBeInTheDocument();
    expect(
      screen.getByText("华为 Mate 60 Pro 256G 可用库存为 0，请及时补货"),
    ).toBeInTheDocument();
    expect(screen.getAllByTitle("2026-10-30 14:48")).toHaveLength(2);
  });

  it("marks read and jumps when a notification carries a link", async () => {
    await openPanel();
    await userEvent.click(await screen.findByText("库存缺货提醒"));

    expect(h.markAsRead).toHaveBeenCalledWith("n-1");
    expect(h.push).toHaveBeenCalledWith("/inventory/alerts");
  });

  it("marks read without navigating for a notification with no target page", async () => {
    h.records = [notification({ link: undefined })];
    await openPanel();
    await userEvent.click(await screen.findByText("库存缺货提醒"));

    expect(h.markAsRead).toHaveBeenCalledWith("n-1");
    expect(h.push).not.toHaveBeenCalled();
  });

  it("reports an empty inbox instead of a stale list", async () => {
    h.unread = 0;
    h.records = [];
    await openPanel("通知，0 条未读");

    expect(await screen.findByText("没有未读通知")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "全部已读" })).toBeDisabled();
  });

  it("links to the full notification centre", async () => {
    await openPanel();
    await userEvent.click(await screen.findByText("查看全部通知"));

    await waitFor(() => expect(h.push).toHaveBeenCalledWith("/notification"));
  });
});
