import apiClient from "../client";
import { unwrap } from "../envelope";
import type { MpPage } from "../contracts/page";
import type {
  AddToCartPayload,
  CartItem,
  Coupon,
  CouponIssuePayload,
  CouponUsePayload,
  CouponVerifyPayload,
  CreateFulfillmentPayload,
  CreateGroupBuyPayload,
  CreatePaymentPayload,
  CreateRefundPayload,
  EcomMember,
  FlashSale,
  FulfillmentOrder,
  GroupBuy,
  GroupBuyMember,
  MemberAddress,
  MemberAddressPayload,
  MemberPointsLog,
  MemberProfilePayload,
  MemberRegisterPayload,
  OcOrder,
  OcOrderItem,
  PaymentOrderDTO,
  PaymentRefundDTO,
  PointsPayload,
  ProductDocument,
  Review,
  ReviewPayload,
  Seller,
  Tracking,
} from "../contracts/ecommerce";

/**
 * Every e-commerce service is reached through the gateway's Nacos discovery
 * locator (`/scm-<service>/**` with the first segment stripped), exactly like
 * the older business services.
 */
const MEMBER_BASE = "/scm-member/api/v1";
const MALL_BASE = "/scm-mall/api/v1/mall";
const PROMOTION_BASE = "/scm-promotion/api/v1/promotions";
const PAYMENT_BASE = "/scm-payment/api/v1/payments";
const ORDER_CENTER_BASE = "/scm-order-center/api/v1/orders";
const FULFILLMENT_BASE = "/scm-fulfillment/api/v1/fulfillments";
const SEARCH_BASE = "/scm-search/api/v1/search";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, data?: unknown) =>
  unwrap<T>(apiClient.post(url, data));

const put = <T>(url: string, data?: unknown, params?: object) =>
  unwrap<T>(apiClient.put(url, data, { params }));

const del = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.delete(url, { params }));

export interface MemberQuery {
  page?: number;
  size?: number;
  keyword?: string;
  memberLevel?: number;
  status?: number;
}

export interface PaymentQuery {
  page?: number;
  size?: number;
  orderNo?: string;
  userId?: string;
  paymentChannel?: string;
  status?: number;
}

export interface RefundQuery {
  page?: number;
  size?: number;
  paymentNo?: string;
  orderNo?: string;
  userId?: string;
  status?: number;
}

export interface OrderQuery {
  page?: number;
  size?: number;
  orderNo?: string;
  userId?: string;
  status?: number;
}

export interface FulfillmentQuery {
  page?: number;
  size?: number;
  orderNo?: string;
  userId?: string;
  status?: number;
}

export interface CouponQuery {
  page?: number;
  size?: number;
  userId?: string;
  status?: number;
}

export const memberApi = {
  page: (query: MemberQuery) =>
    get<MpPage<EcomMember>>(`${MEMBER_BASE}/members/page`, query),
  register: (payload: MemberRegisterPayload) =>
    post<EcomMember>(`${MEMBER_BASE}/members/register`, payload),
  get: (userId: string) =>
    get<EcomMember>(`${MEMBER_BASE}/members/${encodeURIComponent(userId)}`),
  update: (userId: string, payload: MemberProfilePayload) =>
    put<EcomMember>(
      `${MEMBER_BASE}/members/${encodeURIComponent(userId)}`,
      payload
    ),
  updateLevel: (userId: string, level: number) =>
    put<boolean>(
      `${MEMBER_BASE}/members/${encodeURIComponent(userId)}/level`,
      null,
      { level }
    ),

  addresses: {
    list: (userId: string) =>
      get<MemberAddress[]>(
        `${MEMBER_BASE}/members/${encodeURIComponent(userId)}/addresses`
      ),
    create: (userId: string, payload: MemberAddressPayload) =>
      post<MemberAddress>(
        `${MEMBER_BASE}/members/${encodeURIComponent(userId)}/addresses`,
        payload
      ),
    update: (userId: string, addressId: number, payload: MemberAddressPayload) =>
      put<boolean>(
        `${MEMBER_BASE}/members/${encodeURIComponent(userId)}/addresses/${addressId}`,
        payload
      ),
    remove: (userId: string, addressId: number) =>
      del<boolean>(
        `${MEMBER_BASE}/members/${encodeURIComponent(userId)}/addresses/${addressId}`
      ),
    setDefault: (userId: string, addressId: number) =>
      put<boolean>(
        `${MEMBER_BASE}/members/${encodeURIComponent(userId)}/addresses/${addressId}/default`
      ),
  },

  points: {
    balance: (userId: string) =>
      get<number>(
        `${MEMBER_BASE}/members/${encodeURIComponent(userId)}/points`
      ),
    log: (userId: string) =>
      get<MemberPointsLog[]>(
        `${MEMBER_BASE}/members/${encodeURIComponent(userId)}/points/log`
      ),
    add: (userId: string, payload: PointsPayload) =>
      post<boolean>(
        `${MEMBER_BASE}/members/${encodeURIComponent(userId)}/points/add`,
        payload
      ),
    deduct: (userId: string, payload: PointsPayload) =>
      post<boolean>(
        `${MEMBER_BASE}/members/${encodeURIComponent(userId)}/points/deduct`,
        payload
      ),
  },
};

