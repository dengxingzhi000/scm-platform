'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'

import { QUERY_KEYS } from '@/constants/query-keys'

import { notificationService } from '../services/notification.service'
import type { NotificationSearchParams } from '../types'

export function useNotificationList(
  params?: NotificationSearchParams,
  options?: { enabled?: boolean; staleTime?: number },
) {
  return useQuery({
    queryKey: QUERY_KEYS.NOTIFICATION.LIST(params),
    queryFn: () => notificationService.list(params),
    enabled: options?.enabled ?? true,
    staleTime: options?.staleTime,
  })
}

export function useUnreadCount() {
  return useQuery({
    queryKey: QUERY_KEYS.NOTIFICATION.UNREAD_COUNT,
    queryFn: () => notificationService.getUnreadCount(),
    refetchInterval: 60_000,
  })
}

export function useMarkAsRead() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => notificationService.markAsRead(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: QUERY_KEYS.NOTIFICATION.ALL }) },
  })
}

export function useMarkAllAsRead() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.NOTIFICATION.ALL })
      message.success('已全部标为已读')
    },
  })
}

export function useDeleteNotification() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => notificationService.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.NOTIFICATION.ALL })
      message.success('通知已删除')
    },
  })
}

export function useDeleteNotifications() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (ids: string[]) => notificationService.removeMany(ids),
    onSuccess: (deleted) => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.NOTIFICATION.ALL })
      message.success(`已删除 ${deleted} 条通知`)
    },
  })
}
