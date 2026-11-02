import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({
  push: vi.fn(),
  profile: null as unknown,
  performance: null as unknown,
  performanceLoading: false,
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: h.push }) }));
vi.mock("../hooks", () => ({
  useSupplierProfile: () => ({ data: h.profile, isLoading: false }),
  useSupplierPerformance: () => ({
    data: h.performance,
    isLoading: h.performanceLoading,
  }),
  // 分类 / 联系人分区各自的 hook —— 详情页切到这两个分区时会用到。
  useSupplierAssignedCategories: () => ({ data: [] }),
  useSupplierCategories: () => ({ data: [] }),
  useAssignSupplierCategories: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useSupplierContacts: () => ({ data: [] }),
  useCreateSupplierContact: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useUpdateSupplierContact: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useSetPrimarySupplierContact: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDeleteSupplierContact: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

import SupplierDetail from "./supplier-detail";

const profile = {
  supplier: { id: "s-1", name: "深圳华强电子有限公司", code: "SUP-0001", contactPerson: "张伟", contactPhone: "138****2210", status: "active", rating: 4.6, overallScore: 92, supplierLevel: 1, auditStatus: 2, riskLevel: 1, blacklisted: false, cooperationStatus: 1, createdAt: "2021-03-02" },
  contacts: [], bankAccounts: [], certificates: [], categories: [], changeLogs: [],
};

describe("SupplierDetail", () => {
  beforeEach(() => {
    h.profile = profile;
    h.performance = { supplierId: "s-1", supplierName: "深圳华强电子有限公司", supplierLevel: 1, overallScore: 92, qualityScore: 95, deliveryScore: 91.2, serviceScore: 88.5, priceScore: 94, evaluationCount: 12, latestPeriod: "2026-09", latestTotalScore: 92.4, suggestedLevel: 1, recentEvaluations: [] };
    h.push.mockReset();
    h.performanceLoading = false;
  });

  it("shows the supplier name, code and status badges", () => {
    render(<SupplierDetail id="s-1" />);
    expect(screen.getByText("深圳华强电子有限公司")).toBeInTheDocument();
    expect(screen.getByText("SUP-0001")).toBeInTheDocument();
    expect(screen.getByText("战略")).toBeInTheDocument();
    expect(screen.getByText("已通过")).toBeInTheDocument();
  });

  it("shows the performance scorecard dimensions", () => {
    render(<SupplierDetail id="s-1" />);
    expect(screen.getByText("质量")).toBeInTheDocument();
    expect(screen.getByText("95")).toBeInTheDocument();
    expect(screen.getByText("交付")).toBeInTheDocument();
  });

  it("lists the seven sections in the left rail", () => {
    render(<SupplierDetail id="s-1" />);
    for (const label of ["概览", "分类", "联系人", "银行账户", "资质证照", "绩效评价", "变更留痕"]) {
      expect(screen.getByText(label)).toBeInTheDocument();
    }
  });

  it("switches to the placeholder of a section that is not built yet", async () => {
    render(<SupplierDetail id="s-1" />);
    await userEvent.click(screen.getByText("银行账户"));
    expect(screen.getByText("银行账户分区正在建设中。")).toBeInTheDocument();
  });

  it("shows the category and the contact panel instead of their placeholders", async () => {
    render(<SupplierDetail id="s-1" />);

    await userEvent.click(screen.getByText("分类"));
    expect(screen.getByText("供应商分类")).toBeInTheDocument();
    expect(screen.queryByText("分类分区正在建设中。")).not.toBeInTheDocument();

    await userEvent.click(screen.getByText("联系人"));
    expect(screen.getByText("供应商联系人")).toBeInTheDocument();
    expect(screen.queryByText("联系人分区正在建设中。")).not.toBeInTheDocument();
  });

  it("marks a truncated change-log count as at-least instead of a total", () => {
    h.profile = {
      ...profile,
      changeLogs: Array.from({ length: 20 }, (_, index) => ({ id: `l-${index}` })),
    };
    render(<SupplierDetail id="s-1" />);
    expect(screen.getByText("20+")).toBeInTheDocument();
  });

  it("keeps a change-log count below the profile cap as a plain number", () => {
    h.profile = {
      ...profile,
      changeLogs: Array.from({ length: 3 }, (_, index) => ({ id: `l-${index}` })),
    };
    render(<SupplierDetail id="s-1" />);
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.queryByText("3+")).not.toBeInTheDocument();
  });

  it("shows a skeleton in the scorecard while the performance query is in flight", () => {
    h.performance = undefined;
    h.performanceLoading = true;
    render(<SupplierDetail id="s-1" />);

    expect(screen.queryByText("暂无绩效数据。")).not.toBeInTheDocument();
    const scorecard = screen
      .getByText("绩效评分卡")
      .closest('[data-slot="card"]');
    expect(
      scorecard?.querySelectorAll('[data-slot="skeleton"]').length,
    ).toBeGreaterThan(0);
  });

  it("shows the empty state only after the performance query resolves without data", () => {
    h.performance = undefined;
    h.performanceLoading = false;
    render(<SupplierDetail id="s-1" />);

    expect(screen.getByText("暂无绩效数据。")).toBeInTheDocument();
  });
});
