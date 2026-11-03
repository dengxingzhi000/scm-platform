'use client'

import { Suspense } from 'react'
import { Skeleton } from '@/components/ui/skeleton'
import { SupplierList } from '@/features/supplier/components'

export default function SupplierPage() {
  return (
    <Suspense fallback={<Skeleton className="h-64 w-full" />}>
      <SupplierList />
    </Suspense>
  )
}