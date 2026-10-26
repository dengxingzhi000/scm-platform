'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { userService } from '../services/user.service'
import type { PageParams } from '../types'

export function useUserList(params?: PageParams) {
  return useQuery({ queryKey: ['users', 'list', params], queryFn: () => userService.list(params) })
}
export function useUserDetail(id: string) {
  return useQuery({ queryKey: ['users', 'detail', id], queryFn: () => userService.getById(id), enabled: !!id })
}
export function useCreateUser() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (data: Parameters<typeof userService.create>[0]) => userService.create(data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['users'] }); message.success('创建成功') } })
}
export function useUpdateUser() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ id, data }: { id: string; data: Parameters<typeof userService.update>[1] }) => userService.update(id, data), onSuccess: () => { qc.invalidateQueries({ queryKey: ['users'] }); message.success('更新成功') } })
}
export function useDeleteUser() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: (id: string) => userService.delete(id), onSuccess: () => { qc.invalidateQueries({ queryKey: ['users'] }); message.success('删除成功') } })
}
export function useResetUserPassword() {
  return useMutation({ mutationFn: (id: string) => userService.resetPassword(id), onSuccess: () => { message.success('密码已重置') } })
}
export function useLockUser() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ id, lock }: { id: string; lock: boolean }) => userService.lock(id, lock), onSuccess: () => { qc.invalidateQueries({ queryKey: ['users'] }); message.success('状态已更新') } })
}
export function useGrantUserRoles() {
  const qc = useQueryClient()
  return useMutation({ mutationFn: ({ id, roleIds }: { id: string; roleIds: string[] }) => userService.grantRoles(id, roleIds), onSuccess: () => { qc.invalidateQueries({ queryKey: ['users'] }); message.success('角色已更新') } })
}
export function useForceLogoutUser() {
  return useMutation({ mutationFn: ({ id, reason }: { id: string; reason: string }) => userService.forceLogout(id, reason), onSuccess: () => { message.success('用户已强制下线') } })
}
