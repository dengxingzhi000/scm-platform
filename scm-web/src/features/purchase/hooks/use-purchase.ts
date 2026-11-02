'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'

import { toApiError } from '@/lib/api/envelope'
import { purchaseApiService } from '@/lib/api/services/purchase'
import type {
  PurQuotationDTO,
  PurQuotationQuery,
  PurRequestConvertPayload,
  PurRequestQuery,
  PurRequestUpsertPayload,
  PurRfqDTO,
  PurRfqItemDTO,
  PurRfqQuery,
} from '@/lib/api/contracts/purchase'

import { purchaseService } from '../services/purchase.service'
import type { PurchaseSearchParams } from '../types'

export function usePurchaseOrderList(params?: PurchaseSearchParams) {
  return useQuery({ queryKey: ['purchase', 'list', params], queryFn: () => purchaseService.list(params) })
}
export function usePurchaseOrderDetail(id: string) {
  return useQuery({ queryKey: ['purchase', 'detail', id], queryFn: () => purchaseService.getById(id), enabled: !!id })
}

const fail = (error: unknown) => message.error(toApiError(error).message)

export function useRfqPage(query: PurRfqQuery) {
  return useQuery({
    queryKey: ['purchase', 'rfq', query],
    queryFn: () => purchaseApiService.pageRfqs(query),
  })
}

export function useRfqItems(rfqId: string, enabled = true) {
  return useQuery({
    queryKey: ['purchase', 'rfq', rfqId, 'items'],
    queryFn: () => purchaseApiService.listRfqItems(rfqId),
    enabled: enabled && !!rfqId,
  })
}

export function useSaveRfq() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (entity: Partial<PurRfqDTO>) =>
      entity.id
        ? purchaseApiService.updateRfq(entity)
        : purchaseApiService.createRfq(entity),
    onSuccess: () => {
      message.success('询价单已保存')
      qc.invalidateQueries({ queryKey: ['purchase', 'rfq'] })
    },
    onError: fail,
  })
}

export function useRfqAction() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, action }: { id: string; action: 'publish' | 'close' | 'delete' }) => {
      if (action === 'publish') return purchaseApiService.publishRfq(id)
      if (action === 'close') return purchaseApiService.closeRfq(id)
      return purchaseApiService.deleteRfq(id)
    },
    onSuccess: (_data, variables) => {
      message.success(
        variables.action === 'publish'
          ? '询价单已发布'
          : variables.action === 'close'
            ? '询价单已关闭'
            : '询价单已删除',
      )
      qc.invalidateQueries({ queryKey: ['purchase', 'rfq'] })
    },
    onError: fail,
  })
}

export function useCreateRfqItem() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (entity: Partial<PurRfqItemDTO>) =>
      purchaseApiService.createRfqItem(entity),
    onSuccess: () => {
      message.success('询价明细已添加')
      qc.invalidateQueries({ queryKey: ['purchase', 'rfq'] })
    },
    onError: fail,
  })
}

export function useDeleteRfqItem() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => purchaseApiService.deleteRfqItem(id),
    onSuccess: () => {
      message.success('询价明细已删除')
      qc.invalidateQueries({ queryKey: ['purchase', 'rfq'] })
    },
    onError: fail,
  })
}

export function useRequestPage(query: PurRequestQuery) {
  return useQuery({
    queryKey: ['purchase', 'request', query],
    queryFn: () => purchaseApiService.pageRequests(query),
  })
}

export function useRequestDetail(id: string, enabled = true) {
  return useQuery({
    queryKey: ['purchase', 'request', id, 'detail'],
    queryFn: () => purchaseApiService.getRequestDetail(id),
    enabled: enabled && !!id,
  })
}

export function useCreateRequest() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: PurRequestUpsertPayload) =>
      purchaseApiService.createRequest(payload),
    onSuccess: () => {
      message.success('采购申请已保存为草稿')
      qc.invalidateQueries({ queryKey: ['purchase', 'request'] })
    },
    onError: fail,
  })
}

export function useUpdateRequest() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: PurRequestUpsertPayload }) =>
      purchaseApiService.updateRequest(id, payload),
    onSuccess: (_data, variables) => {
      message.success('采购申请已更新')
      qc.invalidateQueries({ queryKey: ['purchase', 'request'] })
      qc.invalidateQueries({
        queryKey: ['purchase', 'request', variables.id, 'detail'],
      })
    },
    onError: fail,
  })
}

export function useRequestAction() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      action,
      reason,
    }: {
      id: string
      action: 'submit' | 'approve' | 'reject' | 'close' | 'delete'
      reason?: string
    }) => {
      if (action === 'submit') return purchaseApiService.submitRequest(id)
      if (action === 'approve') return purchaseApiService.approveRequest(id)
      if (action === 'reject')
        return purchaseApiService.rejectRequest(id, reason ?? '')
      if (action === 'close') return purchaseApiService.closeRequest(id)
      return purchaseApiService.deleteRequest(id)
    },
    onSuccess: (_data, variables) => {
      message.success(
        variables.action === 'submit'
          ? '采购申请已提交审批'
          : variables.action === 'approve'
            ? '采购申请已审批通过'
            : variables.action === 'reject'
              ? '采购申请已驳回'
              : variables.action === 'close'
                ? '采购申请已关闭'
                : '采购申请已删除',
      )
      qc.invalidateQueries({ queryKey: ['purchase', 'request'] })
    },
    onError: fail,
  })
}

export function useConvertRequest() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      payload,
    }: {
      id: string
      payload: PurRequestConvertPayload
    }) => purchaseApiService.convertRequest(id, payload),
    onSuccess: (order) => {
      message.success(`已生成采购单 ${order.orderNo ?? ''}`)
      qc.invalidateQueries({ queryKey: ['purchase', 'request'] })
      qc.invalidateQueries({ queryKey: ['purchase', 'list'] })
    },
    onError: fail,
  })
}

export function useQuotationPage(query: PurQuotationQuery) {
  return useQuery({
    queryKey: ['purchase', 'quotation', query],
    queryFn: () => purchaseApiService.pageQuotations(query),
  })
}

export function useQuotationItems(quotationId: string, enabled = true) {
  return useQuery({
    queryKey: ['purchase', 'quotation', quotationId, 'items'],
    queryFn: () => purchaseApiService.listQuotationItems(quotationId),
    enabled: enabled && !!quotationId,
  })
}

export function useCreateQuotation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (entity: Partial<PurQuotationDTO>) =>
      purchaseApiService.createQuotation(entity),
    onSuccess: () => {
      message.success('报价单已保存')
      qc.invalidateQueries({ queryKey: ['purchase', 'quotation'] })
    },
    onError: fail,
  })
}

export function useQuotationAction() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      action,
      selectedBy,
      selectedByName,
    }: {
      id: string
      action: 'submit' | 'select' | 'delete'
      selectedBy?: string
      selectedByName?: string
    }) => {
      if (action === 'submit') return purchaseApiService.submitQuotation(id)
      if (action === 'select')
        return purchaseApiService.selectQuotation(
          id,
          selectedBy ?? '',
          selectedByName ?? '',
        )
      return purchaseApiService.deleteQuotation(id)
    },
    onSuccess: (_data, variables) => {
      message.success(
        variables.action === 'submit'
          ? '报价单已提交'
          : variables.action === 'select'
            ? '报价单已选用'
            : '报价单已删除',
      )
      qc.invalidateQueries({ queryKey: ['purchase', 'quotation'] })
    },
    onError: fail,
  })
}
