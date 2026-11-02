'use client'

import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { QUERY_KEYS } from '@/constants/query-keys'
import type {
  OrderDashboardCompare,
  OrderDashboardRange,
} from '@/lib/api/contracts/order'
import { dashboardService } from '../services/dashboard.service'

export function useDashboardStats(
  range: OrderDashboardRange,
  compare: OrderDashboardCompare
) {
  return useQuery({
    queryKey: [...QUERY_KEYS.DASHBOARD.STATS, range, compare],
    queryFn: () => dashboardService.getStats(range, compare),
    staleTime: 60 * 1000,
    refetchInterval: 5 * 60 * 1000,
    // Fail fast: the screen has its own retry affordance, and three default
    // retries would keep an outage hidden behind skeletons for ~7 seconds.
    retry: 1,
    // Keep the previous range on screen while the new aggregation loads, so
    // switching 今日/7天/30天 does not flash the layout back to skeletons.
    placeholderData: keepPreviousData,
  })
}
