/**
 * ID helpers for transport headers.
 *
 * `crypto.randomUUID()` is only exposed in **secure contexts**. The standalone
 * build is served over plain HTTP on a LAN address (`http://192.168.0.121:3000`),
 * so `crypto.randomUUID` is `undefined` there even though it exists on
 * `localhost`. `crypto.getRandomValues` is available in insecure contexts and
 * is the fallback; a timestamp + user-agent hash + counter is only the last
 * resort for browsers that expose no `crypto` API at all.
 */

const DEVICE_ID_STORAGE_KEY = 'scm-device-id'

let memoryDeviceId: string | undefined
let fallbackCounter = 0

function formatUuid(bytes: Uint8Array): string {
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

/**
 * Last resort for environments without any `crypto` API. This is not
 * cryptographically random, but the value only correlates request ids and binds
 * the login device id, so a timestamp, a user-agent hash and a per-page counter
 * are enough to stay unique. Throwing here would fail every API call.
 */
function createFallbackId(): string {
  const userAgent = typeof navigator === 'undefined' ? '' : navigator.userAgent
  let hash = 0
  for (let i = 0; i < userAgent.length; i += 1) {
    hash = (hash * 31 + userAgent.charCodeAt(i)) | 0
  }

  fallbackCounter += 1
  const seed = `${Date.now().toString(36)}${(hash >>> 0).toString(36)}${fallbackCounter.toString(36)}`
  const base = seed.padStart(32, '0').slice(0, 32)
  return `${base.slice(0, 8)}-${base.slice(8, 12)}-4${base.slice(12, 15)}-8${base.slice(
    15,
    18
  )}-${base.slice(18, 30)}`
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

  return createFallbackId()
}

/**
 * The backend binds each JWT to the device id it saw at login (`X-Device-ID`)
 * and rejects the token when a later request presents a different value, so it
 * must be generated once and reused.
 *
 * `localStorage` throws `SecurityError` in private mode, with cookies disabled
 * and inside some WebViews. Falling back to a module-level value keeps the id
 * stable for the lifetime of the page instead of failing every API request.
 */
export function resolveDeviceId(): string {
  if (typeof window === 'undefined') return ''

  let stored: string | null = null
  try {
    stored = window.localStorage.getItem(DEVICE_ID_STORAGE_KEY)
  } catch {
    stored = null
  }

  if (stored) {
    memoryDeviceId = stored
    return stored
  }

  if (memoryDeviceId) return memoryDeviceId

  const generated = createRandomId().replace(/-/g, '')
  memoryDeviceId = generated
  try {
    window.localStorage.setItem(DEVICE_ID_STORAGE_KEY, generated)
  } catch {
    // Storage is unavailable; the in-memory value above is the fallback.
  }
  return generated
}
