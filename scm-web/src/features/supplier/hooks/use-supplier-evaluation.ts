'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { toUiPage } from '@/lib/api/contracts/page'
import type { SupplierEvaluationDTO } from '@/lib/api/contracts/supplier'
import { supplierApiService } from '@/lib/api/services/supplier'

import { toSupplierEvaluation } from '../lib/mappers'
import {
  supplierCategoryKeys,
  supplierEvaluationKeys,
  supplierKeys,
} from '../lib/query-keys'

/** 评价列表一页的条数 —— 后端默认 10，前端列表统一用 20。 */
const EVALUATION_PAGE_SIZE = 20

export function useSupplierEvaluations(supplierId: string, page: number, size?: number) {
  return useQuery({
    queryKey: supplierEvaluationKeys.page(supplierId, page),
    queryFn: async () =>
      toUiPage(
        await supplierApiService.evaluations.page({
          page,
          size: size ?? EVALUATION_PAGE_SIZE,
          supplierId,
        }),
        toSupplierEvaluation,
      ),
    enabled: !!supplierId,
  })
}

export function useSupplierEvaluationDetail(id: string) {
  return useQuery({
    queryKey: supplierEvaluationKeys.detail(id),
    queryFn: async () => toSupplierEvaluation(await supplierApiService.evaluations.getById(id)),
    enabled: !!id,
  })
}

/**
 * 供应商的平均得分。后端 `calculateAverageScore` 对空 id 也返回 0，
 * 所以这里永远是数字，不用兜底 null。
 */
export function useSupplierAverageScore(supplierId: string) {
  return useQuery({
    queryKey: supplierEvaluationKeys.averageScore(supplierId),
    queryFn: () => supplierApiService.evaluations.averageScore(supplierId),
    enabled: !!supplierId,
  })
}

export function useCreateSupplierEvaluation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { data: SupplierEvaluationDTO }) =>
      supplierApiService.evaluations.create(vars.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierEvaluationKeys.all })
    },
  })
}

export function useUpdateSupplierEvaluation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string; data: SupplierEvaluationDTO }) =>
      supplierApiService.evaluations.update(vars.id, vars.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierEvaluationKeys.all })
    },
  })
}

export function useDeleteSupplierEvaluation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string }) => supplierApiService.evaluations.remove(vars.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierEvaluationKeys.all })
    },
  })
}
