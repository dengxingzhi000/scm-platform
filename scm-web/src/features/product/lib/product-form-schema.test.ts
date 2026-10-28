import { describe, expect, it } from "vitest";

import type { Product } from "../types";
import {
  productFormSchema,
  toProductFormValues,
  toProductPayload,
  type ProductFormValues,
} from "./product-form-schema";

const validValues: ProductFormValues = {
  name: "  iPhone 15 Pro  ",
  code: "  PRD-001  ",
  categoryId: "11",
  brandId: "1",
  price: "7999",
  status: "on_sale",
  description: "  旗舰机型  ",
};

const product: Product = {
  id: "1",
  name: "iPhone 15 Pro",
  code: "PRD-001",
  categoryId: "11",
  categoryName: "手机",
  brandId: "1",
  brandName: "Apple",
  description: "旗舰机型",
  status: "on_sale",
  price: 7999,
  costPrice: 5500,
  skus: [],
  createdAt: "2026-01-01",
  updatedAt: "2026-01-02",
};

describe("productFormSchema", () => {
  it("accepts the valid long-form payload", () => {
    expect(productFormSchema.safeParse(validValues).success).toBe(true);
  });

  it("rejects a non-numeric price", () => {
    const result = productFormSchema.safeParse({
      ...validValues,
      price: "abc",
    });

    expect(result.success).toBe(false);
    expect(result.error?.issues[0]?.message).toBe("请输入有效的非负金额");
  });

  it("rejects an empty name and an invalid code", () => {
    const result = productFormSchema.safeParse({
      ...validValues,
      name: "   ",
      code: "PRD 001",
    });

    expect(result.success).toBe(false);
    expect(result.error?.issues.map((issue) => issue.path[0])).toEqual(
      expect.arrayContaining(["name", "code"]),
    );
  });

  it("allows an empty optional brand and description", () => {
    const result = productFormSchema.safeParse({
      ...validValues,
      brandId: undefined,
      description: "",
    });

    expect(result.success).toBe(true);
  });
});

describe("toProductPayload", () => {
  it("trims strings, converts money and omits blank optionals", () => {
    expect(toProductPayload(validValues)).toEqual({
      name: "iPhone 15 Pro",
      code: "PRD-001",
      categoryId: "11",
      brandId: "1",
      price: 7999,
      status: "on_sale",
      description: "旗舰机型",
    });
  });

  it("maps blank optional fields to undefined", () => {
    const payload = toProductPayload({
      ...validValues,
      brandId: "",
      description: "   ",
    });

    expect(payload.brandId).toBeUndefined();
    expect(payload.description).toBeUndefined();
  });
});

describe("toProductFormValues", () => {
  it("turns a product entity into editable strings", () => {
    expect(toProductFormValues(product)).toEqual({
      name: "iPhone 15 Pro",
      code: "PRD-001",
      categoryId: "11",
      brandId: "1",
      price: "7999",
      status: "on_sale",
      description: "旗舰机型",
    });
  });

  it("uses an empty string when optional fields are missing", () => {
    const values = toProductFormValues({
      ...product,
      description: undefined,
      brandId: undefined,
    });

    expect(values.description).toBe("");
    expect(values.brandId).toBeUndefined();
  });
});
