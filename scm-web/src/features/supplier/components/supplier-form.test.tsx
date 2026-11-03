import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({
  push: vi.fn(),
  create: vi.fn(),
  update: vi.fn(),
  detail: undefined as Record<string, unknown> | undefined,
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: h.push }) }));
vi.mock("../hooks", () => ({
  useSupplierFormDetail: () => ({ data: h.detail, isLoading: false, isError: false }),
  useCreateSupplier: () => ({ mutateAsync: h.create, isPending: false }),
  useUpdateSupplier: () => ({ mutateAsync: h.update, isPending: false }),
}));

import SupplierForm from "./supplier-form";

describe("SupplierForm", () => {
  beforeEach(() => {
    h.push.mockReset();
    h.detail = undefined;
    h.create.mockReset().mockResolvedValue({ id: "s-1" });
    h.update.mockReset().mockResolvedValue({ id: "s-1" });
  });

  it("requires a supplier name before submitting", async () => {
    render(<SupplierForm />);
    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    expect(await screen.findByText("供应商名称不能为空")).toBeInTheDocument();
    expect(h.create).not.toHaveBeenCalled();
  });

  it("submits the payload built from the form values", async () => {
    render(<SupplierForm />);
    await userEvent.type(screen.getByLabelText("供应商名称"), "深圳华强电子有限公司");
    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    await waitFor(() => expect(h.create).toHaveBeenCalled());
    expect(h.create.mock.calls[0][0]).toMatchObject({ supplierName: "深圳华强电子有限公司" });
  });

  it("shows the backend reason when the save is rejected", async () => {
    h.create.mockRejectedValue({ response: { status: 400, data: { code: 400, message: "Service error: 供应商编码已存在 (traceId=t1)" } } });
    render(<SupplierForm />);
    await userEvent.type(screen.getByLabelText("供应商名称"), "重复编码公司");
    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    expect(await screen.findByText(/供应商编码已存在/)).toBeInTheDocument();
  });

  it("loads an existing supplier into the edit form and submits an update", async () => {
    h.detail = {
      id: "s-1",
      supplierCode: "SUP-0001",
      supplierName: "深圳华强电子有限公司",
      creditRating: "A",
      registeredCapital: 5000000,
    };
    render(<SupplierForm id="s-1" />);

    expect(await screen.findByDisplayValue("深圳华强电子有限公司")).toBeInTheDocument();
    expect(screen.getByLabelText("供应商编码")).toBeDisabled();

    await userEvent.clear(screen.getByLabelText("供应商名称"));
    await userEvent.type(screen.getByLabelText("供应商名称"), "华强电子（深圳）");
    await userEvent.click(screen.getByRole("button", { name: "保存" }));

    await waitFor(() =>
      expect(h.update).toHaveBeenCalledWith({
        id: "s-1",
        data: expect.objectContaining({ supplierName: "华强电子（深圳）" }),
      })
    );
    expect(h.create).not.toHaveBeenCalled();
  });
});
