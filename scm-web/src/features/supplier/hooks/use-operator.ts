'use client'
import { useAuthStore } from '@/stores/useAuthStore'

/**
 * 后端 8 个写接口从 query 上收 operatorId / operatorName,
 * 这里统一从登录态里取当前用户,免得每个页面各写一遍。
 */
export function useOperator(): { operatorId?: string; operatorName?: string } {
  const user = useAuthStore((state) => state.user)
  return {
    operatorId: user?.id,
    operatorName: user?.displayName || user?.username || undefined,
  }
}
