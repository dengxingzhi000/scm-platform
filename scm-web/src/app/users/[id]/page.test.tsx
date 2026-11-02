import { render, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({
  get: vi.fn(),
  id: "u-1",
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
  useParams: () => ({ id: h.id }),
}));

vi.mock("@/lib/api/client", () => ({
  default: {
    get: h.get,
    post: vi.fn(),
    put: vi.fn(),
  },
}));

import UserFormPage from "./page";

describe("UserFormPage", () => {
  beforeEach(() => {
    // Ant Design 的响应式栅格在渲染时读 matchMedia，jsdom 没有实现。
    Object.defineProperty(window, "matchMedia", {
      configurable: true,
      value: (query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }),
    });
    h.get.mockReset().mockResolvedValue({ data: { data: { username: "zhangsan" } } });
    h.id = "u-1";
  });

  it("loads the user once for the edit route", async () => {
    render(<UserFormPage />);

    await waitFor(() => expect(h.get).toHaveBeenCalledTimes(1));
    expect(h.get).toHaveBeenCalledWith("/users/u-1");
  });

  it("does not load a user on the add route", () => {
    h.id = "add";
    render(<UserFormPage />);

    expect(h.get).not.toHaveBeenCalled();
  });
});
