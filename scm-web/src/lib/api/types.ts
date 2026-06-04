export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
  timestamp?: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface PageParam {
  current?: number;
  size?: number;
}

export interface OrderVO {
  orderNo: string;
  status: string;
  totalAmount: number;
  createTime: string;
  orderItems?: OrderItemVO[];
}

export interface OrderItemVO {
  skuId: string;
  skuName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface ProductVO {
  id: string;
  spuName: string;
  skuList: SkuVO[];
}

export interface SkuVO {
  skuId: string;
  skuCode: string;
  skuName: string;
  price: number;
  stock: number;
  imageUrl?: string;
}

export interface InventoryVO {
  skuId: string;
  skuName: string;
  availableStock: number;
  reservedStock: number;
  totalStock: number;
}

export interface UserVO {
  id: string;
  username: string;
  displayName: string;
  email?: string;
  phone?: string;
  avatar?: string;
  status: string;
  deptName?: string;
  roleNames?: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserVO;
}
