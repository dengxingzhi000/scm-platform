import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({
  assign: vi.fn(),
  setPrimary: vi.fn(),
  remove: vi.fn(),
  create: vi.fn(),
  update: vi.fn(),
  contacts: [] as unknown[],
  assigned: [] as unknown[],
}));

vi.mock("../hooks", () => ({
  useSupplierCategories: () => ({ data: [
    { id: "c-1", categoryCode: "RAW", categoryName: "原材料", level: 1, enabled: true, sortOrder: 10, createTime: null, remark: null },
    { id: "c-2", categoryCode: "RAW_ELEC", categoryName: "电子元器件", level: 2, enabled: true, sortOrder: 11, createTime: null, remark: null },
  ] }),
  useSupplierAssignedCategories: () => ({ data: h.assigned }),
  useAssignSupplierCategories: () => ({ mutateAsync: h.assign, isPending: false }),
  useSupplierContacts: () => ({ data: h.contacts }),
  useCreateSupplierContact: () => ({ mutateAsync: h.create, isPending: false }),
  useUpdateSupplierContact: () => ({ mutateAsync: h.update, isPending: false }),
  useSetPrimarySupplierContact: () => ({ mutateAsync: h.setPrimary, isPending: false }),
  useDeleteSupplierContact: () => ({ mutateAsync: h.remove, isPending: false }),
}));

import SupplierCategoryPanel from "./supplier-category-panel";
import SupplierContactPanel from "./supplier-contact-panel";

/** 分类字典里的「原材料」。 */
const rawCategory = {
  id: "c-1",
  categoryCode: "RAW",
  categoryName: "原材料",
  level: 1,
  enabled: true,
  sortOrder: 10,
  createTime: null,
  remark: null,
};

/** 主联系人 —— 张伟。 */
const zhangWei = {
  id: "ct-1",
  supplierId: "s-1",
  contactName: "张伟",
  contactTitle: "销售经理",
  contactRole: "SALES",
  contactPhone: "13800002210",
  contactEmail: "z@example.com",
  contactAddress: null,
  isPrimary: true,
  enabled: true,
  sortOrder: 0,
  createTime: "2026-01-01",
  remark: null,
};

/** 非主联系人 —— 李娜。 */
const liNa = {
  ...zhangWei,
  id: "ct-2",
  contactName: "李娜",
  contactTitle: "财务主管",
  contactRole: "FINANCE",
  isPrimary: false,
};

describe("supplier detail panels", () => {
  beforeEach(() => {
    h.assign.mockReset().mockResolvedValue(undefined);
    h.setPrimary.mockReset().mockResolvedValue(undefined);
    h.remove.mockReset().mockResolvedValue(undefined);
    h.create.mockReset().mockResolvedValue(undefined);
    h.update.mockReset().mockResolvedValue(undefined);
    h.contacts = [zhangWei];
    h.assigned = [];
  });

  it("sends the selected category ids as an array when the assignment is saved", async () => {
    render(<SupplierCategoryPanel supplierId="s-1" />);
    await userEvent.click(screen.getByRole("button", { name: "设置分类" }));
    await userEvent.click(await screen.findByRole("checkbox", { name: "原材料" }));
    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    await waitFor(() => expect(h.assign).toHaveBeenCalledWith({ id: "s-1", categoryIds: ["c-1"] }));
  });

  it("marks the primary contact and offers to change it", async () => {
    render(<SupplierContactPanel supplierId="s-1" />);
    expect(await screen.findByText("张伟")).toBeInTheDocument();
    expect(screen.getByText("主联系人")).toBeInTheDocument();
    expect(screen.getByText("销售")).toBeInTheDocument();
  });

  it("pre-checks the categories the supplier is already assigned", async () => {
    h.assigned = [rawCategory];
    render(<SupplierCategoryPanel supplierId="s-1" />);
    await userEvent.click(screen.getByRole("button", { name: "设置分类" }));
    expect(await screen.findByRole("checkbox", { name: "原材料" })).toBeChecked();
  });

  it("removes an association when the user unchecks it", async () => {
    h.assigned = [rawCategory];
    render(<SupplierCategoryPanel supplierId="s-1" />);
    await userEvent.click(screen.getByRole("button", { name: "设置分类" }));

    const checkbox = await screen.findByRole("checkbox", { name: "原材料" });
    expect(checkbox).toBeChecked();
    await userEvent.click(checkbox);

    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    await waitFor(() =>
      expect(h.assign).toHaveBeenCalledWith({ id: "s-1", categoryIds: [] }),
    );
  });

  it("shows the failure reason when the assignment is rejected", async () => {
    h.assign.mockRejectedValueOnce(new Error("分类保存失败"));
    render(<SupplierCategoryPanel supplierId="s-1" />);
    await userEvent.click(screen.getByRole("button", { name: "设置分类" }));
    await userEvent.click(await screen.findByRole("checkbox", { name: "原材料" }));
    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    expect(await screen.findByText("分类保存失败")).toBeInTheDocument();
  });

  it("promotes another contact to the primary one", async () => {
    h.contacts = [zhangWei, liNa];
    render(<SupplierContactPanel supplierId="s-1" />);
    await userEvent.click(await screen.findByRole("button", { name: "将李娜设为主联系人" }));
    await waitFor(() => expect(h.setPrimary).toHaveBeenCalledWith({ supplierId: "s-1", id: "ct-2" }));
  });

  it("deletes a contact only after the confirmation", async () => {
    render(<SupplierContactPanel supplierId="s-1" />);
    await userEvent.click(await screen.findByRole("button", { name: "删除张伟" }));
    expect(h.remove).not.toHaveBeenCalled();
    await userEvent.click(await screen.findByRole("button", { name: "删除" }));
    await waitFor(() => expect(h.remove).toHaveBeenCalledWith({ supplierId: "s-1", id: "ct-1" }));
  });

  // 「未指定」是角色下拉的默认值，提交时要落成 null。
  it("creates a contact from the form", async () => {
    render(<SupplierContactPanel supplierId="s-1" />);
    await userEvent.click(await screen.findByRole("button", { name: "新增联系人" }));
    await userEvent.type(await screen.findByLabelText("姓名"), "李娜");
    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    await waitFor(() =>
      expect(h.create).toHaveBeenCalledWith({
        supplierId: "s-1",
        data: expect.objectContaining({ contactName: "李娜", contactRole: null }),
      }),
    );
  });

  it("edits a contact through the form", async () => {
    render(<SupplierContactPanel supplierId="s-1" />);
    await userEvent.click(await screen.findByRole("button", { name: "编辑张伟" }));

    const phone = await screen.findByLabelText("电话");
    expect(phone).toHaveValue("13800002210");
    await userEvent.clear(phone);
    await userEvent.type(phone, "13900002211");

    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    await waitFor(() =>
      expect(h.update).toHaveBeenCalledWith({
        supplierId: "s-1",
        id: "ct-1",
        data: expect.objectContaining({ contactPhone: "13900002211" }),
      }),
    );
  });

  it("picks the contact role from the dropdown", async () => {
    render(<SupplierContactPanel supplierId="s-1" />);
    await userEvent.click(await screen.findByRole("button", { name: "新增联系人" }));

    await userEvent.click(await screen.findByLabelText("角色"));
    await userEvent.click(await screen.findByRole("option", { name: "财务" }));

    await userEvent.type(await screen.findByLabelText("姓名"), "李娜");
    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    await waitFor(() =>
      expect(h.create).toHaveBeenCalledWith({
        supplierId: "s-1",
        data: expect.objectContaining({ contactName: "李娜", contactRole: "FINANCE" }),
      }),
    );
  });
});
