'use client'

import { useMutation, useQuery } from '@tanstack/react-query'
import { analyticsService } from '../services/analytics.service'
import type { DatasetQueryRequest } from '../types'

export function useDatasets() {
  return useQuery({
    queryKey: ['analytics', 'datasets'],
    queryFn: () => analyticsService.listDatasets(),
    staleTime: 5 * 60 * 1000,
  })
}

export function useDatasetFields(datasetId?: string) {
  return useQuery({
    queryKey: ['analytics', 'fields', datasetId],
    queryFn: () => analyticsService.listFields(datasetId as string),
    enabled: Boolean(datasetId),
    staleTime: 5 * 60 * 1000,
  })
}

export function useAnalyticsQuery() {
  return useMutation({
    mutationFn: (request: DatasetQueryRequest) => analyticsService.query(request),
  })
}
