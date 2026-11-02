import { act, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import OfflineBanner from "./offline-banner";

const OFFLINE_TEXT = "网络连接已断开，请检查您的网络设置";

const setOnline = (online: boolean) => {
  Object.defineProperty(window.navigator, "onLine", {
    configurable: true,
    get: () => online,
  });
  act(() => {
    window.dispatchEvent(new Event(online ? "online" : "offline"));
  });
};

describe("OfflineBanner", () => {
  beforeEach(() => {
    setOnline(true);
  });

  it("shows the banner only while the browser reports offline", () => {
    render(<OfflineBanner />);
    expect(screen.queryByText(OFFLINE_TEXT)).toBeNull();

    setOnline(false);
    expect(screen.getByText(OFFLINE_TEXT)).toBeInTheDocument();

    setOnline(true);
    expect(screen.queryByText(OFFLINE_TEXT)).toBeNull();
  });
});
