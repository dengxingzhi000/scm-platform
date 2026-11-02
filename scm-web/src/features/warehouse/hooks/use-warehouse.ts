'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'

import { toApiError } from '@/lib/api/envelope'
import { warehouseApiService } from '@/lib/api/services/warehouse'
import type {
  WmsWavePickingDTO,
  WmsWavePickingQuery,
} from '@/lib/api/contracts/warehouse'

import { warehouseService } from '../services/warehouse.service'

export function useWarehouseList() {
  return useQuery({ queryKey: ['warehouses'], queryFn: () => warehouseService.listWarehouses() })
}
export function useInboundList() {
  return useQuery({ queryKey: ['warehouse', 'inbound'], queryFn: () => warehouseService.listInbound() })
}
export function useOutboundList() {
  return useQuery({ queryKey: ['warehouse', 'outbound'], queryFn: () => warehouseService.listOutbound() })
}

const fail = (error: unknown) => message.error(toApiError(error).message)

export function useWavePickingPage(query: WmsWavePickingQuery) {
  return useQuery({
    queryKey: ['warehouse', 'wave-picking', query],
    queryFn: () => warehouseApiService.pageWavePicking(query),
  })
}

export function useCreateWavePicking() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (entity: Partial<WmsWavePickingDTO>) =>
      warehouseApiService.createWavePicking(entity),
    onSuccess: () => {
      message.success('波次拣货单已创建')
      qc.invalidateQueries({ queryKey: ['warehouse', 'wave-picking'] })
    },
    onError: fail,
  })
}

export function useStartPicking() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, pickerId, pickerName }: { id: string; pickerId: string; pickerName: string }) =>
      warehouseApiService.startPicking(id, pickerId, pickerName),
    onSuccess: () => {
      message.success('已开始拣货')
      qc.invalidateQueries({ queryKey: ['warehouse', 'wave-picking'] })
    },
    onError: fail,
  })
}

export function useCompletePicking() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, operatorId }: { id: string; operatorId: string }) =>
      warehouseApiService.completePicking(id, operatorId),
    onSuccess: () => {
      message.success('拣货已完成')
      qc.invalidateQueries({ queryKey: ['warehouse', 'wave-picking'] })
    },
    onError: fail,
  })
}

export function useCancelWavePicking() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, operatorId }: { id: string; operatorId: string }) =>
      warehouseApiService.cancelWavePicking(id, operatorId),
    onSuccess: () => {
      message.success('波次已取消')
      qc.invalidateQueries({ queryKey: ['warehouse', 'wave-picking'] })
    },
    onError: fail,
  })
}

export function useRemoveWavePicking() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => warehouseApiService.removeWavePicking(id),
    onSuccess: () => {
      message.success('波次已删除')
      qc.invalidateQueries({ queryKey: ['warehouse', 'wave-picking'] })
    },
    onError: fail,
  })
}
