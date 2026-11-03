import { describe, expect, it } from "vitest";

import type { SupplierDTO } from "@/lib/api/contracts/supplier";
import { AUDIT_STATUSES, labelOf } from "./constants";
import {
  daysToExpiryOf,
  maskBankAccount,
  toSupplier,
  toSupplierChangeLog,
  toSupplierPage,
} from "./mappers";

const supplierFixture: SupplierDTO = {
  id: "sup-1",
  supplierCode: "SUP-001",
  supplierName: "Apple供应链",
  contactName: "张经理",
  contactPhone: "13800000001",
  contactEmail: "zhang@example.com",
  contactAddress: "上海市浦东新区",
  qualityScore: 96,
  deliveryScore: 94,
  serviceScore: 98,
  enabled: true,
  createTime: "2025-01-01T09:00:00",
};

describe("supplier mappers", () => {
  it("folds the three score fields into a 5-star rating", () => {
    const supplier = toSupplier(supplierFixture);

    expect(supplier.rating).toBe(4.8);
    expect(supplier.status).toBe("active");
    expect(supplier.code).toBe("SUP-001");
    expect(supplier.createdAt).toBe("2025-01-01");
  });

  it("maps disabled suppliers and missing scores", () => {
    const supplier = toSupplier({
      ...supplierFixture,
      enabled: false,
      qualityScore: null,
      deliveryScore: null,
      serviceScore: null,
    });

    expect(supplier.status).toBe("disabled");
    expect(supplier.rating).toBe(0);
  });

  it("translates the MyBatis-Plus page echo into UI paging", () => {
    const page = {
      records: [supplierFixture],
      total: 8,
      size: 20,
      current: 1,
    };

    expect(toSupplierPage(page)).toMatchObject({
      total: 8,
      current: 1,
      pageSize: 20,
    });
    expect(toSupplierPage(page).records).toHaveLength(1);
  });

  it("prefers overallScore over the three-dimension average for the star rating", () => {
    const supplier = toSupplier({
      ...supplierFixture,
      overallScore: 92,
      qualityScore: 10,
      deliveryScore: 10,
      serviceScore: 10,
    });
    expect(supplier.rating).toBe(4.6); // 92 / 20
    expect(supplier.overallScore).toBe(92);
  });

  it("falls back to the three-dimension average when overallScore is missing", () => {
    const supplier = toSupplier({ ...supplierFixture, overallScore: null });
    expect(supplier.rating).toBe(4.8); // 96/94/98 平均 96 → 96/20 = 4.8
  });

  it("defaults the risk and blacklist state when the backend omits them", () => {
    const supplier = toSupplier({
      ...supplierFixture,
      riskLevel: null,
      blacklisted: null,
    });
    expect(supplier.blacklisted).toBe(false);
    expect(supplier.riskLevel).toBeUndefined();
  });

  it("carries the master-data fields the overview panel shows", () => {
    const supplier = toSupplier({
      ...supplierFixture,
      supplierNameEn: "Shenzhen Huaqiang Electronics Co., Ltd.",
      businessType: "电子元器件",
      legalPerson: "张伟",
      registeredCapital: 5000000,
      registrationNo: "91440300MA5EXAMPLE",
      establishmentDate: "2011-05-06",
      cooperationStartDate: "2021-03-02",
      paymentTerms: "月结 30 天",
      paymentMethod: "银行转账",
      website: "https://example.com",
      sortOrder: 3,
    });

    expect(supplier.nameEn).toBe("Shenzhen Huaqiang Electronics Co., Ltd.");
    expect(supplier.businessType).toBe("电子元器件");
    expect(supplier.legalPerson).toBe("张伟");
    expect(supplier.registeredCapital).toBe(5000000);
    expect(supplier.registrationNo).toBe("91440300MA5EXAMPLE");
    expect(supplier.establishmentDate).toBe("2011-05-06");
    expect(supplier.cooperationStartDate).toBe("2021-03-02");
    expect(supplier.paymentTerms).toBe("月结 30 天");
    expect(supplier.paymentMethod).toBe("银行转账");
    expect(supplier.website).toBe("https://example.com");
    expect(supplier.sortOrder).toBe(3);
  });

  it("masks a bank account but keeps the last four digits", () => {
    expect(maskBankAccount("6222021234567890")).toBe("**** **** **** 7890");
    expect(maskBankAccount("123")).toBe("****");
    expect(maskBankAccount(undefined)).toBe("-");
  });

  it("counts the days until a certificate expires, negative once expired", () => {
    expect(daysToExpiryOf("2026-11-30", new Date(2026, 9, 30))).toBe(31);
    expect(daysToExpiryOf("2026-10-20", new Date(2026, 9, 30))).toBe(-10);
    expect(daysToExpiryOf(null, new Date(2026, 9, 30))).toBeNull();
  });

  it("labels a change log with no operator as the system", () => {
    const log = toSupplierChangeLog({
      id: "l-1",
      supplierId: "s-1",
      changeType: "CREATE",
      changeContent: "创建",
      createTime: "2026-10-30T09:00:00",
    });
    expect(log.operatorName).toBe("系统");
  });

  it("maps an unknown enum value to a dash instead of crashing", () => {
    expect(labelOf(AUDIT_STATUSES, 99)).toBe("-");
    expect(labelOf(AUDIT_STATUSES, 2)).toBe("已通过");
  });
});
