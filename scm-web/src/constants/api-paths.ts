export const API_PATHS = {
  AUTH: {
    LOGIN: '/api/auth/login',
    LOGOUT: '/api/auth/logout',
    REFRESH: '/api/auth/refresh',
    ME: '/api/auth/me',
    MFA_VERIFY: '/api/auth/mfa/verify',
  },
  USER: {
    BASE: '/api/users',
    BY_ID: (id: string) => `/api/users/${id}`,
  },
  ORDER: {
    BASE: '/api/v1/orders',
    BY_ID: (id: string | number) => `/api/v1/orders/${id}`,
    QUERY: '/api/v1/orders/query',
    ITEMS: (id: string | number) => `/api/v1/orders/${id}/items`,
    STATUS: (id: string | number) => `/api/v1/orders/${id}/status`,
    CANCEL: (id: string | number) => `/api/v1/orders/${id}/cancel`,
  },
  PRODUCT: {
    BASE: '/api/products',
    BY_ID: (id: string) => `/api/products/${id}`,
    SEARCH: '/api/products/search',
  },
  INVENTORY: {
    BASE: '/api/inventory',
    BY_SKU: (skuId: string) => `/api/inventory/${skuId}`,
  },
} as const
