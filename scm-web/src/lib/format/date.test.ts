import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { formatDateTime, formatRelativeTime } from "./date";

describe("formatDateTime", () => {
  it("renders a minute-precision stamp for the table and tooltips", () => {
    expect(formatDateTime("2026-10-30T14:48:00")).toBe("2026-10-30 14:48");
  });

  it("shows a dash instead of Invalid Date", () => {
    expect(formatDateTime(null)).toBe("-");
    expect(formatDateTime(undefined)).toBe("-");
  });
});

describe("formatRelativeTime", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-10-30T12:00:00"));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("speaks the notification panel's language", () => {
    expect(formatRelativeTime("2026-10-30T11:57:00")).toBe("3 分钟前");
    expect(formatRelativeTime("2026-10-30T10:00:00")).toBe("2 小时前");
  });

  it("shows a dash instead of Invalid Date", () => {
    expect(formatRelativeTime(null)).toBe("-");
  });
});
