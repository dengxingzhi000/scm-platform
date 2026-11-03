import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({
  audit: vi.fn(),
  push: vi.fn(),
  listParams: undefined as Record<string, unknown> | undefined,
  queueRows: [] as unknown[],
  expiryRows: [] as unknown[],
  days: 0,
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: h.push }),
}));

vi.mock("../hooks", () => ({
  useSupplierList: (params: Record<string, unknown>) => {
    h.listParams = params;
    return {
      data: {
        records: h.queueRows,
        total: h.queueRows.length,
        current: 1,
        pageSize: 20,
      },
      isLoading: false,
    };
  },
  useAuditSupplier: () => ({ mutateAsync: h.audit, isPending: false }),
  useExpiringCertificates: (days: number) => {
    h.days = days;
    return {
      data: {
        records: h.expiryRows,
        total: h.expiryRows.length,
        current: 1,
        pageSize: 20,
      },
      isLoading: false,
    };
  },
  useSupplierStats: () => ({
    data: {
      total: 10,
      enabled: 6,
      cooperating: 4,
      pendingAudit: 4,
      blacklisted: 0,
      expiringCertificates: 1,
      byCooperationStatus: {},
      byAuditStatus: { 1: 4, 3: 2 },
    },
  }),
}));

import SupplierAuditWorkbench from "./supplier-audit-workbench";

/** 待审核队列里的供应商 —— 税号已补全，可以提交审核。 */
const pendingSupplier: Record<string, unknown> = {
  id: "s-1",
  name: "苏州瑞丰新材料有限公司",
  code: "SUP-0003",
  contactPerson: "王强",
  contactPhone: "",
  status: "disabled",
  rating: 3.8,
  blacklisted: false,
  cooperationStatus: 0,
  taxNo: "91320500MA1XK3",
  supplierType: 1,
  creditRating: "B",
  auditStatus: 1,
  supplierLevel: 3,
  riskLevel: 1,
  createdAt: "2026-10-28",
};

/** 五年后到期的日期串，保证预警表里永远不是「已过期」。 */
const dateInDays = (days: number): string => {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
};

/** 五天后到期、尚未过期的证照 —— 后端已回填供应商名称。 */
const expiringCertificate: Record<string, unknown> = {
  id: "cert-1",
  supplierId: "s-1",
  supplierName: "苏州瑞丰新材料有限公司",
  certificateType: "QUALITY_ISO",
  certificateName: "ISO9001",
  certificateNo: "00126Q2026R0X",
  validFrom: "2025-01-01",
  validTo: dateInDays(5),
  status: 2,
  daysToExpiry: 5,
};

describe("SupplierAuditWorkbench", () => {
  beforeEach(() => {
    h.audit.mockReset().mockResolvedValue(undefined);
    h.push.mockReset();
    h.queueRows = [pendingSupplier];
    h.expiryRows = [];
  });

  it("asks for the pending audit queue", () => {
    render(<SupplierAuditWorkbench />);
    expect(h.listParams).toMatchObject({ auditStatus: 1 });
  });

  it("approves a supplier with the reviewer remark", async () => {
    render(<SupplierAuditWorkbench />);
    await userEvent.click(await screen.findByRole("button", { name: "通过" }));
    expect(h.audit).toHaveBeenCalledWith({
      id: "s-1",
      action: "APPROVE",
      remark: undefined,
    });
  });

  it("never offers a certificate that already expired", () => {
    h.expiryRows = [expiringCertificate];
    render(<SupplierAuditWorkbench />);
    expect(screen.getByText("ISO9001")).toBeInTheDocument();
    expect(screen.queryByText("已过期")).not.toBeInTheDocument();
  });

  it("switches to the rejected queue from the page header", async () => {
    render(<SupplierAuditWorkbench />);
    await userEvent.click(screen.getByRole("button", { name: /已驳回/ }));
    expect(h.listParams).toMatchObject({ auditStatus: 3, current: 1 });
    expect(
      screen.getByRole("button", { name: "查看供应商" }),
    ).toBeInTheDocument();
  });

  it("keeps approve disabled when the supplier has no tax number", () => {
    h.queueRows = [{ ...pendingSupplier, taxNo: "" }];
    render(<SupplierAuditWorkbench />);
    expect(screen.getByRole("button", { name: "通过" })).toBeDisabled();
  });

  it("rejects a supplier only with a non-empty remark", async () => {
    render(<SupplierAuditWorkbench />);
    await userEvent.click(screen.getByRole("button", { name: "驳回" }));

    const submit = await screen.findByRole("button", { name: "确认驳回" });
    expect(submit).toBeDisabled();

    await userEvent.type(screen.getByLabelText("审核意见"), "材料不全");
    await userEvent.click(submit);

    await waitFor(() =>
      expect(h.audit).toHaveBeenCalledWith({
        id: "s-1",
        action: "REJECT",
        remark: "材料不全",
      }),
    );
  });
});
