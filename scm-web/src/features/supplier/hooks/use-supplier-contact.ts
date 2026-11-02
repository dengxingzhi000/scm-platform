'use client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { SupplierContactDTO } from '@/lib/api/contracts/supplier'
import { supplierApiService } from '@/lib/api/services/supplier'

import { toSupplierContact } from '../lib/mappers'
import { supplierCategoryKeys, supplierKeys } from '../lib/query-keys'

export function useSupplierContacts(supplierId: string) {
  return useQuery({
    queryKey: supplierKeys.contacts(supplierId),
    queryFn: async () =>
      (await supplierApiService.contacts.list(supplierId)).map(toSupplierContact),
    enabled: !!supplierId,
  })
}

export function useCreateSupplierContact() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { supplierId: string; data: SupplierContactDTO }) =>
      supplierApiService.contacts.create(vars.supplierId, vars.data),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierKeys.contacts(vars.supplierId) })
    },
  })
}

export function useUpdateSupplierContact() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { supplierId: string; id: string; data: SupplierContactDTO }) =>
      supplierApiService.contacts.update(vars.supplierId, vars.id, vars.data),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierKeys.contacts(vars.supplierId) })
    },
  })
}

export function useDeleteSupplierContact() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { supplierId: string; id: string }) =>
      supplierApiService.contacts.remove(vars.supplierId, vars.id),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierKeys.contacts(vars.supplierId) })
    },
  })
}

export function useSetPrimarySupplierContact() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { supplierId: string; id: string }) =>
      supplierApiService.contacts.setPrimary(vars.supplierId, vars.id),
    onSuccess: (_result, vars) => {
      qc.invalidateQueries({ queryKey: supplierKeys.all })
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all })
      qc.invalidateQueries({ queryKey: supplierKeys.contacts(vars.supplierId) })
    },
  })
}
