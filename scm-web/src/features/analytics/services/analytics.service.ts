import apiClient from '@/lib/api/client'
import type {
  DatasetDefinition,
  DatasetFieldDefinition,
  DatasetQueryRequest,
  DatasetQueryResult,
  PageData,
} from '../types'

interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

const get = <T>(url: string, params?: Record<string, unknown>) =>
  apiClient.get(url, { params }) as unknown as Promise<ApiEnvelope<T>>

const post = <T>(url: string, body: unknown) =>
  apiClient.post(url, body) as unknown as Promise<ApiEnvelope<T>>

export const analyticsService = {
  listDatasets: async (params?: { page?: number; size?: number; keyword?: string }) => {
    const response = await get<PageData<DatasetDefinition>>('/api/v1/analytics/datasets', {
      page: params?.page ?? 1,
      size: params?.size ?? 100,
      keyword: params?.keyword,
    })
    return response.data
  },

  listFields: async (datasetId: string) => {
    const response = await get<DatasetFieldDefinition[]>(
      `/api/v1/analytics/datasets/${datasetId}/fields`,
    )
    return response.data
  },

  query: async (request: DatasetQueryRequest) => {
    const response = await post<DatasetQueryResult>('/api/v1/analytics/query', request)
    return response.data
  },
}
