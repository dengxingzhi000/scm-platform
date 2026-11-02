'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { SupplierBankAccountDTO } from '@/lib/api/contracts/supplier'
import { supplierApiService } from '@/lib/api/services/supplier'

import { toSupplierBankAccount } from '../lib/mappers'
import { supplierCategoryKeys, supplierKeys } from '../lib/query-keys'

export function useSupplierBankAccounts(supplierId: string) {
  return useQuery({
    queryKey: supplierKeys.bankAccounts(supplierId),
    queryFn: async () =>
      (await supplierApiService.bankAccounts.list(supplierId)).map(toSupplierBankAccount),
    enabled: !!supplierId,
  })
}

export function useCreateSupplierBankAccount() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { supplierId: string; data: SupplierBankAccountDTO }) =>
      supplierApiService.bankAccounts.create(vars.supplierId, vars.data),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierKeys.bankAccounts(vars.supplierId) })
    },
  })
}

export function useUpdateSupplierBankAccount() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { supplierId: string; id: string; data: SupplierBankAccountDTO }) =>
      supplierApiService.bankAccounts.update(vars.supplierId, vars.id, vars.data),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierKeys.bankAccounts(vars.supplierId) })
    },
  })
}

export function useDeleteSupplierBankAccount() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { supplierId: string; id: string }) =>
      supplierApiService.bankAccounts.remove(vars.supplierId, vars.id),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierKeys.bankAccounts(vars.supplierId) })
    },
  })
}

export function useSetDefaultSupplierBankAccount() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { supplierId: string; id: string }) =>
      supplierApiService.bankAccounts.setDefault(vars.supplierId, vars.id),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierKeys.bankAccounts(vars.supplierId) })
    },
  })
}
