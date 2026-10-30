import { describe, expect, it } from "vitest";

import {
  supplierFormDefaults,
  supplierFormSchema,
  toSupplierFormValues,
  toSupplierPayload,
} from "./supplier-form-schema";

describe("supplier form schema", () => {
  it("requires a supplier name", () => {
    const result = supplierFormSchema.safeParse({ ...toSupplierFormValues({ id: "s-1", supplierName: "" }), supplierName: "" });
    expect(result.success).toBe(false);
  });

  it("rejects a supplier code with illegal characters", () => {
    const result = supplierFormSchema.safeParse({ ...toSupplierFormValues({ id: "s-1", supplierName: "华强" }), supplierCode: "SUP 001!" });
    expect(result.success).toBe(false);
  });

  it("rejects a credit rating outside the allowed set", () => {
    const result = supplierFormSchema.safeParse({ ...toSupplierFormValues({ id: "s-1", supplierName: "华强" }), creditRating: "E" });
    expect(result.success).toBe(false);
  });

  it("sends an empty string when the user clears an optional field", () => {
    const values = { ...toSupplierFormValues({ id: "s-1", supplierName: "华强", remark: "旧备注" }) };
    values.remark = "";
    const payload = toSupplierPayload(values);
    expect(payload.remark).toBe("");
    expect("remark" in payload).toBe(true);
  });

  it("omits fields the form does not own", () => {
    const payload = toSupplierPayload(toSupplierFormValues({ id: "s-1", supplierName: "华强" }));
    expect("cooperationStartDate" in payload).toBe(false);
    expect("supplierLevel" in payload).toBe(false);
    expect("auditStatus" in payload).toBe(false);
    expect("riskLevel" in payload).toBe(false);
    expect("blacklisted" in payload).toBe(false);
  });

  it("converts the registered capital string into a number", () => {
    const values = { ...toSupplierFormValues({ id: "s-1", supplierName: "华强" }) };
    values.registeredCapital = "5000000";
    expect(toSupplierPayload(values).registeredCapital).toBe(5000000);
  });

  it("rejects a negative registered capital", () => {
    const values = { ...toSupplierFormValues({ id: "s-1", supplierName: "华强" }) };
    values.registeredCapital = "-1";
    expect(supplierFormSchema.safeParse(values).success).toBe(false);
  });

  it("accepts the empty defaults once a name is filled in", () => {
    expect(supplierFormSchema.safeParse({ ...supplierFormDefaults, supplierName: "华强" }).success).toBe(true);
    expect(supplierFormSchema.safeParse(supplierFormDefaults).success).toBe(false);
  });

  it("rejects a malformed contact email", () => {
    const result = supplierFormSchema.safeParse({ ...toSupplierFormValues({ id: "s-1", supplierName: "华强" }), contactEmail: "not-an-email" });
    expect(result.success).toBe(false);
  });

  it("rejects a supplier type outside 1-4", () => {
    const result = supplierFormSchema.safeParse({ ...toSupplierFormValues({ id: "s-1", supplierName: "华强" }), supplierType: "9" });
    expect(result.success).toBe(false);
  });

  it("sends exactly the fields the form owns", () => {
    const values = {
      ...toSupplierFormValues({ id: "s-1", supplierName: "华强" }),
      supplierType: "2",
      registeredCapital: "100",
      sortOrder: "5",
      creditRating: "A",
    };

    expect(Object.keys(toSupplierPayload(values)).sort()).toEqual(
      [
        "bankAccount",
        "bankAccountName",
        "bankName",
        "businessLicense",
        "businessType",
        "certificates",
        "contactAddress",
        "contactEmail",
        "contactName",
        "contactPhone",
        "creditRating",
        "establishmentDate",
        "legalPerson",
        "paymentMethod",
        "paymentTerms",
        "registrationNo",
        "registeredCapital",
        "remark",
        "sortOrder",
        "supplierCode",
        "supplierName",
        "supplierNameEn",
        "supplierType",
        "taxNo",
        "website",
      ].sort(),
    );
  });

  it("omits the numeric fields when they are blank", () => {
    const payload = toSupplierPayload(toSupplierFormValues({ id: "s-1", supplierName: "华强" }));

    expect("supplierType" in payload).toBe(false);
    expect("registeredCapital" in payload).toBe(false);
    expect("sortOrder" in payload).toBe(false);
  });

  it("omits a blank credit rating because the column rejects an empty string", () => {
    const payload = toSupplierPayload(toSupplierFormValues({ id: "s-1", supplierName: "华强" }));

    expect("creditRating" in payload).toBe(false);
    expect(toSupplierPayload({ ...toSupplierFormValues({ id: "s-1", supplierName: "华强" }), creditRating: "A" }).creditRating).toBe("A");
  });
});

describe("toSupplierFormValues", () => {
  it("turns the dto into editable strings", () => {
    const values = toSupplierFormValues({
      id: "s-1",
      supplierName: "华强",
      supplierType: 2,
      registeredCapital: 5000000,
      sortOrder: 10,
      remark: "旧备注",
    });

    expect(values.supplierName).toBe("华强");
    expect(values.supplierType).toBe("2");
    expect(values.registeredCapital).toBe("5000000");
    expect(values.sortOrder).toBe("10");
    expect(values.remark).toBe("旧备注");
    expect(values.taxNo).toBe("");
  });

  it("does not carry the server-managed fields", () => {
    const values = toSupplierFormValues({
      id: "s-1",
      supplierName: "华强",
      cooperationStartDate: "2024-01-01",
      supplierLevel: 2,
      auditStatus: 1,
      riskLevel: 3,
      blacklisted: true,
      overallScore: 88,
    });

    expect("cooperationStartDate" in values).toBe(false);
    expect("supplierLevel" in values).toBe(false);
    expect("auditStatus" in values).toBe(false);
    expect("riskLevel" in values).toBe(false);
    expect("blacklisted" in values).toBe(false);
    expect("overallScore" in values).toBe(false);
  });
});
