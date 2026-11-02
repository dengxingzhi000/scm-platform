import type {
  ProdBrandDTO,
  ProdCategoryDTO,
  ProdSkuDTO,
  ProdSpuDTO,
  ProdSpuPayload,
  ProductPage,
  ProductStatus,
} from "@/lib/api/contracts/product";

import type { ProductFormPayload } from "../lib/product-form-schema";
import type { Brand, Category, PageResult, Product, SKU } from "../types";

/** Root categories are stored with the sentinel `"0"` parent id. */
const ROOT_PARENT_ID = "0";

export function spuStatusToUi(status?: number | null): Product["status"] {
  if (status === 1) return "on_sale";
  if (status === 2) return "off_sale";
  return "draft";
}

export function spuStatusToApi(status?: string | null): ProductStatus | undefined {
  if (status === "on_sale") return 1;
  if (status === "off_sale") return 2;
  if (status === "draft") return 0;
  return undefined;
}

function parseAttributes(
  raw?: string | null
): Record<string, string> | undefined {
  if (!raw) return undefined;

  try {
    const parsed: unknown = JSON.parse(raw);
    if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
      return parsed as Record<string, string>;
    }
  } catch {
    // `attributes` is free-form on the backend; an unparsable value is ignored.
  }

  return undefined;
}

export function toSku(dto: ProdSkuDTO): SKU {
  return {
    id: dto.id,
    productId: dto.spuId ?? "",
    skuCode: dto.skuCode ?? "",
    name: dto.skuName ?? "",
    price: dto.sellingPrice ?? 0,
    costPrice: dto.costPrice ?? undefined,
    stock: dto.stock ?? 0,
    status: dto.status === 0 ? "disabled" : "active",
    attributes: parseAttributes(dto.attributes),
  };
}

export function toCategory(dto: ProdCategoryDTO): Category {
  return {
    id: dto.id,
    name: dto.categoryName ?? "",
    code: dto.categoryCode ?? "",
    parentId:
      dto.parentId && dto.parentId !== ROOT_PARENT_ID ? dto.parentId : null,
    sort: dto.sortOrder ?? 0,
    status: dto.enabled === false ? "disabled" : "active",
  };
}

/**
 * Builds the nested tree from the flat category catalog. The backend already
 * returns rows sorted by `sortOrder`, so insertion order is preserved.
 */
export function toCategoryTree(records: ProdCategoryDTO[]): Category[] {
  const nodes = new Map<string, Category>();

  records.forEach((dto) => {
    nodes.set(dto.id, toCategory(dto));
  });

  const roots: Category[] = [];

  nodes.forEach((node) => {
    const parent = node.parentId ? nodes.get(node.parentId) : undefined;
    if (parent) {
      (parent.children ??= []).push(node);
    } else {
      roots.push(node);
    }
  });

  return roots;
}

export function toBrand(dto: ProdBrandDTO): Brand {
  return {
    id: dto.id,
    name: dto.brandName ?? "",
    code: dto.brandCode ?? "",
    logo: dto.logoUrl ?? undefined,
    status: dto.enabled === false ? "disabled" : "active",
  };
}

interface ProductContext {
  categories?: ProdCategoryDTO[];
  brands?: ProdBrandDTO[];
  skus?: ProdSkuDTO[];
}

export function toProduct(
  dto: ProdSpuDTO,
  context: ProductContext = {}
): Product {
  const categoryName =
    context.categories?.find((item) => item.id === dto.categoryId)
      ?.categoryName ?? "";
  const brandName =
    context.brands?.find((item) => item.id === dto.brandId)?.brandName ?? "";

  const skus = (context.skus ?? []).map(toSku);
  const costPrices = skus
    .map((sku) => sku.costPrice)
    .filter((value): value is number => typeof value === "number");

  return {
    id: dto.id,
    name: dto.spuName ?? "",
    code: dto.spuCode ?? "",
    categoryId: dto.categoryId ?? "",
    categoryName,
    brandId: dto.brandId ?? undefined,
    brandName: brandName || undefined,
    description: dto.description ?? undefined,
    status: spuStatusToUi(dto.status),
    price: dto.minPrice ?? 0,
    costPrice: costPrices.length > 0 ? Math.min(...costPrices) : undefined,
    imageUrl: dto.mainImage ?? undefined,
    skus,
    createdAt: dto.createTime ?? "",
    updatedAt: dto.updateTime ?? "",
  };
}

export function toProductPage(
  page: ProductPage<ProdSpuDTO> | null | undefined,
  context: Pick<ProductContext, "categories" | "brands"> & { skus?: ProdSkuDTO[] } = {}
): PageResult<Product> {
  const skusBySpu = new Map<string, ProdSkuDTO[]>();
  for (const sku of context.skus ?? []) {
    if (!sku.spuId) continue;
    const group = skusBySpu.get(sku.spuId);
    if (group) {
      group.push(sku);
    } else {
      skusBySpu.set(sku.spuId, [sku]);
    }
  }

  return {
    records: (page?.records ?? []).map((dto) =>
      toProduct(dto, {
        categories: context.categories,
        brands: context.brands,
        skus: skusBySpu.get(dto.id) ?? [],
      })
    ),
    total: page?.total ?? 0,
    current: page?.pageNum ?? 1,
    pageSize: page?.pageSize ?? 0,
  };
}

export function toSpuPayload(data: ProductFormPayload): ProdSpuPayload {
  return {
    spuCode: data.code,
    spuName: data.name,
    categoryId: data.categoryId,
    brandId: data.brandId,
    description: data.description,
    minPrice: data.price,
    maxPrice: data.price,
    status: spuStatusToApi(data.status) ?? 0,
  };
}
