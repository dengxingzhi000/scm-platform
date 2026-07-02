# Year 1 E-Commerce Services Design Specification

**Date:** 2026-06-25  
**Status:** Approved  
**Scope:** 7 new microservices for Year 1 of the E-Commerce Ecosystem evolution

---

## Executive Summary

This document specifies the detailed design for 7 new microservices that form the "Transaction Layer" of the SCM Platform's evolution into a full e-commerce ecosystem. These services build upon the existing SCM core (inventory, warehouse, purchase, supplier, logistics) without modifying them.

### Services Overview

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| scm-member | 8302 | db_user | Member Center (users, profiles, points, WeChat/Alipay) |
| scm-promotion | 8303 | db_promotion | Promotion Center (coupons, flash sales, AI recommendations) |
| scm-mall | 8301 | db_order | E-Commerce Platform (B2C + Marketplace) |
| scm-payment | 8304 | db_finance | Payment Center (Alipay, WeChat, Stripe, PayPal) |
| scm-order-center | 8305 | db_order | Unified Order Center (state machine, saga, event sourcing) |
| scm-fulfillment | 8306 | db_warehouse | Fulfillment Center (3PL, dropshipping, routing) |
| scm-search | 8307 | Elasticsearch | Search Center (NLP, image, voice search) |

---

## 1. scm-member — Member Center

### 1.1 Service Overview

| Property | Value |
|----------|-------|
| Port | 8302 |
| Database | db_user (shared with scm-auth) |
| Package | com.scmcloud.member |
| Dependencies | scm-auth-api, scm-common-web, scm-common-data |

### 1.2 Database Schema

```sql
-- Member core table
CREATE TABLE mem_member (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    member_no VARCHAR(32) NOT NULL UNIQUE,
    nickname VARCHAR(64),
    avatar VARCHAR(256),
    gender SMALLINT DEFAULT 0,  -- 0:unknown, 1:male, 2:female
    birthday DATE,
    member_level INT DEFAULT 1,
    points INT DEFAULT 0,
    total_spent DECIMAL(12,2) DEFAULT 0,
    status SMALLINT DEFAULT 1,  -- 1:active, 0:disabled
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Member levels configuration
CREATE TABLE mem_member_level (
    id BIGINT PRIMARY KEY,
    level_code VARCHAR(32) NOT NULL UNIQUE,
    level_name VARCHAR(64) NOT NULL,
    min_points INT NOT NULL,
    discount_rate DECIMAL(3,2) DEFAULT 1.00,
    privileges_json JSONB,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Address book
CREATE TABLE mem_member_address (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    receiver_name VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(32) NOT NULL,
    city VARCHAR(32) NOT NULL,
    district VARCHAR(32) NOT NULL,
    detail_address VARCHAR(256) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Points transaction log
CREATE TABLE mem_member_points_log (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    points INT NOT NULL,
    type VARCHAR(32) NOT NULL,  -- EARN, DEDUCT, EXPIRE, ADJUST
    source VARCHAR(64),  -- ORDER, ACTIVITY, MANUAL
    order_no VARCHAR(64),
    description VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Third-party bindings (WeChat, Alipay)
CREATE TABLE mem_third_party_binding (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    platform VARCHAR(32) NOT NULL,  -- WECHAT, ALIPAY
    open_id VARCHAR(128) NOT NULL,
    union_id VARCHAR(128),
    nickname VARCHAR(64),
    avatar VARCHAR(256),
    binding_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status SMALLINT DEFAULT 1,
    UNIQUE(platform, open_id)
);

-- Member tags for marketing
CREATE TABLE mem_member_tag (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    tag_name VARCHAR(64) NOT NULL,
    tag_type VARCHAR(32),  -- SYSTEM, MANUAL, BEHAVIOR
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, tag_name)
);

CREATE INDEX idx_mem_member_user_id ON mem_member(user_id);
CREATE INDEX idx_mem_address_user_id ON mem_member_address(user_id);
CREATE INDEX idx_mem_points_user_id ON mem_member_points_log(user_id);
CREATE INDEX idx_mem_binding_platform ON mem_third_party_binding(platform, open_id);
```

### 1.3 API Design

