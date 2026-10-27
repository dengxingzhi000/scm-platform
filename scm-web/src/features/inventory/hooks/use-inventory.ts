'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { toApiError } from '@/lib/api/envelope'
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
