'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { dictService } from '../services/dict.service'

export function useDictTypeList() {
  return useQuery({ queryKey: ['dict', 'types'], queryFn: () => dictService.listTypes() })
}
export function useDictItems(dictCode: string) {
  return useQuery({ queryKey: ['dict', 'items', dictCode], queryFn: () => dictService.listItems(dictCode), enabled: !!dictCode })
}
export function useCreateDictType() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (data: Parameters<typeof dictService.createType>[0]) => dictService.createType(data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['dict', 'types'] }); message.success('创建成功') } })
}
export function useUpdateDictType() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ id, data }: { id: string; data: Parameters<typeof dictService.updateType>[1] }) => dictService.updateType(id, data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['dict', 'types'] }); message.success('更新成功') } })
}
export function useDeleteDictType() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (id: string) => dictService.deleteType(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['dict'] }); message.success('删除成功') } })
}
export function useCreateDictItem() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ data, dictTypeId, dictCode }: { data: Parameters<typeof dictService.createItem>[0]; dictTypeId: string; dictCode: string }) =>
      dictService.createItem(data, dictTypeId, dictCode),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dict', 'items'] }); message.success('创建成功') },
  })
}
export function useUpdateDictItem() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ data, dictTypeId, dictCode }: { data: Parameters<typeof dictService.updateItem>[0]; dictTypeId: string; dictCode: string }) =>
      dictService.updateItem(data, dictTypeId, dictCode),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dict', 'items'] }); message.success('更新成功') },
  })
}
export function useDeleteDictItem() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (id: string) => dictService.deleteItem(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['dict', 'items'] }); message.success('删除成功') } })
}
