import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({ update: vi.fn(), create: vi.fn() }));

vi.mock("../hooks", () => ({
  useSupplierCategories: () => ({ data: [
    { id: "c-1", categoryCode: "RAW", categoryName: "原材料", parentId: null, level: 1, enabled: true, sortOrder: 10, createTime: null, remark: null },
    { id: "c-2", categoryCode: "RAW_ELEC", categoryName: "电子元器件", parentId: "c-1", level: 2, enabled: true, sortOrder: 11, createTime: null, remark: null },
  ] }),
  useCreateSupplierCategory: () => ({ mutateAsync: h.create, isPending: false }),
  useUpdateSupplierCategory: () => ({ mutateAsync: h.update, isPending: false }),
}));

import SupplierCategoryManage, { buildCategoryTree } from "./supplier-category-manage";

describe("SupplierCategoryManage", () => {
  beforeEach(() => { h.update.mockReset().mockResolvedValue(undefined); h.create.mockReset().mockResolvedValue({}); });

  it("nests a child category under its parent", () => {
    const tree = buildCategoryTree([
      { id: "c-1", categoryCode: "RAW", categoryName: "原材料", parentId: null, level: 1, enabled: true, sortOrder: 10, createTime: null, remark: null },
      { id: "c-2", categoryCode: "RAW_ELEC", categoryName: "电子元器件", parentId: "c-1", level: 2, enabled: true, sortOrder: 11, createTime: null, remark: null },
    ]);
    expect(tree).toHaveLength(1);
    expect(tree[0].children?.[0].categoryName).toBe("电子元器件");
  });

  it("resends the code and the name when a category is toggled", async () => {
    render(<SupplierCategoryManage />);
    await userEvent.click(await screen.findByRole("checkbox", { name: "原材料" }));
    await waitFor(() => expect(h.update).toHaveBeenCalled());
    const payload = h.update.mock.calls[0][0];
    expect(payload.data).toMatchObject({ categoryCode: "RAW", categoryName: "原材料", enabled: false });
  });

  it("does not offer a delete button", () => {
    render(<SupplierCategoryManage />);
    expect(screen.queryByRole("button", { name: "删除" })).not.toBeInTheDocument();
  });

  it("keeps submit disabled until the code and the name are filled", async () => {
    render(<SupplierCategoryManage />);
    await userEvent.click(screen.getByRole("button", { name: "新增分类" }));
    expect(await screen.findByRole("button", { name: "保存" })).toBeDisabled();
  });

  it("creates a category from the dialog with the required payload", async () => {
    render(<SupplierCategoryManage />);
    await userEvent.click(screen.getByRole("button", { name: "新增分类" }));
    await userEvent.type(await screen.findByLabelText("分类编码"), "SVC");
    await userEvent.type(screen.getByLabelText("分类名称"), "服务类");
    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    await waitFor(() =>
      expect(h.create).toHaveBeenCalledWith({
        data: {
          categoryCode: "SVC",
          categoryName: "服务类",
          parentId: null,
          sortOrder: 0,
          enabled: true,
        },
      }),
    );
  });

  it("does not offer the edited category or its descendants as a parent", async () => {
    render(<SupplierCategoryManage />);
    await userEvent.click(screen.getByRole("button", { name: "编辑原材料" }));
    await userEvent.click(await screen.findByLabelText("上级分类"));
    expect(
      await screen.findByRole("option", { name: "无（作为一级分类）" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: /原材料/ }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: /电子元器件/ }),
    ).not.toBeInTheDocument();
  });
});
