import apiClient from "./client";

export const authApi = {
  login: (data: { username: string; password: string }) =>
    apiClient.post("/api/auth/login", data),
  logout: () => apiClient.post("/api/auth/logout"),
  refreshToken: (refreshToken: string) =>
    apiClient.post("/api/auth/refresh", { refreshToken }),
  me: () => apiClient.get("/api/auth/me"),
};

export const userApi = {
  list: (params?: { current?: number; size?: number; keyword?: string }) =>
    apiClient.get("/api/users", { params }),
  getById: (id: string) => apiClient.get(`/api/users/${id}`),
  create: (data: Record<string, unknown>) => apiClient.post("/api/users", data),
  update: (id: string, data: Record<string, unknown>) =>
    apiClient.put(`/api/users/${id}`, data),
  delete: (id: string) => apiClient.delete(`/api/users/${id}`),
};

export const orderApi = {
  list: (params?: { current?: number; size?: number; status?: string }) =>
    apiClient.get("/api/orders", { params }),
  getById: (orderNo: string) => apiClient.get(`/api/orders/${orderNo}`),
  create: (data: Record<string, unknown>) => apiClient.post("/api/orders", data),
  updateStatus: (orderNo: string, status: string) =>
    apiClient.put(`/api/orders/${orderNo}/status`, { status }),
};

export const productApi = {
  list: (params?: { current?: number; size?: number; keyword?: string }) =>
    apiClient.get("/api/products", { params }),
  getById: (id: string) => apiClient.get(`/api/products/${id}`),
  search: (keyword: string) => apiClient.get("/api/products/search", { params: { keyword } }),
};

export const inventoryApi = {
  getBySku: (skuId: string) => apiClient.get(`/api/inventory/${skuId}`),
  list: (params?: { keyword?: string; lowStock?: boolean }) =>
    apiClient.get("/api/inventory", { params }),
};
