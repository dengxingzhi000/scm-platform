/**
 * Header helper for the API proxy hop.
 *
 * The browser calls the gateway paths same-origin, but `next.config` rewrites
 * forward the original request (including `Origin`) to the backend. Spring's CORS
 * allow-list only covers localhost, so a LAN deployment such as
 * http://192.168.0.121:3000 gets `403 Invalid CORS request` unless the
 * same-origin `Origin` is dropped.
 *
 * Only a same-origin `Origin` is removed: a genuinely cross-origin caller keeps
 * the header so the backend CORS policy can still accept or reject it.
 */
export function stripSameOriginHeader(headers: Headers): void {
  const origin = headers.get('origin')
  const host = headers.get('host')
  if (!origin || !host) return

  let originHost: string
  try {
    originHost = new URL(origin).host
  } catch {
    return
  }

  if (originHost.toLowerCase() === host.toLowerCase()) {
    headers.delete('origin')
  }
}
