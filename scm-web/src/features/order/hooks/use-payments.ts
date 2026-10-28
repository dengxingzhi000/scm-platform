'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { orderApiService } from '@/lib/api/services/order'
import { toApiError } from '@/lib/api/envelope'
import type {
  PaymentCreateRequest,
  PaymentQueryRequest,
  PaymentStatus,
} from '@/lib/api/contracts/order'

export function usePaymentList(request: PaymentQueryRequest) {
  return useQuery({
    queryKey: ['order', 'payment', 'list', request],
    queryFn: () => orderApiService.queryPayments(request),
  })
}

export function usePaymentDetail(id: string | null) {
  return useQuery({
    queryKey: ['order', 'payment', 'detail', id],
    queryFn: () => orderApiService.getPayment(id as string),
    enabled: !!id,
  })
}

export function useCreatePayment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (request: PaymentCreateRequest) =>
      orderApiService.createPayment(request),
    onSuccess: (payment) => {
      qc.invalidateQueries({ queryKey: ['order', 'payment'] })
      message.success(`支付单 ${payment.paymentNo ?? ''} 已创建`)
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}

export function useUpdatePaymentStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      targetStatus,
      reason,
    }: {
      id: string
      targetStatus: PaymentStatus
      reason?: string
    }) => orderApiService.updatePaymentStatus(id, { targetStatus, reason }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['order', 'payment'] })
      message.success('支付状态已更新')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}
