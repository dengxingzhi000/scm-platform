import { describe, expect, it } from "vitest";

import type { UserNotificationDTO } from "@/lib/api/contracts/notification";
import { TYPE_VARIANT, toNotification, toNotificationPage } from "./mappers";

const fixture: UserNotificationDTO = {
  id: "019a0aee-0000-7000-8000-000000000d01",
  userId: "019a0aee-0000-7000-8000-000000000001",
  title: "库存缺货提醒",
  content: "华为 Mate 60 Pro 256G 可用库存为 0，请及时补货",
  type: "INVENTORY",
  isRead: false,
  link: "/inventory/alerts",
  readAt: null,
  createTime: "2026-10-30T14:48:00",
  updateTime: "2026-10-30T14:48:00",
};

describe("toNotification", () => {
  it("maps the backend type code to the UI type, label and badge colour", () => {
    const ui = toNotification(fixture);

    expect(ui.type).toBe("inventory");
    expect(ui.typeLabel).toBe("库存");
    expect(TYPE_VARIANT[ui.type]).toBe("warning");
    expect(ui.link).toBe("/inventory/alerts");
  });

  it("keeps the raw timestamp so the panel can render relative time", () => {
    const ui = toNotification(fixture);

    expect(ui.createTime).toBe("2026-10-30T14:48:00");
    expect(ui.createdAt).toBe("2026-10-30 14:48");
  });

  it.each([
    ["SYSTEM", "system"],
    ["ORDER", "order"],
    ["APPROVAL", "approval"],
    ["SECURITY", "security"],
  ] as const)("maps %s to the %s bucket", (code, expected) => {
    expect(toNotification({ ...fixture, type: code }).type).toBe(expected);
  });

  it("falls back to the system bucket for an unknown code", () => {
    const ui = toNotification({ ...fixture, type: "MARKETING" });

    expect(ui.type).toBe("system");
    expect(ui.typeLabel).toBe("系统");
  });

  it("treats a null flag as unread so the badge never under-counts", () => {
    expect(toNotification({ ...fixture, isRead: null }).isRead).toBe(false);
    expect(toNotification({ ...fixture, isRead: true }).isRead).toBe(true);
  });

  it("substitutes placeholders for the fields the table renders", () => {
    const ui = toNotification({
      id: "n-1",
      title: null,
      content: null,
      type: null,
      link: null,
      createTime: null,
    });

    expect(ui.title).toBe("-");
    expect(ui.content).toBe("");
    expect(ui.link).toBeUndefined();
    expect(ui.createdAt).toBe("-");
  });
});

describe("toNotificationPage", () => {
  it("normalises a MyBatis-Plus page echo", () => {
    const page = toNotificationPage({
      records: [fixture],
      total: 42,
      current: 3,
      size: 20,
    } as never);

    expect(page.records).toHaveLength(1);
    expect(page).toMatchObject({ total: 42, current: 3, pageSize: 20 });
  });

  it("returns an empty page when the envelope carries no data", () => {
    expect(toNotificationPage(undefined)).toEqual({
      records: [],
      total: 0,
      current: 1,
      pageSize: 0,
    });
  });
});
