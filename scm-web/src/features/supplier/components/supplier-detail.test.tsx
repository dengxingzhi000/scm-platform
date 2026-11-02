import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({
  push: vi.fn(),
  profile: null as unknown,
  performance: null as unknown,
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: h.push }) }));
vi.mock("../hooks", () => ({
  useSupplierProfile: () => ({ data: h.profile, isLoading: false }),
  useSupplierPerformance: () => ({ data: h.performance, isLoading: false }),
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
    await userEvent.click(screen.getByText("联系人"));
    expect(screen.getByText("联系人分区正在建设中。")).toBeInTheDocument();
  });
});
