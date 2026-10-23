import { z } from "zod";

import type { Product } from "../types";

function moneyString(message: string) {
  return z
    .string()
    .trim()
    .min(1, message)
    .refine(
      (value) => Number.isFinite(Number(value)) && Number(value) >= 0,
      "请输入有效的非负金额",
    );
}

/**
 * Long-form validation for the product create/edit screen.
 *
 * Numeric inputs are kept as strings while editing (that is what `<input>`
 * holds) and converted once in `toProductPayload`, which is the unit-tested
 * data transformation.
 */
export const productFormSchema = z.object({
  name: z.string().trim().min(1, "请输入商品名称").max(100, "商品名称最多 100 个字符"),
  code: z
    .string()
    .trim()
    .min(1, "请输入商品编码")
    .max(50, "商品编码最多 50 个字符")
    .regex(/^[A-Za-z0-9_-]+$/, "商品编码只能包含字母、数字、下划线和短横线"),
  categoryId: z.string().min(1, "请选择商品分类"),
  brandId: z.string().optional(),
  price: moneyString("请输入售价"),
  costPrice: z
    .string()
    .optional()
    .refine(
      (value) =>
        !value ||
        (Number.isFinite(Number(value)) && Number(value) >= 0),
      "请输入有效的非负金额",
    ),
  unit: z.string().trim().min(1, "请输入单位").max(20, "单位最多 20 个字符"),
  status: z.enum(["on_sale", "off_sale", "draft"]),
  description: z.string().max(500, "描述最多 500 个字符").optional(),
});

export type ProductFormValues = z.infer<typeof productFormSchema>;

export type ProductFormPayload = Pick<
  Product,
  | "name"
  | "code"
  | "categoryId"
  | "brandId"
  | "price"
  | "costPrice"
  | "unit"
  | "status"
  | "description"
>;

export function toProductPayload(values: ProductFormValues): ProductFormPayload {
  return {
    name: values.name.trim(),
    code: values.code.trim(),
    categoryId: values.categoryId,
    brandId: values.brandId || undefined,
    price: Number(values.price),
    costPrice: values.costPrice ? Number(values.costPrice) : undefined,
    unit: values.unit.trim(),
    status: values.status,
    description: values.description?.trim() || undefined,
  };
}

export function toProductFormValues(product: Product): ProductFormValues {
  return {
    name: product.name,
    code: product.code,
    categoryId: product.categoryId,
    brandId: product.brandId,
    price: String(product.price),
    costPrice:
      product.costPrice === undefined ? "" : String(product.costPrice),
    unit: product.unit,
    status: product.status,
    description: product.description ?? "",
  };
}
