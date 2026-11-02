'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'

import { toApiError } from '@/lib/api/envelope'
import { logisticsApiService } from '@/lib/api/services/logistics'
import type {
  TmsCarrierDTO,
  TmsCarrierQuery,
  TmsTrackingDTO,
  TmsTrackingQuery,
} from '@/lib/api/contracts/logistics'

import { logisticsService } from '../services/logistics.service'

export function useWaybillList() {
  return useQuery({ queryKey: ['logistics', 'waybills'], queryFn: () => logisticsService.listWaybills() })
}
export function useTrackingEvents(waybillId: string) {
  return useQuery({ queryKey: ['logistics', 'tracking', waybillId], queryFn: () => logisticsService.getTracking(waybillId), enabled: !!waybillId })
}

const fail = (error: unknown) => message.error(toApiError(error).message)

export function useTrackingPage(query: TmsTrackingQuery) {
  return useQuery({
    queryKey: ['logistics', 'tracking', 'page', query],
    queryFn: () => logisticsApiService.pageTracking(query),
  })
}

export function useCreateTracking() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (entity: Partial<TmsTrackingDTO>) =>
      logisticsApiService.createTracking(entity),
    onSuccess: () => {
      message.success('物流轨迹已上报')
      qc.invalidateQueries({ queryKey: ['logistics', 'tracking'] })
    },
    onError: fail,
  })
}

export function useCarrierPage(query: TmsCarrierQuery) {
  return useQuery({
    queryKey: ['logistics', 'carrier', query],
    queryFn: () => logisticsApiService.pageCarriers(query),
  })
}

export function useSaveCarrier() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (entity: Partial<TmsCarrierDTO>) =>
      entity.id
        ? logisticsApiService.updateCarrier(entity.id, entity)
        : logisticsApiService.createCarrier(entity),
    onSuccess: () => {
      message.success('承运商已保存')
      qc.invalidateQueries({ queryKey: ['logistics', 'carrier'] })
    },
    onError: fail,
  })
}

export function useDeleteCarrier() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => logisticsApiService.deleteCarrier(id),
    onSuccess: () => {
      message.success('承运商已删除')
      qc.invalidateQueries({ queryKey: ['logistics', 'carrier'] })
    },
    onError: fail,
  })
}

export function useToggleCarrier() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      logisticsApiService.setCarrierEnabled(id, enabled),
    onSuccess: (_data, variables) => {
      message.success(variables.enabled ? '承运商已启用' : '承运商已停用')
      qc.invalidateQueries({ queryKey: ['logistics', 'carrier'] })
    },
    onError: fail,
  })
}
