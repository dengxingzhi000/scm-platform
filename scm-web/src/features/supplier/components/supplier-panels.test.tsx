import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({
  assign: vi.fn(),
  setPrimary: vi.fn(),
  remove: vi.fn(),
  create: vi.fn(),
  update: vi.fn(),
  setDefault: vi.fn(),
  createBankAccount: vi.fn(),
  updateBankAccount: vi.fn(),
  removeBankAccount: vi.fn(),
  verify: vi.fn(),
  createCertificate: vi.fn(),
  updateCertificate: vi.fn(),
  removeCertificate: vi.fn(),
  contacts: [] as unknown[],
  assigned: [] as unknown[],
  bankAccounts: [] as unknown[],
  certificates: [] as unknown[],
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
  useSupplierBankAccounts: () => ({ data: h.bankAccounts }),
  useCreateSupplierBankAccount: () => ({ mutateAsync: h.createBankAccount, isPending: false }),
  useUpdateSupplierBankAccount: () => ({ mutateAsync: h.updateBankAccount, isPending: false }),
  useDeleteSupplierBankAccount: () => ({ mutateAsync: h.removeBankAccount, isPending: false }),
  useSetDefaultSupplierBankAccount: () => ({ mutateAsync: h.setDefault, isPending: false }),
  useSupplierCertificates: () => ({ data: h.certificates }),
  useCreateSupplierCertificate: () => ({ mutateAsync: h.createCertificate, isPending: false }),
  useUpdateSupplierCertificate: () => ({ mutateAsync: h.updateCertificate, isPending: false }),
  useVerifySupplierCertificate: () => ({ mutateAsync: h.verify, isPending: false }),
  useDeleteSupplierCertificate: () => ({ mutateAsync: h.removeCertificate, isPending: false }),
}));

import SupplierBankAccountPanel from "./supplier-bank-account-panel";
import SupplierCategoryPanel from "./supplier-category-panel";
import SupplierCertificatePanel from "./supplier-certificate-panel";
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

/** 默认结算账户 —— 招商银行。 */
const primaryBankAccount = {
  id: "ba-1",
  supplierId: "s-1",
  bankName: "招商银行",
  bankBranch: "深圳分行科技园支行",
  bankAccount: "6222021234567890",
  accountName: "深圳华强电子有限公司",
  currency: "CNY",
  swiftCode: "CMBCCNBS",
  isDefault: true,
  enabled: true,
  sortOrder: 0,
  createTime: "2026-01-01",
  remark: null,
};

/** 非默认账户 —— 中国银行。 */
const secondaryBankAccount = {
  ...primaryBankAccount,
  id: "ba-2",
  bankName: "中国银行",
  bankBranch: "深圳分行前海支行",
  bankAccount: "6222029876543210",
  accountName: "华强电子（香港）有限公司",
  swiftCode: "BKCHCNBJ",
  isDefault: false,
};

/** 已过期的资质 —— 状态是后端判定的 3。 */
const expiredCertificate = {
  id: "cert-1",
  supplierId: "s-1",
  certificateType: "QUALITY_ISO",
  certificateName: "ISO 9001 质量体系认证",
  certificateNo: "00126Q2026R0X",
  issuingAuthority: "中国质量认证中心",
  validFrom: "2019-01-01",
  validTo: "2020-01-01",
  fileUrl: null,
  status: 3,
  verifiedBy: null,
  verifiedAt: null,
  createTime: "2026-01-01",
  remark: null,
  daysToExpiry: -10,
};

/** 待审核的资质。 */
const pendingCertificate = {
  ...expiredCertificate,
  id: "cert-2",
  certificateType: "PRODUCTION_LICENSE",
  certificateName: "食品生产许可证",
  certificateNo: "SC12345678901234",
  issuingAuthority: "广东省市场监督管理局",
  validFrom: "2026-01-01",
  validTo: "2027-01-01",
  status: 0,
  daysToExpiry: 120,
};

