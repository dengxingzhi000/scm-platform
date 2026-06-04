"use client";

import { useQuery } from "@tanstack/react-query";
import { inventoryApi } from "../endpoints";

const inventoryKeys = {
  all: ["inventory"] as const,
  bySku: (skuId: string) => [...inventoryKeys.all, skuId] as const,
  list: (params?: { keyword?: string; lowStock?: boolean }) =>
    [...inventoryKeys.all, "list", params] as const,
};

export function useInventoryList(params?: { keyword?: string; lowStock?: boolean }) {
  return useQuery({
    queryKey: inventoryKeys.list(params),
    queryFn: () => inventoryApi.list(params),
  });
}

export function useInventoryBySku(skuId: string) {
  return useQuery({
    queryKey: inventoryKeys.bySku(skuId),
    queryFn: () => inventoryApi.getBySku(skuId),
    enabled: !!skuId,
  });
}
