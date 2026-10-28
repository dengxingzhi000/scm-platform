'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { toApiError } from '@/lib/api/envelope'
import { productApiService } from '@/lib/api/services/product'
import type {
  ProdCategoryPayload,
  ProdCategoryQuery,
} from '@/lib/api/contracts/product'

/** The backend marks a root category with the `"0"` parent sentinel. */
export const ROOT_PARENT_ID = '0'

/** One large page keeps the whole catalog in a single round trip (same trick as `listCategories`). */
const CATALOG_PAGE_SIZE = 1000

const CATEGORY_KEY = ['product', 'category'] as const

export function useCategoryPage(query: ProdCategoryQuery) {
  return useQuery({
    queryKey: [...CATEGORY_KEY, 'list', query],
    queryFn: () =>
      productApiService.category
        .list(query)
        .then((page) => ({ ...page, records: page.records ?? [] })),
  })
}

/** Flat catalog used to fill category Selects — `tree()` only returns roots, so the list endpoint is used. */
export function useCategoryOptions() {
  return useQuery({
    queryKey: [...CATEGORY_KEY, 'options'],
    queryFn: () =>
      productApiService.category
        .list({ page: 1, size: CATALOG_PAGE_SIZE })
        .then((page) => page.records ?? []),
  })
}

export function useCategoryChildren(parentId: string | null) {
  return useQuery({
    queryKey: [...CATEGORY_KEY, 'children', parentId],
    // The root view is served by the paged list, so the drill-down query only
    // runs once a parent id is selected.
    queryFn: () =>
      productApiService.category
        .children(parentId as string)
        .then((records) => records ?? []),
    enabled: !!parentId,
  })
}

export function useCreateCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: ProdCategoryPayload) =>
      productApiService.category.create(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CATEGORY_KEY })
      message.success('分类创建成功')
    },
    // Deleting/creating can be rejected by the backend (duplicate code, parent
    // with children, …) — always surface the real reason.
    onError: (error) => message.error(toApiError(error).message),
  })
}

export function useUpdateCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: ProdCategoryPayload }) =>
      productApiService.category.update(id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CATEGORY_KEY })
      message.success('分类更新成功')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}

export function useDeleteCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => productApiService.category.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: CATEGORY_KEY })
      message.success('分类删除成功')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}
