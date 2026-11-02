export const QUERY_KEYS = {
  AUTH: {
    ME: ['auth', 'me'] as const,
    PERMISSIONS: ['auth', 'permissions'] as const,
  },
  USER: {
    ALL: ['users'] as const,
    LIST: (params?: Record<string, unknown>) => ['users', 'list', params] as const,
    DETAIL: (id: string) => ['users', 'detail', id] as const,
  },
  PRODUCT: {
    ALL: ['products'] as const,
    LIST: (params?: Record<string, unknown>) => ['products', 'list', params] as const,
    DETAIL: (id: string) => ['products', 'detail', id] as const,
  },
  ORDER: {
    ALL: ['orders'] as const,
    LIST: (params?: Record<string, unknown>) => ['orders', 'list', params] as const,
    DETAIL: (orderNo: string) => ['orders', 'detail', orderNo] as const,
  },
  INVENTORY: {
    ALL: ['inventory'] as const,
    LIST: (params?: Record<string, unknown>) => ['inventory', 'list', params] as const,
    DETAIL: (skuId: string) => ['inventory', 'detail', skuId] as const,
  },
  NOTIFICATION: {
    ALL: ['notifications'] as const,
    LIST: (params?: unknown) => ['notifications', 'list', params] as const,
    UNREAD_COUNT: ['notifications', 'unread'] as const,
  },
  DASHBOARD: {
    STATS: ['dashboard', 'stats'] as const,
    KPI: ['dashboard', 'kpi'] as const,
  },
} as const
