// Legacy supply-chain models. Auth and system contracts live in
// src/lib/api/contracts/{auth,system}.ts and should not be duplicated here.
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
