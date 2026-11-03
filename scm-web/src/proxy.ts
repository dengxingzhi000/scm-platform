import createMiddleware from 'next-intl/middleware'
import { routing } from './i18n/routing'
import { NextRequest, NextResponse } from 'next/server'

const intlMiddleware = createMiddleware(routing)

const publicPaths = ['/login', '/register', '/forgot-password']

const API_PREFIXES = ['/auth', '/common', '/system/api']

function isApiRequest(pathname: string): boolean {
  return (
    pathname === '/api' ||
    pathname.startsWith('/api/') ||
    pathname.startsWith('/scm-') ||
    API_PREFIXES.some(
      (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`)
    )
  )
}

/**
 * The browser calls these paths same-origin, but next.config rewrites forward the
 * original request (including `Origin`) to the backend. Spring's CORS allow-list
 * only covers localhost, so a LAN deployment such as http://192.168.0.121:3000
 * gets `403 Invalid CORS request`. This is a server-side proxy hop, not a browser
 * cross-origin call, so drop `Origin` before the rewrite runs.
 */
function forwardWithoutOrigin(request: NextRequest) {
  const headers = new Headers(request.headers)
  headers.delete('origin')
  return NextResponse.next({ request: { headers } })
}

// Next.js 16 renamed the `middleware` convention to `proxy`.
export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl

  if (isApiRequest(pathname)) {
    return forwardWithoutOrigin(request)
  }

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
  // API prefixes must reach this proxy so the forwarded request can drop
  // `Origin`; page routes still go through the locale + auth middleware.
  matcher: ['/((?!_next|_vercel|.*\\..*).*)'],
}