```
POST   /api/v1/members/register          # Register (phone/email/WeChat)
POST   /api/v1/members/login             # Login (password/SMS/WeChat/Alipay)
POST   /api/v1/members/login/wechat      # WeChat OAuth login
POST   /api/v1/members/login/alipay      # Alipay OAuth login
GET    /api/v1/members/{id}              # Get member profile
PUT    /api/v1/members/{id}              # Update profile
GET    /api/v1/members/{id}/addresses    # List addresses
POST   /api/v1/members/{id}/addresses    # Add address
PUT    /api/v1/members/{id}/addresses/{addrId}  # Update address
DELETE /api/v1/members/{id}/addresses/{addrId}  # Delete address
GET    /api/v1/members/{id}/points       # Get points balance
GET    /api/v1/members/{id}/points/log   # Points history
POST   /api/v1/members/{id}/points/add   # Add points
POST   /api/v1/members/{id}/points/deduct # Deduct points
GET    /api/v1/members/levels            # List member levels
```

### 1.4 Dubbo Interface

```java
public interface MemberDubboService {
    MemberVO getMember(Long userId);
    MemberVO register(RegisterRequest request);
    void updateMemberLevel(Long userId, Integer level);
    void addPoints(Long userId, Integer points, String source);
    void deductPoints(Long userId, Integer points, String source);
    List<AddressVO> getAddresses(Long userId);
    boolean verifyMember(Long userId);
}
```

### 1.5 WeChat/Alipay Integration Flow

```
User → Mini-Program/APP → WeChat/Alipay Auth → scm-auth (OAuth) → scm-member (Bindind)
    ↓
WeChat/Alipay OpenID → mem_third_party_binding → Link to user_id
    ↓
Subsequent logins: OpenID → scm-member → Get user_id → Generate JWT
```

---

## 2. scm-promotion — Promotion Center

### 2.1 Service Overview

| Property | Value |
|----------|-------|
| Port | 8303 |
| Database | db_promotion |
| Package | com.scmcloud.promotion |
| Dependencies | scm-member-api, scm-product-api, scm-decision-engine-api, scm-common-web |

### 2.2 Database Schema

```sql
-- Promotion activities
CREATE TABLE pro_activity (
    id BIGINT PRIMARY KEY,
    activity_name VARCHAR(128) NOT NULL,
    activity_type VARCHAR(32) NOT NULL,  -- COUPON, FLASH_SALE, GROUP_BUY, BUNDLE, TIERED
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status SMALLINT DEFAULT 1,
    rules_json JSONB,
    applicable_scope VARCHAR(32),  -- ALL, CATEGORY, PRODUCT
    created_by VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Coupon templates
CREATE TABLE pro_coupon_template (
    id BIGINT PRIMARY KEY,
    template_name VARCHAR(128) NOT NULL,
    coupon_type VARCHAR(32) NOT NULL,  -- FIXED, PERCENT, FREE_SHIPPING
    discount_value DECIMAL(10,2),
    min_amount DECIMAL(10,2),
    max_discount DECIMAL(10,2),
    total_count INT,
    issued_count INT DEFAULT 0,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Issued coupons
CREATE TABLE pro_coupon (
    id BIGINT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    coupon_code VARCHAR(64) NOT NULL UNIQUE,
    status SMALLINT DEFAULT 1,  -- 1:unused, 2:used, 3:expired
    used_at TIMESTAMP,
    order_no VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Flash sale
CREATE TABLE pro_flash_sale (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    original_price DECIMAL(10,2),
    flash_price DECIMAL(10,2) NOT NULL,
    flash_stock INT NOT NULL,
    sold_count INT DEFAULT 0,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status SMALLINT DEFAULT 1
);

-- Group buy
CREATE TABLE pro_group_buy (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    group_price DECIMAL(10,2) NOT NULL,
    group_size INT NOT NULL,
    current_size INT DEFAULT 0,
    expire_time TIMESTAMP,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pro_group_buy_member (
    id BIGINT PRIMARY KEY,
    group_buy_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    is_leader BOOLEAN DEFAULT FALSE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bundle deals
CREATE TABLE pro_bundle (
    id BIGINT PRIMARY KEY,
    bundle_name VARCHAR(128),
    bundle_type VARCHAR(32),  -- FIXED, MIX_MATCH
    discount_type VARCHAR(32),  -- FIXED, PERCENT
    discount_value DECIMAL(10,2),
    min_quantity INT DEFAULT 2,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pro_bundle_item (
    id BIGINT PRIMARY KEY,
    bundle_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT,
    quantity INT DEFAULT 1,
    is_required BOOLEAN DEFAULT TRUE
);

-- Tiered pricing
CREATE TABLE pro_tiered_pricing (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT,
    product_id BIGINT NOT NULL,
    min_quantity INT NOT NULL,
    max_quantity INT,
    unit_price DECIMAL(10,2),
    discount_rate DECIMAL(3,2)
);

-- AI recommendation log
CREATE TABLE pro_recommendation_log (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64),
    product_id BIGINT,
    recommendation_type VARCHAR(32),
    score DECIMAL(5,4),
    position INT,
    clicked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- A/B testing
CREATE TABLE pro_ab_test (
    id BIGINT PRIMARY KEY,
    test_name VARCHAR(128),
    test_type VARCHAR(32),
    variants_json JSONB,
    traffic_split JSONB,
    status SMALLINT DEFAULT 1,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pro_ab_test_assignment (
    id BIGINT PRIMARY KEY,
    test_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    variant VARCHAR(32),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(test_id, user_id)
);

CREATE INDEX idx_pro_coupon_user ON pro_coupon(user_id, status);
CREATE INDEX idx_pro_flash_sale_time ON pro_flash_sale(start_time, end_time);
CREATE INDEX idx_pro_group_buy_activity ON pro_group_buy(activity_id, status);
CREATE INDEX idx_pro_recommendation_user ON pro_recommendation_log(user_id);
```

