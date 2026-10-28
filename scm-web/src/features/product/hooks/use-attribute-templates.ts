'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { toApiError } from '@/lib/api/envelope'
import { productApiService } from '@/lib/api/services/product'
import type {
  ProdAttributeTemplatePayload,
  ProdAttributeTemplateQuery,
} from '@/lib/api/contracts/product'

const TEMPLATE_KEY = ['product', 'attribute-template'] as const

export function useAttributeTemplatePage(query: ProdAttributeTemplateQuery) {
  return useQuery({
    queryKey: [...TEMPLATE_KEY, 'list', query],
    // The envelope is `Void`-shaped for empty result sets, so normalise the record list.
    queryFn: () =>
      productApiService.attributeTemplate
        .list(query)
        .then((page) => ({ ...page, records: page.records ?? [] })),
  })
}

export function useAttributeTemplatesByCategory(categoryId: string | null) {
  return useQuery({
    queryKey: [...TEMPLATE_KEY, 'by-category', categoryId],
    queryFn: () =>
      productApiService.attributeTemplate
        .byCategory(categoryId as string)
        .then((records) => records ?? []),
    enabled: !!categoryId,
  })
}

export function useCreateAttributeTemplate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: ProdAttributeTemplatePayload) =>
      productApiService.attributeTemplate.create(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: TEMPLATE_KEY })
      message.success('属性模板创建成功')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}

export function useUpdateAttributeTemplate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      payload,
    }: {
      id: string
      payload: ProdAttributeTemplatePayload
    }) => productApiService.attributeTemplate.update(id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: TEMPLATE_KEY })
      message.success('属性模板更新成功')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}

export function useDeleteAttributeTemplate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) =>
      productApiService.attributeTemplate.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: TEMPLATE_KEY })
      message.success('属性模板删除成功')
    },
    onError: (error) => message.error(toApiError(error).message),
  })
}

/**
 * `attributes` is an opaque JSON string the backend stores verbatim, so the
 * only check that is possible is syntactic. Returns the parse error text, or
 * `null` when the value is valid JSON.
 */
export function getAttributesJsonError(value: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  try {
    JSON.parse(trimmed)
    return null
  } catch (error) {
    return error instanceof Error ? error.message : 'JSON 解析失败'
  }
}
