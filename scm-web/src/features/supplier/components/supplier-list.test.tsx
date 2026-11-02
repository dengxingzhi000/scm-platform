import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({
  push: vi.fn(),
  replace: vi.fn(),
  searchParams: new URLSearchParams() as URLSearchParams,
  listParams: undefined as Record<string, unknown> | undefined,
  rows: [] as unknown[],
  stats: { total: 128, enabled: 96, cooperating: 74, pendingAudit: 9, blacklisted: 3, expiringCertificates: 5, byCooperationStatus: {}, byAuditStatus: {} },
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: h.push, replace: h.replace }),
  useSearchParams: () => h.searchParams,
}));
vi.mock("../hooks", () => ({
  useSupplierList: (params: Record<string, unknown>) => { h.listParams = params; return { data: { records: h.rows, total: 1, current: 1, pageSize: 20 }, isLoading: false }; },
  useSupplierStats: () => ({ data: h.stats }),
  useSupplierCategories: () => ({ data: [] }),
  useSupplierSearch: () => ({ data: [] }),
  useAuditSupplier: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useBlacklistSupplier: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useEnableSupplier: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDisableSupplier: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useSubmitSupplierAudit: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useChangeSupplierRiskLevel: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useChangeCooperationStatus: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useUnblacklistSupplier: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDeleteSupplier: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

import SupplierList from "./supplier-list";

const supplier = {
  id: "s-1", name: "深圳华强电子有限公司", code: "SUP-0001",
  contactPerson: "张伟", contactPhone: "138****2210", email: undefined, address: undefined,
  status: "active", rating: 4.6, overallScore: 92, supplierLevel: 1, auditStatus: 2,
  riskLevel: 1, blacklisted: false, cooperationStatus: 1, createdAt: "2021-03-02",
};

describe("SupplierList", () => {
  beforeEach(() => { h.rows = [supplier]; h.push.mockReset(); });

  it("shows the six supplier statistics", () => {
    render(<SupplierList />);
    expect(screen.getByText("128")).toBeInTheDocument();
    expect(screen.getByText("待审核")).toBeInTheDocument();
    expect(screen.getByText("9")).toBeInTheDocument();
  });

  it("passes the search keyword to the list query", async () => {
    render(<SupplierList />);
    await userEvent.type(screen.getByPlaceholderText("搜索供应商名称 / 编码"), "华强");
    await userEvent.click(screen.getByRole("button", { name: "搜索" }));
    await waitFor(() => expect(h.listParams).toMatchObject({ keyword: "华强" }));
  });

  it("navigates to the supplier detail when the name is clicked", async () => {
    render(<SupplierList />);
    await userEvent.click(screen.getByText("深圳华强电子有限公司"));
    expect(h.push).toHaveBeenCalledWith("/supplier/s-1");
  });

  it("greys out delete for a supplier that is still cooperating", async () => {
    render(<SupplierList />);
    await userEvent.click(screen.getByRole("button", { name: "更多操作" }));
    const del = await screen.findByRole("menuitem", { name: "删除" });
    expect(del).toHaveAttribute("data-disabled");
  });
});
