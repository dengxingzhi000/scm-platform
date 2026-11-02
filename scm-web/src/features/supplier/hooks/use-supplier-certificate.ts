'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { toUiPage } from '@/lib/api/contracts/page'
import type { SupplierCertificateDTO } from '@/lib/api/contracts/supplier'
import { supplierApiService } from '@/lib/api/services/supplier'

import { toSupplierCertificate } from '../lib/mappers'
import {
  supplierCategoryKeys,
  supplierCertificateExpiryKeys,
  supplierKeys,
} from '../lib/query-keys'
import { useOperator } from './use-operator'

/** 到期预警一页的条数 —— 后端默认 10，前端列表统一用 20。 */
const EXPIRY_PAGE_SIZE = 20

export function useSupplierCertificates(supplierId: string) {
  return useQuery({
    queryKey: supplierKeys.certificates(supplierId),
    queryFn: async () =>
      (await supplierApiService.certificates.list(supplierId)).map(toSupplierCertificate),
    enabled: !!supplierId,
  })
}

export function useExpiringCertificates(days: number, page: number) {
  return useQuery({
    queryKey: supplierCertificateExpiryKeys.expiring(days, page),
    queryFn: async () =>
      toUiPage(
        await supplierApiService.certificateExpiry.expiring({
          page,
          size: EXPIRY_PAGE_SIZE,
          days,
        }),
        toSupplierCertificate,
      ),
  })
}

export function useCreateSupplierCertificate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { supplierId: string; data: SupplierCertificateDTO }) =>
      supplierApiService.certificates.create(vars.supplierId, vars.data),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierKeys.certificates(vars.supplierId) })
      qc.invalidateQueries({ queryKey: supplierCertificateExpiryKeys.all })
    },
  })
}

export function useUpdateSupplierCertificate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { supplierId: string; id: string; data: SupplierCertificateDTO }) =>
      supplierApiService.certificates.update(vars.supplierId, vars.id, vars.data),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierKeys.certificates(vars.supplierId) })
      qc.invalidateQueries({ queryKey: supplierCertificateExpiryKeys.all })
    },
  })
}

export function useVerifySupplierCertificate() {
  const qc = useQueryClient()
  const { operatorId, operatorName } = useOperator()
  return useMutation({
    mutationFn: (vars: {
      supplierId: string
      id: string
      approved: boolean
      remark?: string
    }) =>
      supplierApiService.certificates.verify(vars.supplierId, vars.id, {
        approved: vars.approved,
        operatorId,
        operatorName,
        remark: vars.remark,
      }),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierKeys.certificates(vars.supplierId) })
      qc.invalidateQueries({ queryKey: supplierCertificateExpiryKeys.all })
    },
  })
}

export function useDeleteSupplierCertificate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { supplierId: string; id: string }) =>
      supplierApiService.certificates.remove(vars.supplierId, vars.id),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierKeys.certificates(vars.supplierId) })
      qc.invalidateQueries({ queryKey: supplierCertificateExpiryKeys.all })
    },
  })
}