### 2.3 API Design

```
# Activities
POST   /api/v1/promotions/activities              # Create activity
GET    /api/v1/promotions/activities/{id}          # Get activity
PUT    /api/v1/promotions/activities/{id}          # Update activity
GET    /api/v1/promotions/activities/active        # List active activities

# Coupons
POST   /api/v1/promotions/coupons/templates        # Create coupon template
GET    /api/v1/promotions/coupons/templates/{id}   # Get template
POST   /api/v1/promotions/coupons/issue            # Issue coupon to user
GET    /api/v1/promotions/coupons/user/{userId}    # User's coupons
POST   /api/v1/promotions/coupons/use              # Use coupon
POST   /api/v1/promotions/coupons/verify           # Verify coupon validity

# Flash Sale
POST   /api/v1/promotions/flash-sales              # Create flash sale
GET    /api/v1/promotions/flash-sales/active        # Active flash sales
POST   /api/v1/promotions/flash-sales/purchase      # Purchase flash sale item

# Group Buy
POST   /api/v1/promotions/group-buys               # Create group buy
GET    /api/v1/promotions/group-buys/{id}           # Get group buy details
POST   /api/v1/promotions/group-buys/{id}/join      # Join group
POST   /api/v1/promotions/group-buys/create-group   # Start new group

# AI Recommendations
GET    /api/v1/promotions/recommendations/user/{userId}  # Personalized recs
POST   /api/v1/promotions/recommendations/click          # Track click
GET    /api/v1/promotions/recommendations/similar/{productId}  # Similar products

# A/B Testing
POST   /api/v1/promotions/ab-tests                 # Create test
GET    /api/v1/promotions/ab-tests/{id}/assignment  # Get user's variant
POST   /api/v1/promotions/ab-tests/{id}/convert     # Track conversion
```

### 2.4 Dubbo Interface

```java
public interface PromotionDubboService {
    CouponVO issueCoupon(Long userId, Long templateId);
    CouponVerifyResult verifyCoupon(Long userId, String couponCode, BigDecimal orderAmount);
    void useCoupon(String couponCode, String orderNo);
    PromotionCalcResult calculatePromotions(Long userId, List<CartItem> items);
    boolean checkFlashSaleStock(Long flashSaleId, Long skuId);
    boolean deductFlashSaleStock(Long flashSaleId, Long skuId, Integer quantity);
    GroupBuyVO createGroup(Long userId, Long activityId, Long skuId);
    GroupBuyVO joinGroup(Long userId, Long groupBuyId);
    List<RecommendationVO> getPersonalizedRecommendations(Long userId, Integer limit);
    List<RecommendationVO> getSimilarProducts(Long productId, Integer limit);
}
```

---

## 3. scm-mall — E-Commerce Platform

### 3.1 Service Overview

| Property | Value |
|----------|-------|
| Port | 8301 |
| Database | db_order (shared with scm-order) |
| Package | com.scmcloud.mall |
| Dependencies | scm-product-api, scm-member-api, scm-promotion-api, scm-order-center-api, scm-search-api, scm-common-web |

### 3.2 Database Schema

