'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { toApiError } from '@/lib/api/envelope'
import { orderService } from '../services/order.service'
import type { OrderSearchParams, OrderStatus } from '../types'

export function useOrderList(params?: OrderSearchParams) {
  return useQuery({
    queryKey: ['orders', 'list', params],
    queryFn: () => orderService.list(params),
  })
}

export function useOrderDetail(id: string) {
  return useQuery({
    queryKey: ['orders', 'detail', id],
    queryFn: () => orderService.getById(id),
    enabled: !!id,
  })
}

export function useOrderRefunds(orderId: string) {
  return useQuery({
    queryKey: ['orders', 'refunds', orderId],
    queryFn: () => orderService.listRefundsByOrder(orderId),
    enabled: !!orderId,
  })
}

export function useOrderStatusHistory(orderId: string) {
  return useQuery({
    queryKey: ['orders', 'status-history', orderId],
    queryFn: () => orderService.listStatusHistory(orderId),
    enabled: !!orderId,
  })
}

export function useUpdateOrderStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: OrderStatus }) =>
      orderService.updateStatus(id, status),
    onSuccess: (order) => {
      qc.invalidateQueries({ queryKey: ['orders'] })
      message.success(`已更新为${order.statusLabel}`)
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}

export function useCancelOrder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      orderService.cancel(id, reason),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['orders'] })
      message.success('订单已取消')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}
