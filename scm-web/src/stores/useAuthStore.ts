import { create } from 'zustand'

export interface User {
  id: string
  username: string
  displayName: string
  email?: string
  avatar?: string
  roles?: string[]
}

/**
 * 记住模式的标记 cookie:过期时间与 access_token 一致(都是 7 天),两边不会各走各的。
 */
const PERSIST_COOKIE = 'access_token_persist'

/**
 * 写 cookie。`days` 省略时写**会话 cookie**(不设 expires)——
 * 刷新页面仍然保持登录,关掉浏览器就失效。
 */
function setCookie(name: string, value: string, days?: number) {
  const expires = days === undefined ? '' : `; expires=${new Date(Date.now() + days * 864e5).toUTCString()}`
  document.cookie = `${name}=${encodeURIComponent(value)}${expires}; path=/; SameSite=Lax`
}

function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : null
}

function deleteCookie(name: string) {
  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`
}

interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  permissions: string[]
  isAuthenticated: boolean
  isLoading: boolean
  /**
   * 记住模式的可读镜像:access_token cookie 是 7 天持久 cookie 还是会话 cookie。
   * 只作展示用 —— 真正的判断以标记 cookie 为准(这个值只活在内存里,刷新后会复位)。
   */
  rememberMe: boolean
  login: (user: User, accessToken: string, refreshToken: string, remember?: boolean) => void
  setTokens: (accessToken: string, refreshToken: string) => void
  logout: () => void
  setUser: (user: User) => void
  setPermissions: (permissions: string[]) => void
  setLoading: (loading: boolean) => void
  hasPermission: (code: string) => boolean
  initialize: () => Promise<void>
}

/**
 * 当前会话是不是持久会话 —— 以标记 cookie 为准。
 * 标记 cookie 与 token cookie 同生命周期,页面刷新后依然可信;
 * store 里的 rememberMe 只活在内存里,刷新后会回到默认的 true,拿它当兜底
 * 会把「不记住我」的会话又写成 7 天持久 cookie。
 */
function currentPersistMode(): boolean {
  return getCookie(PERSIST_COOKIE) !== null
}

export const useAuthStore = create<AuthState>()((set, get) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  permissions: [],
  isAuthenticated: false,
  isLoading: true,
  rememberMe: true,

  login: (user, accessToken, refreshToken, remember) => {
    // `remember` 省略时以标记 cookie 为准(见 currentPersistMode)。
    const persist = remember ?? currentPersistMode()
    set({ user, accessToken, refreshToken, isAuthenticated: true, isLoading: false, rememberMe: persist })
    // 不记住我 → 会话 cookie(没有 expires)。同名同 path 的 cookie 会被浏览器整体替换,
    // 所以用户「先勾选后取消」时,之前那个 7 天的持久 cookie 也随之消失。
    setCookie('access_token', accessToken, persist ? 7 : undefined)
    if (refreshToken) setCookie('refresh_token', refreshToken, persist ? 7 : undefined)
    // 记住模式本身也要活过页面刷新 —— 401 刷新路径写 cookie 时读的就是这个标记。
    // 标记 cookie 的生命周期与 token 一致,所以两边不会各走各的。
    if (persist) {
      setCookie(PERSIST_COOKIE, '1', 7)
    } else {
      deleteCookie(PERSIST_COOKIE)
    }
  },

  // Refresh runs after a full page reload, where `user` is still null — tokens must be
  // persisted without requiring a profile.
  // 轮换出来的 token 必须沿用当前的记住模式,否则「不记住我」的会话会在刷新时被
  // 悄悄升级成 7 天持久 cookie。
  setTokens: (accessToken, refreshToken) => {
    const persist = currentPersistMode()
    set({ accessToken, refreshToken, isAuthenticated: true, rememberMe: persist })
    setCookie('access_token', accessToken, persist ? 7 : undefined)
    if (refreshToken) setCookie('refresh_token', refreshToken, persist ? 7 : undefined)
    // 标记 cookie 与 token 一起滑动 —— 两者必须同时到期,否则标记会先消失。
    if (persist) setCookie(PERSIST_COOKIE, '1', 7)
  },

  logout: () => {
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      permissions: [],
      isAuthenticated: false,
      isLoading: false,
      rememberMe: true,
    })
    deleteCookie('access_token')
    deleteCookie('refresh_token')
    // token 没了,标记也得跟着走 —— 否则下次登录会平白继承「记住我」。
    deleteCookie(PERSIST_COOKIE)
  },

  setUser: (user) => set({ user }),
  setPermissions: (permissions) => set({ permissions }),
  setLoading: (loading) => set({ isLoading: loading }),

  hasPermission: (code) => {
    const { permissions } = get()
    return permissions.includes(code) || permissions.includes('*')
  },

  initialize: async () => {
    const { accessToken, refreshToken } = get()
    const cookieToken = getCookie('access_token')
    const token = accessToken ?? cookieToken
    // 从标记 cookie 恢复记住模式:store 本身不持久化,刷新后 rememberMe 会回到默认的 true。
    const rememberMe = currentPersistMode()

    if (!token) {
      set({ isLoading: false, rememberMe })
      return
    }

    set({
      accessToken: token,
      refreshToken: refreshToken ?? getCookie('refresh_token'),
      isAuthenticated: true,
      isLoading: false,
      rememberMe,
    })
  },
}))