```sql
-- Shopping cart
CREATE TABLE mall_cart (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    sku_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    selected BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seller management (marketplace)
CREATE TABLE mall_seller (
    id BIGINT PRIMARY KEY,
    seller_name VARCHAR(128) NOT NULL,
    seller_type VARCHAR(32),  -- INDIVIDUAL, ENTERPRISE
    contact_name VARCHAR(64),
    contact_phone VARCHAR(20),
    license_no VARCHAR(64),
    license_image VARCHAR(256),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mall_seller_shop (
    id BIGINT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    shop_name VARCHAR(128) NOT NULL,
    shop_logo VARCHAR(256),
    shop_description TEXT,
    category_ids JSONB,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product display (denormalized for performance)
CREATE TABLE mall_product_display (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    seller_id BIGINT,
    title VARCHAR(256) NOT NULL,
    subtitle VARCHAR(512),
    main_image VARCHAR(256),
    images_json JSONB,
    price_range VARCHAR(64),
    sales_count INT DEFAULT 0,
    review_count INT DEFAULT 0,
    rating DECIMAL(3,2) DEFAULT 5.00,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product reviews
CREATE TABLE mall_review (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT,
    order_no VARCHAR(64),
    rating SMALLINT NOT NULL,
    content TEXT,
    images_json JSONB,
    is_anonymous BOOLEAN DEFAULT FALSE,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mall_review_reply (
    id BIGINT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Browsing history
CREATE TABLE mall_browsing_history (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    category_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Favorites
CREATE TABLE mall_favorite (
    id BIGINT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, product_id)
);

CREATE INDEX idx_mall_cart_user ON mall_cart(user_id);
CREATE INDEX idx_mall_review_product ON mall_review(product_id, status);
CREATE INDEX idx_mall_history_user ON mall_browsing_history(user_id, created_at DESC);
CREATE INDEX idx_mall_favorite_user ON mall_favorite(user_id);
```

### 3.3 API Design

```
# Product browsing
GET    /api/v1/mall/products                    # List products (with filters)
GET    /api/v1/mall/products/{id}               # Product detail
GET    /api/v1/mall/products/{id}/reviews       # Product reviews
GET    /api/v1/mall/categories                  # Category tree
GET    /api/v1/mall/products/hot                # Hot products
GET    /api/v1/mall/products/new                # New arrivals
GET    /api/v1/mall/products/recommend          # Personalized recommendations

# Shopping cart
GET    /api/v1/mall/cart                        # Get cart
POST   /api/v1/mall/cart                        # Add to cart
PUT    /api/v1/mall/cart/{itemId}               # Update quantity
DELETE /api/v1/mall/cart/{itemId}               # Remove from cart
PUT    /api/v1/mall/cart/{itemId}/select        # Toggle selection
PUT    /api/v1/mall/cart/select-all             # Select all
DELETE /api/v1/mall/cart/clear                  # Clear cart

# Checkout
POST   /api/v1/mall/checkout/preview            # Preview order (with promotions)
POST   /api/v1/mall/checkout/submit             # Submit order

# Reviews
POST   /api/v1/mall/reviews                     # Create review
GET    /api/v1/mall/reviews/user/{userId}       # User's reviews

# Favorites
GET    /api/v1/mall/favorites/user/{userId}     # User's favorites
POST   /api/v1/mall/favorites                   # Add favorite
DELETE /api/v1/mall/favorites/{productId}       # Remove favorite

# Seller management
POST   /api/v1/mall/sellers                     # Register seller
GET    /api/v1/mall/sellers/{id}                # Seller profile
PUT    /api/v1/mall/sellers/{id}                # Update seller
GET    /api/v1/mall/sellers/{id}/products       # Seller's products
GET    /api/v1/mall/sellers/{id}/orders         # Seller's orders
GET    /api/v1/mall/sellers/{id}/analytics      # Seller analytics
```

### 3.4 Checkout Flow

```
User → Cart → Checkout Preview
    ↓
1. Get cart items (scm-mall)
2. Get product details (scm-product)
3. Check inventory (scm-inventory)
4. Calculate promotions (scm-promotion)
5. Apply coupons (scm-promotion)
6. Calculate shipping (scm-logistics)
7. Show preview with total
    ↓
User confirms → Submit Order
    ↓
1. Create order (scm-order-center)
2. Deduct inventory (scm-inventory)
3. Deduct coupons (scm-promotion)
4. Create payment request (scm-payment)
5. Clear cart items (scm-mall)
```

---

## 4. scm-payment — Payment Center

### 4.1 Service Overview

| Property | Value |
|----------|-------|
| Port | 8304 |
| Database | db_finance (shared with scm-finance) |
| Package | com.scmcloud.payment |
| Dependencies | scm-order-api, scm-common-web, scm-common-integration |

### 4.2 Database Schema

