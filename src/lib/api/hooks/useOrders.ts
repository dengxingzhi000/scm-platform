"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { orderApi } from "../endpoints";
import type { PageParam, OrderVO } from "../types";

const orderKeys = {
  all: ["orders"] as const,
  list: (params?: PageParam & { status?: string }) =>
    [...orderKeys.all, "list", params] as const,
  detail: (orderNo: string) => [...orderKeys.all, orderNo] as const,
};

export function useOrders(params?: PageParam & { status?: string }) {
  return useQuery({
    queryKey: orderKeys.list(params),
    queryFn: () => orderApi.list(params),
  });
}

export function useOrderDetail(orderNo: string) {
  return useQuery({
    queryKey: orderKeys.detail(orderNo),
    queryFn: () => orderApi.getById(orderNo),
    enabled: !!orderNo,
  });
}

export function useCreateOrder() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: Record<string, unknown>) => orderApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: orderKeys.all });
    },
  });
}

export function useUpdateOrderStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ orderNo, status }: { orderNo: string; status: string }) =>
      orderApi.updateStatus(orderNo, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: orderKeys.all });
    },
  });
}