export const mallApi = {
  cart: {
    get: (userId: string) =>
      get<CartItem[]>(`${MALL_BASE}/cart`, { userId }),
    add: (payload: AddToCartPayload) =>
      post<boolean>(`${MALL_BASE}/cart`, payload),
    updateQuantity: (cartItemId: number, userId: string, quantity: number) =>
      put<boolean>(`${MALL_BASE}/cart/${cartItemId}`, { userId, quantity }),
    remove: (cartItemId: number, userId: string) =>
      del<boolean>(`${MALL_BASE}/cart/${cartItemId}`, { userId }),
    clear: (userId: string) =>
      del<boolean>(`${MALL_BASE}/cart/clear`, { userId }),
  },

  reviews: {
    create: (payload: ReviewPayload) =>
      post<Review>(`${MALL_BASE}/reviews`, payload),
    listByProduct: (productId: number, page = 1, size = 20) =>
      get<MpPage<Review>>(`${MALL_BASE}/products/${productId}/reviews`, {
        page,
        size,
      }),
    remove: (reviewId: number, userId: string) =>
      del<boolean>(`${MALL_BASE}/reviews/${reviewId}`, { userId }),
  },

  sellers: {
    register: (payload: Partial<Seller>) =>
      post<Seller>(`${MALL_BASE}/sellers`, payload),
    get: (sellerId: number) => get<Seller>(`${MALL_BASE}/sellers/${sellerId}`),
  },
};

export const promotionApi = {
  coupons: {
    page: (query: CouponQuery) =>
      get<MpPage<Coupon>>(`${PROMOTION_BASE}/coupons/page`, query),
    issue: (payload: CouponIssuePayload) =>
      post<Coupon>(`${PROMOTION_BASE}/coupons/issue`, payload),
    verify: (payload: CouponVerifyPayload) =>
      post<Coupon>(`${PROMOTION_BASE}/coupons/verify`, payload),
    use: (payload: CouponUsePayload) =>
      post<boolean>(`${PROMOTION_BASE}/coupons/use`, payload),
    listByUser: (userId: string) =>
      get<Coupon[]>(
        `${PROMOTION_BASE}/coupons/user/${encodeURIComponent(userId)}`
      ),
  },

  flashSales: {
    active: () => get<FlashSale[]>(`${PROMOTION_BASE}/flash-sales/active`),
    checkStock: (flashSaleId: number, skuId: number) =>
      post<boolean>(`${PROMOTION_BASE}/flash-sales/check-stock`, {
        flashSaleId,
        skuId,
      }),
    purchase: (flashSaleId: number, skuId: number, quantity: number) =>
      post<boolean>(`${PROMOTION_BASE}/flash-sales/purchase`, {
        flashSaleId,
        skuId,
        quantity,
      }),
  },

  groups: {
    create: (payload: CreateGroupBuyPayload) =>
      post<GroupBuy>(`${PROMOTION_BASE}/groups`, payload),
    join: (groupBuyId: number, userId: number) =>
      post<GroupBuy>(`${PROMOTION_BASE}/groups/${groupBuyId}/join`, {
        userId,
      }),
    get: (groupBuyId: number) =>
      get<GroupBuy>(`${PROMOTION_BASE}/groups/${groupBuyId}`),
    members: (groupBuyId: number) =>
      get<GroupBuyMember[]>(`${PROMOTION_BASE}/groups/${groupBuyId}/members`),
  },
};

export const paymentApi = {
  page: (query: PaymentQuery) =>
    get<MpPage<PaymentOrderDTO>>(`${PAYMENT_BASE}/page`, query),
  create: (payload: CreatePaymentPayload) =>
    post<PaymentOrderDTO>(`${PAYMENT_BASE}/create`, payload),
  get: (paymentNo: string) =>
    get<PaymentOrderDTO>(`${PAYMENT_BASE}/${encodeURIComponent(paymentNo)}`),
  cancel: (paymentNo: string) =>
    post<boolean>(`${PAYMENT_BASE}/${encodeURIComponent(paymentNo)}/cancel`),

  refunds: {
    page: (query: RefundQuery) =>
      get<MpPage<PaymentRefundDTO>>(`${PAYMENT_BASE}/refunds/page`, query),
    create: (payload: CreateRefundPayload) =>
      post<PaymentRefundDTO>(`${PAYMENT_BASE}/refunds`, payload),
    get: (refundNo: string) =>
      get<PaymentRefundDTO>(
        `${PAYMENT_BASE}/refunds/${encodeURIComponent(refundNo)}`
      ),
    approve: (refundNo: string) =>
      post<PaymentRefundDTO>(
        `${PAYMENT_BASE}/refunds/${encodeURIComponent(refundNo)}/approve`
      ),
    reject: (refundNo: string, reason?: string) =>
      post<PaymentRefundDTO>(
        `${PAYMENT_BASE}/refunds/${encodeURIComponent(refundNo)}/reject`,
        { reason }
      ),
  },
};