```sql
-- Payment orders
CREATE TABLE pay_payment_order (
    id BIGINT PRIMARY KEY,
    payment_no VARCHAR(64) NOT NULL UNIQUE,
    order_no VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'CNY',
    payment_method VARCHAR(32),
    payment_channel VARCHAR(32),  -- ALIPAY, WECHAT, STRIPE, PAYPAL, BANK
    status SMALLINT DEFAULT 1,  -- 1:pending, 2:paid, 3:failed, 4:cancelled, 5:refunded
    paid_at TIMESTAMP,
    callback_url VARCHAR(256),
    notify_url VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payment channel configuration
CREATE TABLE pay_channel_config (
    id BIGINT PRIMARY KEY,
    channel_name VARCHAR(32) NOT NULL,
    channel_type VARCHAR(32),  -- ALIPAY, WECHAT, STRIPE, PAYPAL, BANK
    app_id VARCHAR(128),
    app_secret VARCHAR(256),
    merchant_id VARCHAR(64),
    private_key TEXT,
    public_key TEXT,
    callback_url VARCHAR(256),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Refund
CREATE TABLE pay_refund (
    id BIGINT PRIMARY KEY,
    refund_no VARCHAR(64) NOT NULL UNIQUE,
    payment_no VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    refund_amount DECIMAL(12,2) NOT NULL,
    refund_reason VARCHAR(256),
    status SMALLINT DEFAULT 1,  -- 1:pending, 2:success, 3:failed
    refunded_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pay_refund_item (
    id BIGINT PRIMARY KEY,
    refund_id BIGINT NOT NULL,
    sku_id BIGINT,
    quantity INT,
    amount DECIMAL(12,2),
    reason VARCHAR(256)
);

-- Reconciliation
CREATE TABLE pay_reconciliation (
    id BIGINT PRIMARY KEY,
    recon_date DATE NOT NULL,
    channel VARCHAR(32) NOT NULL,
    total_count INT,
    total_amount DECIMAL(14,2),
    success_count INT,
    success_amount DECIMAL(14,2),
    diff_count INT,
    diff_amount DECIMAL(14,2),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pay_reconciliation_detail (
    id BIGINT PRIMARY KEY,
    recon_id BIGINT NOT NULL,
    payment_no VARCHAR(64),
    order_amount DECIMAL(12,2),
    channel_amount DECIMAL(12,2),
    diff DECIMAL(12,2),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payment log
CREATE TABLE pay_payment_log (
    id BIGINT PRIMARY KEY,
    payment_no VARCHAR(64),
    action VARCHAR(32),
    request_data JSONB,
    response_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pay_order_no ON pay_payment_order(order_no);
CREATE INDEX idx_pay_user ON pay_payment_order(user_id, status);
CREATE INDEX idx_pay_refund_no ON pay_refund(payment_no);
CREATE INDEX idx_pay_recon_date ON pay_reconciliation(recon_date, channel);
```

### 4.3 API Design

```
# Payment
POST   /api/v1/payments/create                  # Create payment order
GET    /api/v1/payments/{paymentNo}              # Get payment status
POST   /api/v1/payments/{paymentNo}/cancel       # Cancel payment
POST   /api/v1/payments/callback/alipay          # Alipay callback
POST   /api/v1/payments/callback/wechat          # WeChat callback
POST   /api/v1/payments/callback/stripe          # Stripe webhook
POST   /api/v1/payments/callback/paypal          # PayPal webhook

# Refund
POST   /api/v1/payments/refunds                  # Create refund
GET    /api/v1/payments/refunds/{refundNo}       # Get refund status
GET    /api/v1/payments/refunds/order/{orderNo}  # Refunds for order

# Reconciliation
GET    /api/v1/payments/reconciliation/daily      # Daily reconciliation
GET    /api/v1/payments/reconciliation/{date}     # Reconciliation by date
POST   /api/v1/payments/reconciliation/generate   # Generate reconciliation

# Payment methods
GET    /api/v1/payments/methods/user/{userId}    # User's payment methods
POST   /api/v1/payments/methods                  # Add payment method
DELETE /api/v1/payments/methods/{id}             # Remove payment method
```

### 4.4 Payment Channel Integration

| Channel | SDK/API | Features |
|---------|---------|----------|
| Alipay | Alipay SDK | QR code, H5, APP, Mini-program |
| WeChat Pay | WeChat Pay SDK | JSAPI, Native, H5, APP |
| Stripe | Stripe API | Card, Apple Pay, Google Pay |
| PayPal | PayPal REST API | PayPal balance, card |
| Bank Transfer | Manual verification | Corporate payments |

### 4.5 Payment Flow

