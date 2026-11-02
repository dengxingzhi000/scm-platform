'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { SupplierCategoryDTO } from '@/lib/api/contracts/supplier'
import { supplierApiService } from '@/lib/api/services/supplier'

import { toSupplierCategory } from '../lib/mappers'
import { supplierCategoryKeys, supplierKeys } from '../lib/query-keys'

/**
 * 分类字典（GET /supplier-categories）。
 * 某个供应商**已关联**的分类在 use-supplier.ts 的 useSupplierAssignedCategories 里，
 * 两个 hook 名字相近但语义不同，不要合并。
 */
export function useSupplierCategories(params?: { keyword?: string; enabled?: boolean }) {
  return useQuery({
    queryKey: supplierCategoryKeys.list(params),
    queryFn: async () =>
      (await supplierApiService.categories.list(params)).map(toSupplierCategory),
  })
}

export function useCreateSupplierCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { data: SupplierCategoryDTO }) =>
      supplierApiService.categories.create(vars.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}

export function useUpdateSupplierCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string; data: SupplierCategoryDTO }) =>
      supplierApiService.categories.update(vars.id, vars.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}
