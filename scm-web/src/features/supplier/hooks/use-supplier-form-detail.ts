'use client'
import { useQuery } from '@tanstack/react-query'

import type { SupplierDTO } from '@/lib/api/contracts/supplier'
import { supplierApiService } from '@/lib/api/services/supplier'

import { supplierKeys } from '../lib/query-keys'

/**
 * 编辑表单用的主档详情：直接返回后端 **`SupplierDTO`**（不是 UI `Supplier`）。
 *
 * `useSupplierDetail` 走 `toSupplier` 映射出的 UI 模型缺
 * `bankName` / `bankAccount` / `businessLicense` / `certificates` 等表单字段，
 * 表单要用原始 DTO。query key 也单独用 `formDetail`，
 * 避免和 detail 的 UI 形状在同一条缓存里撞车。
 *
 * `id` 为空（新建模式）时查询保持禁用，不发请求。
 */
export function useSupplierFormDetail(id: string) {
  return useQuery({
    queryKey: supplierKeys.formDetail(id),
    queryFn: async (): Promise<SupplierDTO> => supplierApiService.getById(id),
    enabled: !!id,
  })
}
