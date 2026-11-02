'use client'

import { ReactNode } from 'react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { LayoutDashboard, LogOut, ShoppingBag, ShoppingCart, UserRound } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { mallApi } from '@/lib/api/services/ecommerce'
import { useAuthStore } from '@/stores/useAuthStore'
import { cn } from '@/lib/utils'

const NAV_ITEMS = [
  { href: '/shop', label: '商城首页' },
  { href: '/shop/cart', label: '购物车', icon: ShoppingCart },
  { href: '/shop/orders', label: '我的订单' },
  { href: '/shop/member', label: '会员中心', icon: UserRound },
]

function stripLocale(pathname: string) {
  return pathname.replace(/^\/[a-z]{2}-[A-Z]{2}/, '') || pathname
}

export default function ShopShell({ children }: { children: ReactNode }) {
  const pathname = stripLocale(usePathname() ?? '')
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)

  const cartQuery = useQuery({
    queryKey: ['shop', 'cart', user?.id],
    queryFn: () => mallApi.cart.get(user!.id),
    enabled: !!user?.id,
  })

  return (
    <div className="min-h-screen bg-[var(--color-bg-layout)]">
      <header className="border-b border-[var(--color-border-secondary)] bg-white">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-6">
          <Link href="/shop" className="flex items-center gap-2.5">
            <span className="flex size-9 items-center justify-center rounded-xl bg-[var(--color-primary)] text-white">
              <ShoppingBag className="size-4" />
            </span>
            <span className="text-lg font-semibold tracking-[-0.02em]">
              SCM
              <span className="text-[var(--color-primary-text)]"> Mall</span>
            </span>
          </Link>

          <nav className="hidden items-center gap-1 md:flex">
            {NAV_ITEMS.map((item) => {
              const active = pathname === item.href
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    'relative rounded-lg px-3 py-2 text-[13px] transition-colors',
                    active
                      ? 'bg-[var(--color-primary-soft)] font-medium text-[var(--color-primary-text)]'
                      : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-bg-sunken)] hover:text-foreground'
                  )}
                >
                  <span className="inline-flex items-center gap-1.5">
                    {item.icon && <item.icon className="size-3.5" />}
                    {item.label}
                  </span>
                  {item.href === '/shop/cart' && (cartQuery.data?.length ?? 0) > 0 && (
                    <span className="ml-1.5 rounded-full bg-[var(--color-primary)] px-1.5 py-0.5 text-[10px] leading-none text-white">
                      {cartQuery.data!.length}
                    </span>
                  )}
                </Link>
              )
            })}
          </nav>

          <div className="ml-auto flex items-center gap-2">
            <span className="hidden text-xs text-[var(--color-text-secondary)] sm:inline">
              {user?.displayName || user?.username || '未登录'}
            </span>
            <Button asChild variant="outline" size="sm">
              <Link href="/dashboard">
                <LayoutDashboard className="size-3.5" />
                运营后台
              </Link>
            </Button>
            <Button
              variant="ghost"
              size="icon"
              aria-label="退出登录"
              onClick={() => {
                logout()
                window.location.replace('/login')
              }}
            >
              <LogOut />
            </Button>
          </div>
        </div>

        <nav className="flex gap-1 overflow-x-auto px-4 pb-2 md:hidden">
          {NAV_ITEMS.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                'shrink-0 rounded-lg px-3 py-1.5 text-xs',
                pathname === item.href
                  ? 'bg-[var(--color-primary-soft)] font-medium text-[var(--color-primary-text)]'
                  : 'text-[var(--color-text-secondary)]'
              )}
            >
              {item.label}
            </Link>
          ))}
        </nav>
      </header>

      <main className="mx-auto max-w-6xl px-6 py-8">{children}</main>

      <footer className="border-t border-[var(--color-border-secondary)] py-6">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-6 text-[11px] text-[var(--color-text-tertiary)]">
          <span>SCM Mall · 多租户电商演示前台</span>
          <span>商品、购物车、订单与支付均由 scm-cloud 电商服务提供</span>
        </div>
      </footer>
    </div>
  )
}
