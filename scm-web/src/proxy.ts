import createMiddleware from 'next-intl/middleware'
import { routing } from './i18n/routing'
import { NextRequest, NextResponse } from 'next/server'

const intlMiddleware = createMiddleware(routing)

const publicPaths = ['/login', '/register', '/forgot-password']

// Next.js 16 renamed the `middleware` convention to `proxy`.
export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl

  // Extract locale from pathname
  const localeMatch = pathname.match(/^\/([a-z]{2}-[A-Z]{2})(\/.*)?$/)
  const locale = localeMatch ? localeMatch[1] : null
  const pathWithoutLocale = localeMatch
    ? localeMatch[2] || '/'
    : pathname

  // Check if it's a public path
  const isPublicPath = publicPaths.some(
    (p) => pathWithoutLocale === p || pathWithoutLocale.startsWith(p + '/')
  )

  // Check for auth token (simplified - in production, verify JWT)
  const token = request.cookies.get('access_token')?.value

  // If not authenticated and not on public path, redirect to login
  if (!token && !isPublicPath && !pathWithoutLocale.startsWith('/api')) {
    const loginUrl = locale ? `/${locale}/login` : '/login'
    return NextResponse.redirect(new URL(loginUrl, request.url))
  }

  // If authenticated and on public path, redirect to dashboard
  if (token && isPublicPath) {
    const dashboardUrl = locale ? `/${locale}/dashboard` : '/dashboard'
    return NextResponse.redirect(new URL(dashboardUrl, request.url))
  }

  // Apply i18n middleware
  return intlMiddleware(request)
}

export const config = {
  matcher: ['/((?!api|_next|_vercel|.*\\..*).*)'],
}