describe("supplier detail panels", () => {
  beforeEach(() => {
    h.assign.mockReset().mockResolvedValue(undefined);
    h.setPrimary.mockReset().mockResolvedValue(undefined);
    h.remove.mockReset().mockResolvedValue(undefined);
    h.create.mockReset().mockResolvedValue(undefined);
    h.update.mockReset().mockResolvedValue(undefined);
    h.setDefault.mockReset().mockResolvedValue(undefined);
    h.createBankAccount.mockReset().mockResolvedValue(undefined);
    h.updateBankAccount.mockReset().mockResolvedValue(undefined);
    h.removeBankAccount.mockReset().mockResolvedValue(undefined);
    h.verify.mockReset().mockResolvedValue(undefined);
    h.createCertificate.mockReset().mockResolvedValue(undefined);
    h.updateCertificate.mockReset().mockResolvedValue(undefined);
    h.removeCertificate.mockReset().mockResolvedValue(undefined);
    h.contacts = [zhangWei];
    h.assigned = [];
    h.bankAccounts = [primaryBankAccount, secondaryBankAccount];
    h.certificates = [expiredCertificate, pendingCertificate];
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

  it("masks the bank account and reveals it on demand", async () => {
    render(<SupplierBankAccountPanel supplierId="s-1" />);
    expect(await screen.findByText("**** **** **** 7890")).toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", { name: "显示深圳华强电子有限公司的账号" }),
    );
    expect(await screen.findByText("6222021234567890")).toBeInTheDocument();
    expect(screen.queryByText("**** **** **** 7890")).not.toBeInTheDocument();
  });

  it("promotes another bank account to the default one", async () => {
    render(<SupplierBankAccountPanel supplierId="s-1" />);
    await userEvent.click(
      await screen.findByRole("button", {
        name: "设为默认华强电子（香港）有限公司",
      }),
    );
    await waitFor(() =>
      expect(h.setDefault).toHaveBeenCalledWith({ supplierId: "s-1", id: "ba-2" }),
    );
  });

  it("creates a bank account from the form", async () => {
    render(<SupplierBankAccountPanel supplierId="s-1" />);
    await userEvent.click(await screen.findByRole("button", { name: "新增银行账户" }));

    await userEvent.type(await screen.findByLabelText("开户行"), "中国银行");
    await userEvent.type(await screen.findByLabelText("账号"), "6222029876543210");
    await userEvent.type(
      await screen.findByLabelText("账户名"),
      "华强电子（香港）有限公司",
    );
    await userEvent.click(screen.getByRole("button", { name: "保存" }));

    await waitFor(() =>
      expect(h.createBankAccount).toHaveBeenCalledWith({
        supplierId: "s-1",
        data: expect.objectContaining({
          bankName: "中国银行",
          bankAccount: "6222029876543210",
          accountName: "华强电子（香港）有限公司",
        }),
      }),
    );
  });

  it("deletes a bank account only after the confirmation", async () => {
    render(<SupplierBankAccountPanel supplierId="s-1" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "删除华强电子（香港）有限公司" }),
    );
    expect(h.removeBankAccount).not.toHaveBeenCalled();
    await userEvent.click(await screen.findByRole("button", { name: "删除" }));
    await waitFor(() =>
      expect(h.removeBankAccount).toHaveBeenCalledWith({
        supplierId: "s-1",
        id: "ba-2",
      }),
    );
  });

  it("shows the failure reason when the bank account is rejected", async () => {
    h.setDefault.mockRejectedValueOnce(new Error("银行账户保存失败"));
    render(<SupplierBankAccountPanel supplierId="s-1" />);
    await userEvent.click(
      await screen.findByRole("button", {
        name: "设为默认华强电子（香港）有限公司",
      }),
    );
    expect(await screen.findByText("银行账户保存失败")).toBeInTheDocument();
  });

  it("shows an expired certificate in red with a negative day count", async () => {
    render(<SupplierCertificatePanel supplierId="s-1" />);
    expect(await screen.findByText("已过期")).toBeInTheDocument();
    expect(screen.getByText("已过期")).toHaveClass("text-[var(--color-error-text)]");
    expect(screen.getByText("-10")).toBeInTheDocument();
  });

  it("takes the certificate status from the server", async () => {
    render(<SupplierCertificatePanel supplierId="s-1" />);
    expect(await screen.findByText("待审核")).toBeInTheDocument();
    expect(screen.queryByText("有效")).not.toBeInTheDocument();
  });

  it("rejects a certificate with a remark", async () => {
    render(<SupplierCertificatePanel supplierId="s-1" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "审核食品生产许可证" }),
    );

    await userEvent.click(await screen.findByRole("radio", { name: "驳回" }));
    await userEvent.type(await screen.findByLabelText("审核备注"), "材料不全");
    await userEvent.click(screen.getByRole("button", { name: "确认审核" }));

    await waitFor(() =>
      expect(h.verify).toHaveBeenCalledWith({
        supplierId: "s-1",
        id: "cert-2",
        approved: false,
        remark: "材料不全",
      }),
    );
  });

  it("creates a certificate from the form", async () => {
    render(<SupplierCertificatePanel supplierId="s-1" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "新增资质证照" }),
    );

    await userEvent.click(await screen.findByLabelText("证照类型"));
    await userEvent.click(await screen.findByRole("option", { name: "营业执照" }));
    await userEvent.type(await screen.findByLabelText("证照名称"), "食品经营许可证");
    await userEvent.type(await screen.findByLabelText("证照编号"), "JY12345678");
    await userEvent.type(
      await screen.findByLabelText("发证机关"),
      "广东省市场监督管理局",
    );
    await userEvent.type(await screen.findByLabelText("有效期起"), "2026-01-01");
    await userEvent.type(await screen.findByLabelText("有效期止"), "2027-01-01");
    await userEvent.click(screen.getByRole("button", { name: "保存" }));

    await waitFor(() =>
      expect(h.createCertificate).toHaveBeenCalledWith({
        supplierId: "s-1",
        data: {
          certificateType: "BUSINESS_LICENSE",
          certificateName: "食品经营许可证",
          certificateNo: "JY12345678",
          issuingAuthority: "广东省市场监督管理局",
          validFrom: "2026-01-01",
          validTo: "2027-01-01",
          fileUrl: null,
        },
      }),
    );
  });

  it("deletes a certificate only after the confirmation", async () => {
    render(<SupplierCertificatePanel supplierId="s-1" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "删除ISO 9001 质量体系认证" }),
    );
    expect(h.removeCertificate).not.toHaveBeenCalled();
    await userEvent.click(await screen.findByRole("button", { name: "删除" }));
    await waitFor(() =>
      expect(h.removeCertificate).toHaveBeenCalledWith({
        supplierId: "s-1",
        id: "cert-1",
      }),
    );
  });

  // 后端 update 在请求不带 status 时会按 validTo 重算（只可能给出 1/2/3），
  // 把人工的「已驳回(4)」静默改掉 —— 所以编辑时必须回显当前状态。
  it("echoes the audited status back when a certificate is edited", async () => {
    h.certificates = [{ ...pendingCertificate, status: 4 }];
    render(<SupplierCertificatePanel supplierId="s-1" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "编辑食品生产许可证" }),
    );

    const name = await screen.findByLabelText("证照名称");
    await userEvent.clear(name);
    await userEvent.type(name, "食品经营许可证");
    await userEvent.click(screen.getByRole("button", { name: "保存" }));

    await waitFor(() =>
      expect(h.updateCertificate).toHaveBeenCalledWith({
        supplierId: "s-1",
        id: "cert-2",
        data: expect.objectContaining({
          certificateName: "食品经营许可证",
          status: 4,
        }),
      }),
    );
  });

  // 新增时状态由后端按有效期推导，前端不代填。
  it("leaves the status out when a certificate is created", async () => {
    render(<SupplierCertificatePanel supplierId="s-1" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "新增资质证照" }),
    );

    await userEvent.click(await screen.findByLabelText("证照类型"));
    await userEvent.click(await screen.findByRole("option", { name: "营业执照" }));
    await userEvent.type(await screen.findByLabelText("证照名称"), "食品经营许可证");
    await userEvent.click(screen.getByRole("button", { name: "保存" }));

    await waitFor(() => expect(h.createCertificate).toHaveBeenCalled());
    const [{ data }] = h.createCertificate.mock.calls[0];
    expect(Object.keys(data)).not.toContain("status");
  });
});
