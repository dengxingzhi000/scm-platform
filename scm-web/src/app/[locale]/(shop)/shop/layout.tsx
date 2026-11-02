import { ReactNode } from 'react'

import ShopShell from '@/features/shop/components/shop-shell'

export default function ShopLayout({ children }: { children: ReactNode }) {
  return <ShopShell>{children}</ShopShell>
}