```
Order Created → Payment Request (via Kafka)
    ↓
1. scm-payment creates payment_order (status: PENDING)
2. scm-payment calls payment channel API
3. Returns payment URL/QR code to user
    ↓
User Pays → Channel Callback
    ↓
1. Payment channel sends callback to scm-payment
2. scm-payment verifies signature
3. scm-payment updates payment_order (status: PAID)
4. scm-payment sends payment success event via Kafka
5. scm-order-center receives event → updates order status
6. scm-fulfillment receives event → starts fulfillment
```

---

## 5. scm-order-center — Unified Order Center

### 5.1 Service Overview

| Property | Value |
|----------|-------|
| Port | 8305 |
| Database | db_order (shared with scm-order, scm-mall) |
| Package | com.scmcloud.ordercenter |
| Dependencies | scm-order-api, scm-inventory-api, scm-promotion-api, scm-payment-api, scm-fulfillment-api, scm-common-web |

### 5.2 Database Schema

```sql
-- Unified order (partitioned by create_time)
CREATE TABLE oc_order (
    id BIGINT,
    order_no VARCHAR(64) NOT NULL,
    parent_order_no VARCHAR(64),
    channel VARCHAR(32),  -- MALL, POS, API, MINIAPP
    order_type VARCHAR(32),  -- NORMAL, FLASH_SALE, GROUP_BUY
    user_id VARCHAR(64) NOT NULL,
    seller_id BIGINT,
    status SMALLINT DEFAULT 1,
    total_amount DECIMAL(12,2),
    discount_amount DECIMAL(12,2),
    payable_amount DECIMAL(12,2),
    paid_amount DECIMAL(12,2),
    shipping_fee DECIMAL(10,2),
    receiver_name VARCHAR(64),
    receiver_phone VARCHAR(20),
    receiver_address VARCHAR(512),
    remark VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, create_time)
) PARTITION BY RANGE (create_time);

-- Order items
CREATE TABLE oc_order_item (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(256),
    sku_name VARCHAR(256),
    price DECIMAL(10,2),
    quantity INT,
    discount_amount DECIMAL(10,2),
    payable_amount DECIMAL(10,2),
    seller_id BIGINT,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order payment
CREATE TABLE oc_order_payment (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    payment_no VARCHAR(64),
    payment_method VARCHAR(32),
    amount DECIMAL(12,2),
    status SMALLINT DEFAULT 1,
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order logistics
CREATE TABLE oc_order_logistics (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    logistics_no VARCHAR(64),
    carrier VARCHAR(32),
    tracking_no VARCHAR(64),
    status SMALLINT DEFAULT 1,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order state machine history
CREATE TABLE oc_order_state_history (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    from_state VARCHAR(32),
    to_state VARCHAR(32) NOT NULL,
    operator VARCHAR(64),
    reason VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order events (event sourcing)
CREATE TABLE oc_order_event (
    id BIGINT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    order_no VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_data_json JSONB,
    version INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order split record
CREATE TABLE oc_order_split (
    id BIGINT PRIMARY KEY,
    parent_order_no VARCHAR(64) NOT NULL,
    child_order_no VARCHAR(64) NOT NULL,
    split_type VARCHAR(32),  -- SELLER, WAREHOUSE, CATEGORY, MANUAL
    split_reason VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_oc_order_user ON oc_order(user_id, created_at DESC);
CREATE INDEX idx_oc_order_seller ON oc_order(seller_id, status);
CREATE INDEX idx_oc_order_status ON oc_order(status, created_at DESC);
CREATE INDEX idx_oc_event_order ON oc_order_event(order_no, version);
```

### 5.3 Order State Machine

```
CREATED → PENDING_PAYMENT → PAID → PENDING_SHIP → SHIPPED → DELIVERED → COMPLETED
    ↓           ↓                ↓
CANCELLED   EXPIRED         CANCELLED (refund)
```

### 5.4 Saga Pattern

```
Create Order Saga:
1. Reserve Inventory (scm-inventory) → Success
2. Deduct Promotion (scm-promotion) → Success
3. Create Payment (scm-payment) → Success
4. Create Order (scm-order-center) → Success
    ↓
If any step fails:
1. Release Inventory
2. Restore Promotion
3. Cancel Payment
4. Cancel Order
```

---

## 6. scm-fulfillment — Fulfillment Center

### 6.1 Service Overview

| Property | Value |
|----------|-------|
| Port | 8306 |
| Database | db_warehouse |
| Package | com.scmcloud.fulfillment |
| Dependencies | scm-order-api, scm-warehouse-api, scm-inventory-api, scm-logistics-api, scm-common-web |

### 6.2 Database Schema

