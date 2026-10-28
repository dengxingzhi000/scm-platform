'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { orderApiService } from '@/lib/api/services/order'
import { toApiError } from '@/lib/api/envelope'
import type { RefundQueryRequest } from '@/lib/api/contracts/order'

interface HandlerRequest {
  handlerId: string
  handlerName?: string
  handlerRemark?: string
}

/**
 * Refund data is reached straight through `orderApiService` (the raw contract
 * DTOs are consumed by the screen), so the query namespace is the flat
 * `['order', 'refund', ...]` — never the `['orders', ...]` used by the
 * order-list hooks, which map through the feature mappers.
 */
export function useRefundList(request: RefundQueryRequest) {
  return useQuery({
    queryKey: ['order', 'refund', 'list', request],
    queryFn: () => orderApiService.queryRefunds(request),
  })
}

export function useRefundDetail(id: string | null) {
  return useQuery({
    queryKey: ['order', 'refund', 'detail', id],
    queryFn: () => orderApiService.getRefund(id as string),
    enabled: !!id,
  })
}

export function useRefundItems(id: string | null) {
  return useQuery({
    queryKey: ['order', 'refund', 'items', id],
    queryFn: () => orderApiService.listRefundItems(id as string),
    enabled: !!id,
  })
}

export function useApproveRefund() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, request }: { id: string; request: HandlerRequest }) =>
      orderApiService.approveRefund(id, request),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['order', 'refund'] })
      message.success('退款已审核通过')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}

export function useRejectRefund() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      request,
    }: {
      id: string
      request: { handlerId: string; handlerName?: string; handlerRemark: string }
    }) => orderApiService.rejectRefund(id, request),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['order', 'refund'] })
      message.success('退款已驳回')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}

export function useCompleteRefund() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => orderApiService.completeRefund(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['order', 'refund'] })
      message.success('退款已完成')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}
