import { createElement, type ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const h = vi.hoisted(() => ({
  audit: vi.fn(),
  list: vi.fn(),
  user: { id: 'u-1', username: 'zhangsan', displayName: '张三' },
}))

vi.mock('@/lib/api/services/supplier', () => ({
  supplierApiService: {
    audit: h.audit,
    list: h.list,
    stats: vi.fn(),
    page: vi.fn(),
    profile: vi.fn(),
    performance: vi.fn(),
    changeLogs: vi.fn(),
  },
}))
vi.mock('@/stores/useAuthStore', () => ({
  useAuthStore: (selector: (state: { user: unknown }) => unknown) => selector({ user: h.user }),
}))

import { useAuditSupplier, useSupplierList } from './use-supplier'
import { supplierKeys } from '../lib/query-keys'

const DEFAULT_USER = { id: 'u-1', username: 'zhangsan', displayName: '张三' }

let queryClient: QueryClient

// 这里是 .ts 文件，用 createElement 而不是 JSX —— JSX 只能出现在 .tsx 里。
const wrapper = ({ children }: { children: ReactNode }) =>
  createElement(QueryClientProvider, { client: queryClient }, children)

describe('supplier hooks', () => {
  beforeEach(() => {
    h.audit.mockReset().mockResolvedValue(undefined)
    h.list.mockReset().mockResolvedValue({ records: [], total: 0, size: 20, current: 1 })
    h.user = { ...DEFAULT_USER }
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  })

  it('sends the current user as the audit operator', async () => {
    const { result } = renderHook(() => useAuditSupplier(), { wrapper })
    await result.current.mutateAsync({ id: 's-1', action: 'APPROVE', remark: '资料齐全' })
    expect(h.audit).toHaveBeenCalledWith('s-1', {
      action: 'APPROVE',
      remark: '资料齐全',
      operatorId: 'u-1',
      operatorName: '张三',
    })
  })

  it('falls back to the username when there is no display name', async () => {
    h.user = { id: 'u-2', username: 'lisi', displayName: '' }
    const { result } = renderHook(() => useAuditSupplier(), { wrapper })
    await result.current.mutateAsync({ id: 's-1', action: 'REJECT', remark: '资料不全' })
    expect(h.audit).toHaveBeenCalledWith('s-1', expect.objectContaining({ operatorName: 'lisi' }))
  })

  it('registers the supplier list under the shared query key', async () => {
    const params = { current: 2, pageSize: 20, keyword: '华强' }
    const { result } = renderHook(() => useSupplierList(params), { wrapper })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(h.list).toHaveBeenCalledWith(expect.objectContaining({ page: 2, size: 20, keyword: '华强' }))
    expect(queryClient.getQueryCache().getAll().map((query) => query.queryKey)).toContainEqual(
      supplierKeys.list(params),
    )
  })
})
