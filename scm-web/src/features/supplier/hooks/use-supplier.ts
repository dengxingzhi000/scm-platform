'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { SupplierUpsertPayload } from '@/lib/api/contracts/supplier'
import { supplierApiService } from '@/lib/api/services/supplier'

import {
  toSupplier,
  toSupplierChangeLog,
  toSupplierCategory,
  toSupplierPage,
  toSupplierPerformance,
  toSupplierProfile,
  toSupplierStats,
} from '../lib/mappers'
import { supplierCategoryKeys, supplierKeys } from '../lib/query-keys'
import type { SupplierSearchParams } from '../types'
import { useOperator } from './use-operator'

/** 变更留痕默认取最近 20 条 —— 与后端 GET /{id}/change-logs 的默认值一致。 */
const DEFAULT_CHANGE_LOG_LIMIT = 20

/**
 * 供应商列表。`current` / `pageSize` / `status` 是前端自己的分页与启停筛选项，
 * 其余字段（keyword、supplierType、auditStatus…）与后端的查询参数同名，直接透传。
 */
export function useSupplierList(params?: SupplierSearchParams) {
  return useQuery({
    queryKey: supplierKeys.list(params),
    queryFn: async () => {
      const { current, pageSize, keyword, status, ...filters } = params ?? {}
      return toSupplierPage(
        await supplierApiService.list({
          page: current ?? 1,
          size: pageSize ?? 20,
          keyword: keyword || undefined,
          enabled: status === 'active' ? true : status === 'disabled' ? false : undefined,
          ...filters,
        })
      )
    },
  })
}

/**
 * 名称联想候选。输入为空时不发请求 —— 否则每次聚焦输入框都会打一次后端。
 */
export function useSupplierSearch(name: string) {
  return useQuery({
    queryKey: supplierKeys.search(name),
    queryFn: async () => (await supplierApiService.search(name)).map(toSupplier),
    enabled: name.trim().length > 0,
  })
}

export function useSupplierDetail(id: string) {
  return useQuery({
    queryKey: supplierKeys.detail(id),
    queryFn: async () => toSupplier(await supplierApiService.getById(id)),
    enabled: !!id,
  })
}

export function useSupplierProfile(id: string) {
  return useQuery({
    queryKey: supplierKeys.profile(id),
    queryFn: async () => toSupplierProfile(await supplierApiService.profile(id)),
    enabled: !!id,
  })
}

export function useSupplierPerformance(id: string) {
  return useQuery({
    queryKey: supplierKeys.performance(id),
    queryFn: async () => toSupplierPerformance(await supplierApiService.performance(id)),
    enabled: !!id,
  })
}

export function useSupplierStats() {
  return useQuery({
    queryKey: supplierKeys.stats,
    queryFn: async () => toSupplierStats(await supplierApiService.stats()),
  })
}

export function useSupplierChangeLogs(id: string, limit?: number) {
  const size = limit ?? DEFAULT_CHANGE_LOG_LIMIT
  return useQuery({
    queryKey: supplierKeys.changeLogs(id, size),
    queryFn: async () =>
      (await supplierApiService.changeLogs(id, size)).map(toSupplierChangeLog),
    enabled: !!id,
  })
}

/**
 * 某个供应商**已关联**的分类（GET /suppliers/{id}/categories）。
 * 分类字典（GET /supplier-categories）在 use-supplier-category.ts 里，两个 hook 不要混用。
 */
export function useSupplierAssignedCategories(supplierId: string) {
  return useQuery({
    queryKey: supplierKeys.categories(supplierId),
    queryFn: async () =>
      (await supplierApiService.getCategories(supplierId)).map(toSupplierCategory),
    enabled: !!supplierId,
  })
}

export function useCreateSupplier() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: SupplierUpsertPayload) => supplierApiService.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}

export function useUpdateSupplier() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string; data: SupplierUpsertPayload }) =>
      supplierApiService.update(vars.id, vars.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}

export function useDeleteSupplier() {
  const qc = useQueryClient()
  const { operatorId, operatorName } = useOperator()
  return useMutation({
    mutationFn: (vars: { id: string }) =>
      supplierApiService.remove(vars.id, { operatorId, operatorName }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}

export function useEnableSupplier() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string }) => supplierApiService.enable(vars.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}

export function useDisableSupplier() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { id: string }) => supplierApiService.disable(vars.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}

export function useSubmitSupplierAudit() {
  const qc = useQueryClient()
  const { operatorId, operatorName } = useOperator()
  return useMutation({
    mutationFn: (vars: { id: string }) =>
      supplierApiService.submitAudit(vars.id, { operatorId, operatorName }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}

export function useAuditSupplier() {
  const qc = useQueryClient()
  const { operatorId, operatorName } = useOperator()
  return useMutation({
    mutationFn: (vars: { id: string; action: 'APPROVE' | 'REJECT'; remark?: string }) =>
      supplierApiService.audit(vars.id, {
        action: vars.action,
        remark: vars.remark,
        operatorId,
        operatorName,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}

export function useBlacklistSupplier() {
  const qc = useQueryClient()
  const { operatorId, operatorName } = useOperator()
  return useMutation({
    mutationFn: (vars: { id: string; reason: string }) =>
      supplierApiService.blacklist(vars.id, vars.reason, { operatorId, operatorName }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}

export function useUnblacklistSupplier() {
  const qc = useQueryClient()
  const { operatorId, operatorName } = useOperator()
  return useMutation({
    mutationFn: (vars: { id: string }) =>
      supplierApiService.unblacklist(vars.id, { operatorId, operatorName }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}

export function useChangeSupplierRiskLevel() {
  const qc = useQueryClient()
  const { operatorId, operatorName } = useOperator()
  return useMutation({
    mutationFn: (vars: { id: string; level: number; reason?: string }) =>
      supplierApiService.changeRiskLevel(vars.id, vars.level, vars.reason, {
        operatorId,
        operatorName,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}

export function useChangeCooperationStatus() {
  const qc = useQueryClient()
  const { operatorId, operatorName } = useOperator()
  return useMutation({
    mutationFn: (vars: { id: string; status: number }) =>
      supplierApiService.changeCooperationStatus(vars.id, vars.status, {
        operatorId,
        operatorName,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}

export function useAssignSupplierCategories() {
  const qc = useQueryClient()
  const { operatorId, operatorName } = useOperator()
  return useMutation({
    mutationFn: (vars: { id: string; categoryIds: string[] }) =>
      supplierApiService.assignCategories(vars.id, vars.categoryIds, {
        operatorId,
        operatorName,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
    },
  })
}