export const orderCenterApi = {
  page: (query: OrderQuery) =>
    get<MpPage<OcOrder>>(`${ORDER_CENTER_BASE}/page`, query),
  create: (order: OcOrder, items: OcOrderItem[]) =>
    post<OcOrder>(ORDER_CENTER_BASE, { order, items }),
  get: (orderNo: string) =>
    get<OcOrder>(`${ORDER_CENTER_BASE}/${encodeURIComponent(orderNo)}`),
  cancel: (orderNo: string, reason?: string) =>
    post<boolean>(`${ORDER_CENTER_BASE}/${encodeURIComponent(orderNo)}/cancel`, {
      reason,
    }),
  pay: (orderNo: string, paymentNo: string) =>
    post<boolean>(`${ORDER_CENTER_BASE}/${encodeURIComponent(orderNo)}/pay`, {
      paymentNo,
    }),
  ship: (orderNo: string, logisticsNo: string) =>
    post<boolean>(`${ORDER_CENTER_BASE}/${encodeURIComponent(orderNo)}/ship`, {
      logisticsNo,
    }),
  deliver: (orderNo: string) =>
    post<boolean>(
      `${ORDER_CENTER_BASE}/${encodeURIComponent(orderNo)}/deliver`
    ),
  confirm: (orderNo: string) =>
    post<boolean>(
      `${ORDER_CENTER_BASE}/${encodeURIComponent(orderNo)}/confirm`
    ),
};

export const fulfillmentApi = {
  page: (query: FulfillmentQuery) =>
    get<MpPage<FulfillmentOrder>>(`${FULFILLMENT_BASE}/page`, query),
  create: (payload: CreateFulfillmentPayload) =>
    post<FulfillmentOrder>(FULFILLMENT_BASE, payload),
  get: (fulfillmentNo: string) =>
    get<FulfillmentOrder>(
      `${FULFILLMENT_BASE}/${encodeURIComponent(fulfillmentNo)}`
    ),
  cancel: (fulfillmentNo: string, reason?: string) =>
    post<boolean>(
      `${FULFILLMENT_BASE}/${encodeURIComponent(fulfillmentNo)}/cancel`,
      { reason }
    ),
  pick: (fulfillmentNo: string) =>
    post<boolean>(
      `${FULFILLMENT_BASE}/${encodeURIComponent(fulfillmentNo)}/pick`
    ),
  pack: (fulfillmentNo: string) =>
    post<boolean>(
      `${FULFILLMENT_BASE}/${encodeURIComponent(fulfillmentNo)}/pack`
    ),
  ship: (fulfillmentNo: string, trackingNo: string, carrier: string) =>
    post<boolean>(
      `${FULFILLMENT_BASE}/${encodeURIComponent(fulfillmentNo)}/ship`,
      { trackingNo, carrier }
    ),
  deliver: (fulfillmentNo: string) =>
    post<boolean>(
      `${FULFILLMENT_BASE}/${encodeURIComponent(fulfillmentNo)}/deliver`
    ),

  tracking: {
    record: (payload: Tracking) =>
      post<Tracking>(`${FULFILLMENT_BASE}/tracking`, payload),
    byTrackingNo: (trackingNo: string) =>
      get<Tracking[]>(
        `${FULFILLMENT_BASE}/tracking/${encodeURIComponent(trackingNo)}`
      ),
    byFulfillmentNo: (fulfillmentNo: string) =>
      get<Tracking[]>(
        `${FULFILLMENT_BASE}/${encodeURIComponent(fulfillmentNo)}/tracking`
      ),
    latest: (fulfillmentNo: string) =>
      get<Tracking>(
        `${FULFILLMENT_BASE}/${encodeURIComponent(fulfillmentNo)}/tracking/latest`
      ),
  },
};

export const searchApi = {
  products: (params: {
    keyword?: string;
    categoryId?: number;
    brandId?: number;
    page?: number;
    size?: number;
  }) => get<ProductDocument[]>(`${SEARCH_BASE}/products`, params),
  similar: (productId: number, limit = 10) =>
    get<ProductDocument[]>(`${SEARCH_BASE}/products/${productId}/similar`, {
      limit,
    }),
  personalized: (userId: number, limit = 20) =>
    get<ProductDocument[]>(
      `${SEARCH_BASE}/products/personalized/${userId}`,
      { limit }
    ),
  indexProduct: (document: ProductDocument) =>
    post<boolean>(`${SEARCH_BASE}/index/product`, document),
  removeProduct: (productId: number) =>
    del<boolean>(`${SEARCH_BASE}/index/product/${productId}`),
  bulkIndex: (documents: ProductDocument[]) =>
    post<number>(`${SEARCH_BASE}/index/products`, documents),
  reindexAll: () => post<number>(`${SEARCH_BASE}/index/reindex`),
  deleteIndexedProduct: (productId: number) =>
    del<boolean>(`${SEARCH_BASE}/index/products/${productId}`),
};
