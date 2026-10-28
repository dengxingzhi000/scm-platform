'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { toApiError } from '@/lib/api/envelope'
import { systemService } from '@/lib/api/services/system'
import type {
  AvailableActionDTO,
  StatusDictDTO,
  StatusTransitionDTO,
  TransitionCheckDTO,
  TransitionResultDTO,
} from '@/lib/api/contracts/system'

/** Canonical bizType values from the backend warmer (StatusDictCacheManager.prewarmGlobal). */
export const BIZ_TYPES = [
  'ORDER',
  'PURCHASE',
  'PURCHASE_REQUEST',
  'INBOUND',
  'OUTBOUND',
  'APPROVAL',
  'LOGISTICS',
] as const

const ROOT = ['status'] as const

function fail(action: string) {
  return (error: unknown) => {
    message.error(`${action}失败：${toApiError(error).message}`)
  }
}

// ---------- 状态字典 ----------

export function useStatusDictList(bizType: string) {
  return useQuery({
    queryKey: [...ROOT, 'dict', bizType],
    queryFn: () => systemService.status.listDict(bizType),
    enabled: !!bizType,
  })
}

export function useStatusDictItem(bizType: string, statusCode: string) {
  return useQuery({
    queryKey: [...ROOT, 'dict', bizType, statusCode],
    queryFn: () => systemService.status.getDict(bizType, statusCode),
    enabled: !!bizType && !!statusCode,
  })
}

export function useCreateStatusDict(bizType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: StatusDictDTO) => systemService.status.createDict(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'dict', bizType] })
      message.success('创建成功')
    },
    onError: fail('创建'),
  })
}

/** PUT takes the whole row including `id` — there is no path variable. */
export function useUpdateStatusDict(bizType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: StatusDictDTO) => systemService.status.updateDict(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'dict', bizType] })
      message.success('更新成功')
    },
    onError: fail('更新'),
  })
}

export function useDeleteStatusDict(bizType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => systemService.status.deleteDict(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'dict', bizType] })
      message.success('删除成功')
    },
    onError: fail('删除'),
  })
}

// ---------- 状态流转规则 ----------

export function useStatusTransitionList(bizType: string) {
  return useQuery({
    queryKey: [...ROOT, 'transitions', bizType],
    queryFn: () => systemService.status.listTransitions(bizType),
    enabled: !!bizType,
  })
}

export function useCreateStatusTransition(bizType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: StatusTransitionDTO) =>
      systemService.status.createTransition(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'transitions', bizType] })
      message.success('创建成功')
    },
    onError: fail('创建'),
  })
}

export function useUpdateStatusTransition(bizType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: StatusTransitionDTO) =>
      systemService.status.updateTransition(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'transitions', bizType] })
      message.success('更新成功')
    },
    onError: fail('更新'),
  })
}

export function useDeleteStatusTransition(bizType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => systemService.status.deleteTransition(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'transitions', bizType] })
      message.success('删除成功')
    },
    onError: fail('删除'),
  })
}

// ---------- 只读查询辅助（不触发缓存失效） ----------

export function useNextStatuses(bizType: string, currentStatus: string) {
  return useQuery({
    queryKey: [...ROOT, 'next-statuses', bizType, currentStatus],
    queryFn: () => systemService.status.nextStatuses(bizType, currentStatus),
    enabled: !!bizType && !!currentStatus,
  })
}

export function useAvailableActions(
  bizType: string,
  currentStatus: string,
  enabled: boolean,
) {
  return useQuery<AvailableActionDTO[]>({
    queryKey: [...ROOT, 'actions', bizType, currentStatus],
    queryFn: () => systemService.status.actions(bizType, currentStatus),
    enabled: enabled && !!bizType && !!currentStatus,
  })
}

export interface CheckInput {
  bizType: string
  fromStatus: string
  toStatus: string
}

export function useTransitionCheck() {
  return useMutation<TransitionCheckDTO, unknown, CheckInput>({
    mutationFn: ({ bizType, fromStatus, toStatus }) =>
      systemService.status.check(bizType, fromStatus, toStatus),
    onError: fail('校验'),
  })
}

export interface TransitionInput {
  bizType: string
  fromStatus: string
  actionCode: string
}

export function useStatusTransitionPreview() {
  return useMutation<TransitionResultDTO, unknown, TransitionInput>({
    mutationFn: ({ bizType, fromStatus, actionCode }) =>
      systemService.status.transition(bizType, fromStatus, actionCode),
    onError: fail('流转'),
  })
}
