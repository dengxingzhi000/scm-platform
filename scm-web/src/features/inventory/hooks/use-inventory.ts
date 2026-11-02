'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { toApiError } from '@/lib/api/envelope'
import { inventoryApiService } from '@/lib/api/services/inventory'
import { inventoryService } from '../services/inventory.service'
import type { InventorySearchParams, StockAdjustment } from '../types'

export function useInventoryList(params?: InventorySearchParams) {
  return useQuery({
    queryKey: ['inventory', 'list', params],
    queryFn: () => inventoryService.list(params),
  })
}

export function useStockAlerts() {
  return useQuery({
    queryKey: ['inventory', 'alerts'],
    queryFn: () => inventoryService.getAlerts(),
  })
}

export function useScanStockAlerts() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => inventoryApiService.scanAlerts(),
    onSuccess: (result) => {
      qc.invalidateQueries({ queryKey: ['inventory'] })
      message.success(
        `扫描完成：新建 ${result.created}，刷新 ${result.updated}，解除 ${result.resolved}`
      )
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}

export function useResolveStockAlert() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => inventoryApiService.resolveAlert(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inventory', 'alerts'] })
      message.success('告警已处理')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}

export function useInventoryStats() {
  return useQuery({
    queryKey: ['inventory', 'stats'],
    queryFn: () => inventoryService.stats(),
  })
}

export function useAdjustStock() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (adjustment: StockAdjustment) => inventoryService.adjustStock(adjustment),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inventory'] })
      message.success('库存调整成功')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}

export function useTransferStock() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (request: Parameters<typeof inventoryService.transfer>[0]) =>
      inventoryService.transfer(request),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inventory'] })
      message.success('库存调拨成功')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}

export function useInitStock() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (request: { skuId: string; warehouseId: string; initialStock: number }) =>
      inventoryService.initStock(request.skuId, request.warehouseId, request.initialStock),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inventory'] })
      message.success('库存初始化成功')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}