```sql
-- Fulfillment orders
CREATE TABLE ful_fulfillment_order (
    id BIGINT PRIMARY KEY,
    fulfillment_no VARCHAR(64) NOT NULL UNIQUE,
    order_no VARCHAR(64) NOT NULL,
    user_id VARCHAR(64),
    status SMALLINT DEFAULT 1,
    fulfillment_type VARCHAR(32),  -- NORMAL, DROPSHIP, CROSS_DOCK
    warehouse_id BIGINT,
    shipping_method VARCHAR(32),
    receiver_name VARCHAR(64),
    receiver_phone VARCHAR(20),
    receiver_address VARCHAR(512),
    total_items INT,
    total_weight DECIMAL(10,2),
    shipping_fee DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Fulfillment items
CREATE TABLE ful_fulfillment_item (
    id BIGINT PRIMARY KEY,
    fulfillment_no VARCHAR(64) NOT NULL,
    order_item_id BIGINT,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(256),
    quantity INT NOT NULL,
    picked_quantity INT DEFAULT 0,
    packed_quantity INT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3PL configuration
CREATE TABLE ful_3pl_config (
    id BIGINT PRIMARY KEY,
    provider_name VARCHAR(64) NOT NULL,
    provider_type VARCHAR(32),
    api_url VARCHAR(256),
    api_key VARCHAR(128),
    api_secret VARCHAR(256),
    supported_services JSONB,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ful_3pl_order (
    id BIGINT PRIMARY KEY,
    fulfillment_no VARCHAR(64) NOT NULL,
    provider_id BIGINT,
    provider_order_no VARCHAR(64),
    service_type VARCHAR(32),
    estimated_fee DECIMAL(10,2),
    actual_fee DECIMAL(10,2),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Real-time tracking
CREATE TABLE ful_tracking (
    id BIGINT PRIMARY KEY,
    fulfillment_no VARCHAR(64) NOT NULL,
    tracking_no VARCHAR(64),
    carrier VARCHAR(32),
    status VARCHAR(32),
    location VARCHAR(128),
    description VARCHAR(256),
    event_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Dropshipping
CREATE TABLE ful_dropship_config (
    id BIGINT PRIMARY KEY,
    supplier_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    min_quantity INT,
    lead_time INT,  -- hours
    shipping_method VARCHAR(32),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ful_dropship_order (
    id BIGINT PRIMARY KEY,
    fulfillment_no VARCHAR(64) NOT NULL,
    supplier_id BIGINT,
    supplier_order_no VARCHAR(64),
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Warehouse routing rules
CREATE TABLE ful_routing_rule (
    id BIGINT PRIMARY KEY,
    rule_name VARCHAR(128),
    rule_type VARCHAR(32),  -- INVENTORY, DISTANCE, COST, CAPACITY
    conditions_json JSONB,
    priority INT DEFAULT 0,
    warehouse_id BIGINT,
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ful_order_no ON ful_fulfillment_order(order_no);
CREATE INDEX idx_ful_tracking_no ON ful_tracking(fulfillment_no, event_time DESC);
CREATE INDEX idx_ful_3pl_order ON ful_3pl_order(fulfillment_no);
```

### 6.3 Fulfillment Chain

```
Order Received → Fulfillment Created
    ↓
1. Warehouse Routing
   - Calculate optimal warehouse based on:
     * Inventory availability
     * Distance to receiver
     * Warehouse capacity
     * Shipping cost
    ↓
2. Inventory Reservation
   - Reserve stock in selected warehouse
   - If insufficient → split to multiple warehouses
    ↓
3. Picking → Packing → Shipping → Tracking → Delivery
```

### 6.4 3PL Integration

| Provider | API | Services |
|----------|-----|----------|
| SF Express | SF-SDK | Express, Economy, Same-day |
| JD Logistics | JD-SDK | Standard, Express, Installation |
| ZTO Express | ZTO-SDK | Express, Economy |
| FedEx | FedEx-AP | International Express |
| DHL | DHL-API | International Express |

---

## 7. scm-search — Search Center

### 7.1 Service Overview

| Property | Value |
|----------|-------|
| Port | 8307 |
| Database | Elasticsearch (primary), Redis (cache) |
| Package | com.scmcloud.search |
| Dependencies | scm-product-api, scm-member-api, scm-promotion-api, scm-common-web |

### 7.2 Elasticsearch Index Schema

