'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { productApiService } from '@/lib/api/services/product'
import type { ProdBrandPayload, ProdBrandQuery } from '@/lib/api/contracts/product'

const BRAND_KEY = ['product', 'brand'] as const

export function useBrandList(query: ProdBrandQuery) {
  return useQuery({
    queryKey: [...BRAND_KEY, 'list', query],
    queryFn: () => productApiService.brand.list(query),
  })
}

export function useBrandDetail(id: string) {
  return useQuery({
    queryKey: [...BRAND_KEY, 'detail', id],
    queryFn: () => productApiService.brand.get(id),
    enabled: !!id,
  })
}

export function useBrandSearch(name: string) {
  return useQuery({
    queryKey: [...BRAND_KEY, 'search', name],
    queryFn: () => productApiService.brand.search(name),
    enabled: !!name.trim(),
  })
}

export function useFeaturedBrands() {
  return useQuery({
    queryKey: [...BRAND_KEY, 'featured'],
    queryFn: () => productApiService.brand.featured(),
  })
}

export function useCreateBrand() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: ProdBrandPayload) => productApiService.brand.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: BRAND_KEY })
      message.success('创建成功')
    },
  })
}

export function useUpdateBrand() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ProdBrandPayload }) =>
      productApiService.brand.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: BRAND_KEY })
      message.success('更新成功')
    },
  })
}

export function useDeleteBrand() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => productApiService.brand.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: BRAND_KEY })
      message.success('删除成功')
    },
  })
}
