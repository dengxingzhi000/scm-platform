'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'

import { toApiError } from '@/lib/api/envelope'
import { financeApiService } from '@/lib/api/services/finance'
import type {
  ReconciliationQuery,
  ReconciliationRecordDTO,
} from '@/lib/api/contracts/finance'

import { financeService } from '../services/finance.service'

export function useSettlementList() {
  return useQuery({ queryKey: ['finance', 'settlements'], queryFn: () => financeService.listSettlements() })
}
export function useInvoiceList() {
  return useQuery({ queryKey: ['finance', 'invoices'], queryFn: () => financeService.listInvoices() })
}

const fail = (error: unknown) => message.error(toApiError(error).message)

export function useReconciliation(id: string) {
  return useQuery({
    queryKey: ['finance', 'reconciliation', id],
    queryFn: () => financeApiService.getReconciliation(id),
    enabled: !!id,
  })
}

export function useReconciliationPage(query: ReconciliationQuery) {
  return useQuery({
    queryKey: ['finance', 'reconciliation-page', query],
    queryFn: () => financeApiService.pageReconciliations(query),
  })
}

export function useCreateReconciliation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (entity: Partial<ReconciliationRecordDTO>) =>
      financeApiService.createReconciliation(entity),
    onSuccess: () => {
      message.success('对账记录已创建')
      qc.invalidateQueries({ queryKey: ['finance'] })
    },
    onError: fail,
  })
}

export function useUpdateReconciliation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      entity,
    }: {
      id: string
      entity: Partial<ReconciliationRecordDTO>
    }) => financeApiService.updateReconciliation(id, entity),
    onSuccess: () => {
      message.success('对账记录已更新')
      qc.invalidateQueries({ queryKey: ['finance'] })
    },
    onError: fail,
  })
}

export function useDeleteReconciliation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => financeApiService.deleteReconciliation(id),
    onSuccess: () => {
      message.success('对账记录已删除')
      qc.invalidateQueries({ queryKey: ['finance'] })
    },
    onError: fail,
  })
}

export function useReconcileAction() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      action,
      actorId,
      actorName,
      diffReason,
    }: {
      id: string
      action: 'reconcile' | 'confirm' | 'mark-diff'
      actorId?: string
      actorName?: string
      diffReason?: string
    }) => {
      if (action === 'reconcile')
        return financeApiService.reconcile(id, actorId ?? '', actorName ?? '')
      if (action === 'confirm')
        return financeApiService.confirmReconciliation(
          id,
          actorId ?? '',
          actorName ?? '',
        )
      return financeApiService.markReconciliationDiff(id, diffReason ?? '')
    },
    onSuccess: (_data, variables) => {
      message.success(
        variables.action === 'reconcile'
          ? '对账已完成'
          : variables.action === 'confirm'
            ? '对账已确认'
            : '已标记差异',
      )
      qc.invalidateQueries({ queryKey: ['finance'] })
    },
    onError: fail,
  })
}
