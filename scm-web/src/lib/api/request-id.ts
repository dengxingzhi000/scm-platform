/**
 * ID helpers for transport headers.
 *
 * `crypto.randomUUID()` is only exposed in **secure contexts**. The standalone
 * build is served over plain HTTP on a LAN address (`http://192.168.0.121:3000`),
 * so `crypto.randomUUID` is `undefined` there even though it exists on
 * `localhost`. `crypto.getRandomValues` is available in insecure contexts and
 * is the fallback; Math.random is only the last resort for very old browsers.
 */

const DEVICE_ID_STORAGE_KEY = 'scm-device-id'

function formatUuid(bytes: Uint8Array): string {
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

/** RFC 4122 v4 UUID, without assuming a secure context. */
export function createRandomId(): string {
  const cryptoApi = typeof globalThis === 'undefined' ? undefined : globalThis.crypto

  if (typeof cryptoApi?.randomUUID === 'function') {
    return cryptoApi.randomUUID()
  }

  if (typeof cryptoApi?.getRandomValues === 'function') {
    const bytes = new Uint8Array(16)
    cryptoApi.getRandomValues(bytes)
    bytes[6] = (bytes[6] & 0x0f) | 0x40
    bytes[8] = (bytes[8] & 0x3f) | 0x80
    return formatUuid(bytes)
  }

  const base = Array.from({ length: 32 }, () =>
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
  return `${base.slice(0, 8)}-${base.slice(8, 12)}-4${base.slice(13, 16)}-8${base.slice(
    17,
    20
  )}-${base.slice(20)}`
}

/**
 * The backend binds each JWT to the device id it saw at login (`X-Device-ID`)
 * and rejects the token when a later request presents a different value, so it
 * must be generated once and reused.
 */
export function resolveDeviceId(): string {
  if (typeof window === 'undefined') return ''
  const stored = window.localStorage.getItem(DEVICE_ID_STORAGE_KEY)
  if (stored) return stored
  const generated = createRandomId().replace(/-/g, '')
  window.localStorage.setItem(DEVICE_ID_STORAGE_KEY, generated)
  return generated
}
