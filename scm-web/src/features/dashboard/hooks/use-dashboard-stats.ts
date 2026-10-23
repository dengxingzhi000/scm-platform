'use client'

import { useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/constants/query-keys'
import { dashboardService } from '../services/dashboard.service'

export function useDashboardStats() {
  return useQuery({
    queryKey: QUERY_KEYS.DASHBOARD.STATS,
    queryFn: () => dashboardService.getStats(),
    staleTime: 60 * 1000,
    refetchInterval: 5 * 60 * 1000,
  })
}
