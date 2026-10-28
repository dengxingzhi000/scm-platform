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
  /** 是否记住我:决定 access_token cookie 是 7 天持久 cookie 还是会话 cookie。 */
  rememberMe: boolean
  login: (user: User, accessToken: string, refreshToken: string, remember?: boolean) => void
  logout: () => void
  setUser: (user: User) => void
  setPermissions: (permissions: string[]) => void
  setLoading: (loading: boolean) => void
  hasPermission: (code: string) => boolean
  initialize: () => Promise<void>
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
    // `remember` 省略时沿用当前模式 —— apiClient 的 401 刷新路径
    // (src/lib/api/client.ts:100)会不带参数地重新 login(),
    // 那条路径不能把「不记住我」的会话又变回 7 天持久 cookie。
    const persist = remember ?? get().rememberMe
    set({ user, accessToken, refreshToken, isAuthenticated: true, isLoading: false, rememberMe: persist })
    // 不记住我 → 会话 cookie(没有 expires)。同名同 path 的 cookie 会被浏览器整体替换,
    // 所以用户「先勾选后取消」时,之前那个 7 天的持久 cookie 也随之消失。
    setCookie('access_token', accessToken, persist ? 7 : undefined)
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
  },

  setUser: (user) => set({ user }),
  setPermissions: (permissions) => set({ permissions }),
  setLoading: (loading) => set({ isLoading: loading }),

  hasPermission: (code) => {
    const { permissions } = get()
    return permissions.includes(code) || permissions.includes('*')
  },

  initialize: async () => {
    const { accessToken } = get()
    const cookieToken = getCookie('access_token')
    const token = accessToken ?? cookieToken

    if (!token) {
      set({ isLoading: false })
      return
    }

    set({ accessToken: token, isLoading: true })
  },
}))