```json
{
  "mappings": {
    "properties": {
      "product_id": { "type": "keyword" },
      "seller_id": { "type": "keyword" },
      "title": { "type": "text", "analyzer": "ik_max_word" },
      "subtitle": { "type": "text" },
      "description": { "type": "text", "analyzer": "ik_max_word" },
      "category_id": { "type": "keyword" },
      "category_path": { "type": "keyword" },
      "brand_id": { "type": "keyword" },
      "brand_name": { "type": "keyword" },
      "price": { "type": "float" },
      "original_price": { "type": "float" },
      "sales_count": { "type": "integer" },
      "rating": { "type": "float" },
      "review_count": { "type": "integer" },
      "main_image": { "type": "keyword" },
      "images": { "type": "keyword" },
      "attributes": { "type": "object" },
      "skus": {
        "type": "nested",
        "properties": {
          "sku_id": { "type": "keyword" },
          "price": { "type": "float" },
          "stock": { "type": "integer" },
          "attributes": { "type": "object" }
        }
      },
      "tags": { "type": "keyword" },
      "status": { "type": "keyword" },
      "created_at": { "type": "date" },
      "suggest": { "type": "completion" }
    }
  }
}
```

### 7.3 AI-Powered Features

1. **NLP Search**: "red dress under 500 yuan for party" → structured query
2. **Image Search**: Upload image → CNN feature extraction → vector similarity search
3. **Voice Search**: Speech-to-text → NLP → search
4. **Personalized Ranking**: Based on user behavior, preferences, purchase history

### 7.4 Search Ranking Algorithm

```
Final Score = Text Relevance × 0.3
            + Sales Performance × 0.2
            + Rating × 0.15
            + Personalization × 0.2
            + Freshness × 0.1
            + Promotion Boost × 0.05
```

---

## 8. Port Allocation Summary

| Service | Port | Status |
|---------|------|--------|
| Gateway | 8761 | Existing |
| Auth | 8106 | Existing |
| System | 8081 | Existing |
| Product | 8201 | Existing |
| Inventory | 8202 | Existing |
| Order | 8203 | Existing |
| Warehouse | 8204 | Existing |
| Logistics | 8205 | Existing |
| Supplier | 8206 | Existing |
| Purchase | 8207 | Existing |
| Finance | 8208 | Existing |
| Approval | 8209 | Existing |
| Notify | 8210 | Existing |
| **Mall** | 8301 | Year 1 |
| **Member** | 8302 | Year 1 |
| **Promotion** | 8303 | Year 1 |
| **Payment** | 8304 | Year 1 |
| **Order Center** | 8305 | Year 1 |
| **Fulfillment** | 8306 | Year 1 |
| **Search** | 8307 | Year 1 |

---

## 9. Database Allocation Summary

| Database | Services | New Tables |
|----------|----------|------------|
| db_user | member, auth | mem_* |
| db_product | product, search | (ES index) |
| db_order | order, mall, order-center | mall_*, oc_* |
| db_warehouse | warehouse, fulfillment | ful_* |
| db_finance | finance, payment | pay_* |
| db_promotion | promotion | pro_* |

---

## 10. Integration Summary

| From | To | Mechanism | Purpose |
|------|-----|-----------|---------|
| scm-mall | scm-member | REST/Dubbo | User auth, profile |
| scm-mall | scm-product | Dubbo | Product catalog |
| scm-mall | scm-promotion | Dubbo | Apply promotions |
| scm-mall | scm-order-center | Dubbo | Create orders |
| scm-order-center | scm-payment | Kafka | Payment requests |
| scm-order-center | scm-fulfillment | Kafka | Fulfillment requests |
| scm-order-center | scm-inventory | Dubbo | Stock deduction |
| scm-fulfillment | scm-warehouse | Dubbo | Warehouse operations |
| scm-fulfillment | scm-logistics | Dubbo | Shipping |
| scm-promotion | scm-decision-engine | Dubbo | AI recommendations |
| scm-search | scm-product | Kafka | Index updates |

---

## 11. Implementation Order

Based on dependencies, the recommended implementation order is:

1. **scm-member** (Week 1-6) — Foundation for user system
2. **scm-promotion** (Week 1-8) — Can start in parallel with member
3. **scm-payment** (Week 7-12) — Depends on member
4. **scm-search** (Week 7-12) — Can start in parallel with payment
5. **scm-order-center** (Week 9-16) — Depends on member, promotion, payment
6. **scm-mall** (Week 13-24) — Depends on all above
7. **scm-fulfillment** (Week 17-24) — Depends on order-center

---

## 12. Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Service complexity | Strict layering, clear boundaries, well-defined interfaces |
| Data consistency | Saga pattern, eventual consistency, compensation transactions |
| Performance | Caching (Redis), async processing (Kafka), read-write separation |
| Integration complexity | API gateway, service mesh, circuit breakers (Sentinel) |
| Payment security | Signature verification, idempotency, reconciliation |
