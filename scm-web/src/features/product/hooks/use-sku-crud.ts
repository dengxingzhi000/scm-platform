'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { productApiService } from '@/lib/api/services/product'
import type { ProdSkuPayload, ProdSkuQuery, ProdSpuQuery } from '@/lib/api/contracts/product'

const SKU_KEY = ['product', 'sku'] as const

export function useSkuList(query: ProdSkuQuery) {
  return useQuery({
    queryKey: [...SKU_KEY, 'list', query],
    queryFn: () => productApiService.sku.list(query),
  })
}

export function useSkuDetail(id: string) {
  return useQuery({
    queryKey: [...SKU_KEY, 'detail', id],
    queryFn: () => productApiService.sku.get(id),
    enabled: !!id,
  })
}

export function useSkusBySpu(spuId: string) {
  return useQuery({
    queryKey: [...SKU_KEY, 'by-spu', spuId],
    queryFn: () => productApiService.listSkusBySpu(spuId),
    enabled: !!spuId,
  })
}

export function useSpuOptions(query: ProdSpuQuery) {
  return useQuery({
    queryKey: ['product', 'spu', 'options', query],
    queryFn: () => productApiService.querySpus(query),
  })
}

export function useCreateSku() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: ProdSkuPayload) => productApiService.sku.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: SKU_KEY })
      message.success('创建成功')
    },
  })
}

export function useUpdateSku() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ProdSkuPayload }) =>
      productApiService.sku.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: SKU_KEY })
      message.success('更新成功')
    },
  })
}

export function useDeleteSku() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => productApiService.sku.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: SKU_KEY })
      message.success('删除成功')
    },
  })
}
