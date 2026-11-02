/**
 * DTOs for the e-commerce layer (scm-member / scm-mall / scm-promotion /
 * scm-payment / scm-order-center / scm-fulfillment / scm-search).
 *
 * These services return raw entities and MyBatis-Plus `Page` echoes rather
 * than `ApiResponse<T>`; `unwrap()` in `../envelope` passes them through.
 */

export interface EcomMember {
  id: number;
  tenantId?: string;
  userId: string;
  memberNo: string;
  nickname?: string;
  avatar?: string;
  gender?: number;
  birthday?: string;
  memberLevel?: number;
  points?: number;
  totalSpent?: number;
  status?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface MemberAddress {
  id: number;
  tenantId?: string;
  userId: string;
  receiverName: string;
  phone: string;
  province?: string;
  city?: string;
  district?: string;
  detailAddress?: string;
  isDefault?: boolean;
  createdAt?: string;
}

export interface MemberPointsLog {
  id: number;
  userId: string;
  points: number;
  type?: string;
  source?: string;
  orderNo?: string;
  description?: string;
  createdAt?: string;
}

export interface MemberRegisterPayload {
  userId: string;
  nickname?: string;
  avatar?: string;
}

export interface MemberProfilePayload {
  nickname?: string;
  avatar?: string;
  gender?: number;
  birthday?: string;
}

export interface MemberAddressPayload {
  receiverName: string;
  phone: string;
  province?: string;
  city?: string;
  district?: string;
  detailAddress?: string;
  isDefault?: boolean;
}

export interface PointsPayload {
  points: number;
  source?: string;
  orderNo?: string;
  description?: string;
}

export interface CartItem {
  id: number;
  userId: string;
  skuId: number;
  productId: number;
  quantity: number;
  selected?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AddToCartPayload {
  userId: string;
  productId: number;
  skuId: number;
  quantity: number;
}

export interface Review {
  id: number;
  userId: string;
  productId: number;
  skuId?: number;
  orderNo?: string;
  rating?: number;
  content?: string;
  imagesJson?: string;
  isAnonymous?: boolean;
  status?: number;
  createdAt?: string;
}

export interface ReviewPayload {
  userId: string;
  productId: number;
  skuId?: number;
  orderNo?: string;
  rating: number;
  content: string;
  isAnonymous?: boolean;
}

export interface Seller {
  id: number;
  sellerName: string;
  sellerType?: string;
  contactName?: string;
  contactPhone?: string;
  licenseNo?: string;
  licenseImage?: string;
  status?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface Coupon {
  id: number;
  templateId: number;
  userId: string;
  couponCode: string;
  status?: number;
  usedAt?: string;
  orderNo?: string;
  createdAt?: string;
}

export interface CouponIssuePayload {
  userId: string;
  templateId: number;
}

export interface CouponVerifyPayload {
  userId: string;
  couponCode: string;
  orderAmount: number;
}

export interface CouponUsePayload {
  couponCode: string;
  orderNo: string;
}

export interface FlashSale {
  id: number;
  activityId: number;
  productId: number;
  skuId: number;
  originalPrice?: number;
  flashPrice?: number;
  flashStock?: number;
  soldCount?: number;
  startTime?: string;
  endTime?: string;
  status?: number;
}

export interface GroupBuy {
  id: number;
  activityId: number;
  productId: number;
  skuId: number;
  groupPrice?: number;
  groupSize?: number;
  currentSize?: number;
  expireTime?: string;
  status?: number;
  createdAt?: string;
}

export interface GroupBuyMember {
  id: number;
  groupBuyId: number;
  userId: string;
  orderNo?: string;
  status?: number;
  joinedAt?: string;
}

export interface CreateGroupBuyPayload {
  userId: number;
  activityId: number;
  productId: number;
  skuId: number;
}

export interface PaymentOrderDTO {
  id: number;
  paymentNo: string;
  orderNo: string;
  userId: string;
  amount: number;
  currency?: string;
  paymentMethod?: string;
  paymentChannel?: string;
  status?: number;
  transactionId?: string;
  paidAt?: string;
  callbackUrl?: string;
  notifyUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreatePaymentPayload {
  orderNo: string;
  userId: string;
  amount: number;
  paymentChannel: string;
}

export interface PaymentRefundDTO {
  id: number;
  refundNo: string;
  paymentNo: string;
  orderNo: string;
  userId: string;
  refundAmount: number;
  refundReason?: string;
  status?: number;
  refundedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateRefundPayload {
  paymentNo: string;
  orderNo: string;
  userId: string;
  amount: number;
  reason?: string;
}

export interface OcOrder {
  id: number;
  tenantId?: string;
  orderNo: string;
  parentOrderNo?: string;
  channel?: string;
  orderType?: string;
  userId: string;
  sellerId?: number;
  status?: number;
  totalAmount?: number;
  discountAmount?: number;
  payableAmount?: number;
  paidAmount?: number;
  shippingFee?: number;
  receiverName?: string;
  receiverPhone?: string;
  receiverAddress?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface OcOrderItem {
  id?: number;
  orderNo?: string;
  productId?: number;
  skuId?: number;
  productName?: string;
  skuName?: string;
  price?: number;
  quantity: number;
  discountAmount?: number;
  payableAmount?: number;
  sellerId?: number;
  status?: number;
  createdAt?: string;
}

export interface FulfillmentOrder {
  id: number;
  fulfillmentNo: string;
  orderNo: string;
  userId: string;
  status?: number;
  fulfillmentType?: string;
  warehouseId?: number;
  shippingMethod?: string;
  receiverName?: string;
  receiverPhone?: string;
  receiverAddress?: string;
  totalItems?: number;
  totalWeight?: number;
  shippingFee?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateFulfillmentPayload {
  orderNo: string;
  userId: string;
  fulfillmentType: string;
}

export interface Tracking {
  id?: number;
  fulfillmentNo: string;
  trackingNo?: string;
  carrier?: string;
  status?: string;
  location?: string;
  description?: string;
  eventTime?: string;
  createdAt?: string;
}

export interface ProductDocument {
  id?: string;
  productId: number;
  sellerId?: number;
  title: string;
  subtitle?: string;
  description?: string;
  categoryId?: number;
  categoryPath?: string;
  brandId?: number;
  brandName?: string;
  price?: number;
  originalPrice?: number;
  salesCount?: number;
  rating?: number;
  reviewCount?: number;
  mainImage?: string;
  images?: string[];
  tags?: string[];
  status?: string;
  createdAt?: string;
  updatedAt?: string;
}
