import apiClient from "./client";

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
