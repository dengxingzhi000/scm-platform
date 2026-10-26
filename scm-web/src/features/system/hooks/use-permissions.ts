'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { permissionService } from '../services/permission.service'

export function usePermissionList() {
  return useQuery({ queryKey: ['permissions'], queryFn: () => permissionService.list() })
}
export function useCreatePermission() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (data: Parameters<typeof permissionService.create>[0]) => permissionService.create(data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['permissions'] }); message.success('创建成功') } })
}
export function useUpdatePermission() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ id, data }: { id: string; data: Parameters<typeof permissionService.update>[1] }) => permissionService.update(id, data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['permissions'] }); message.success('更新成功') } })
}
export function useDeletePermission() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (id: string) => permissionService.delete(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['permissions'] }); message.success('删除成功') } })
}
