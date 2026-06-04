"use client";

import { useQuery } from "@tanstack/react-query";
import { productApi } from "../endpoints";
import type { PageParam } from "../types";

const productKeys = {
  all: ["products"] as const,
  list: (params?: PageParam & { keyword?: string }) =>
    [...productKeys.all, "list", params] as const,
  detail: (id: string) => [...productKeys.all, id] as const,
  search: (keyword: string) => [...productKeys.all, "search", keyword] as const,
};

export function useProducts(params?: PageParam & { keyword?: string }) {
  return useQuery({
    queryKey: productKeys.list(params),
    queryFn: () => productApi.list(params),
  });
}

export function useProductDetail(id: string) {
  return useQuery({
    queryKey: productKeys.detail(id),
    queryFn: () => productApi.getById(id),
    enabled: !!id,
  });
}

export function useProductSearch(keyword: string) {
  return useQuery({
    queryKey: productKeys.search(keyword),
    queryFn: () => productApi.search(keyword),
    enabled: keyword.length > 0,
  });
}
