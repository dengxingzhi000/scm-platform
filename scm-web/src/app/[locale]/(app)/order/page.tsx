'use client'

import { Suspense } from 'react'
import { OrderList } from '@/features/order/components'
import { Skeleton } from '@/components/ui/skeleton'

export default function OrderPage() {
  return (
    <Suspense fallback={<Skeleton className="h-64 w-full" />}>
      <OrderList />
    </Suspense>
  )
}
