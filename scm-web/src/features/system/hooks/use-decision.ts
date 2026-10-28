'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { toApiError } from '@/lib/api/envelope'
import { systemService } from '@/lib/api/services/system'
import type {
  ConflictDTO,
  DecisionRuleDTO,
  WeightProfileDTO,
} from '@/lib/api/contracts/system'

/** Canonical engineType values used by the decision modules. */
export const ENGINE_TYPES = ['SCORING', 'NEAREST_NEIGHBOR', 'GREEDY'] as const

const ROOT = ['decision'] as const

function fail(action: string) {
  return (error: unknown) => {
    message.error(`${action}失败：${toApiError(error).message}`)
  }
}

// ---------- 规则 ----------

export function useDecisionRules(engineType: string) {
  return useQuery({
    queryKey: [...ROOT, 'rules', engineType],
    queryFn: () => systemService.decision.listRules(engineType),
    enabled: !!engineType,
  })
}

export function useCreateDecisionRule(engineType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: DecisionRuleDTO) =>
      systemService.decision.createRule(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'rules', engineType] })
      message.success('创建成功')
    },
    onError: fail('创建'),
  })
}

export function useUpdateDecisionRule(engineType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: DecisionRuleDTO }) =>
      systemService.decision.updateRule(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'rules', engineType] })
      message.success('更新成功')
    },
    onError: fail('更新'),
  })
}

export function useDeleteDecisionRule(engineType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => systemService.decision.deleteRule(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'rules', engineType] })
      message.success('删除成功')
    },
    onError: fail('删除'),
  })
}

export function useToggleDecisionRule(engineType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      systemService.decision.toggleRule(id, enabled),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'rules', engineType] })
    },
    onError: fail('启停'),
  })
}

/** POST — results are shown ad hoc in the panel, not cached. */
export function useDetectConflicts() {
  return useMutation<ConflictDTO[], unknown, string>({
    mutationFn: (engineType) => systemService.decision.detectConflicts(engineType),
    onError: fail('冲突检测'),
  })
}

// ---------- 权重方案 ----------

export function useWeightProfiles(engineType: string, scene?: string) {
  return useQuery({
    queryKey: [...ROOT, 'weights', engineType, scene ?? null],
    queryFn: () => systemService.decision.listWeights(engineType, scene),
    enabled: !!engineType,
  })
}

export function useWeightVersions(engineType: string, enabled: boolean) {
  return useQuery({
    queryKey: [...ROOT, 'versions', engineType],
    queryFn: () => systemService.decision.weightVersions(engineType),
    enabled: enabled && !!engineType,
  })
}

export function useCreateWeight(engineType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: WeightProfileDTO) =>
      systemService.decision.createWeight(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'weights', engineType] })
      qc.invalidateQueries({ queryKey: [...ROOT, 'versions', engineType] })
      message.success('创建成功')
    },
    onError: fail('创建'),
  })
}

export function useActivateWeight(engineType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => systemService.decision.activateWeight(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'weights', engineType] })
      qc.invalidateQueries({ queryKey: [...ROOT, 'versions', engineType] })
      message.success('启用成功')
    },
    onError: fail('启用'),
  })
}

export function useDeleteWeight(engineType: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => systemService.decision.deleteWeight(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [...ROOT, 'weights', engineType] })
      qc.invalidateQueries({ queryKey: [...ROOT, 'versions', engineType] })
      message.success('删除成功')
    },
    onError: fail('删除'),
  })
}
