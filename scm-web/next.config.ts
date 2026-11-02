import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin("./src/i18n/request.ts");

/**
 * The browser cannot always reach the backend hosts directly, so the frontend calls
 * same-origin paths and Next proxies them. In development the proxy goes straight to each
 * service (the services validate the HS512 token issued by scm-auth); production points at
 * the gateway. Every prefix is stripped, mirroring the gateway's StripPrefix=1 routes.
 */
const targets = {
  auth: process.env.API_AUTH_TARGET ?? "http://localhost:8106",
  system: process.env.API_SYSTEM_TARGET ?? "http://localhost:8081",
  product: process.env.API_PRODUCT_TARGET ?? "http://localhost:8201",
  inventory: process.env.API_INVENTORY_TARGET ?? "http://localhost:8202",
  order: process.env.API_ORDER_TARGET ?? "http://localhost:8203",
  warehouse: process.env.API_WAREHOUSE_TARGET ?? "http://localhost:8204",
  logistics: process.env.API_LOGISTICS_TARGET ?? "http://localhost:8205",
  supplier: process.env.API_SUPPLIER_TARGET ?? "http://localhost:8206",
  purchase: process.env.API_PURCHASE_TARGET ?? "http://localhost:8207",
  finance: process.env.API_FINANCE_TARGET ?? "http://localhost:8208",
  approval: process.env.API_APPROVAL_TARGET ?? "http://localhost:8209",
  audit: process.env.API_AUDIT_TARGET ?? "http://localhost:8210",
  notify: process.env.API_NOTIFY_TARGET ?? "http://localhost:8211",
  tenant: process.env.API_TENANT_TARGET ?? "http://localhost:8212",
  document: process.env.API_DOCUMENT_TARGET ?? "http://localhost:8213",
  file: process.env.API_FILE_TARGET ?? "http://localhost:8214",
  analytics: process.env.API_ANALYTICS_TARGET ?? "http://localhost:8308",
  mall: process.env.API_MALL_TARGET ?? "http://localhost:8301",
  member: process.env.API_MEMBER_TARGET ?? "http://localhost:8302",
  promotion: process.env.API_PROMOTION_TARGET ?? "http://localhost:8303",
  payment: process.env.API_PAYMENT_TARGET ?? "http://localhost:8304",
  orderCenter: process.env.API_ORDER_CENTER_TARGET ?? "http://localhost:8305",
  fulfillment: process.env.API_FULFILLMENT_TARGET ?? "http://localhost:8306",
  search: process.env.API_SEARCH_TARGET ?? "http://localhost:8307",
};

const nextConfig: NextConfig = {
  output: "standalone",
  reactStrictMode: true,
  async rewrites() {
    return [
      { source: "/auth/:path*", destination: `${targets.auth}/:path*` },
      // Scoped to `/system/api`: the 系统管理 pages are on `/system/*`, so a bare
      // `/system/:path*` source would proxy those page navigations to scm-system.
      { source: "/system/api/:path*", destination: `${targets.system}/api/:path*` },
      { source: "/common/:path*", destination: `${targets.system}/:path*` },
      { source: "/scm-product/:path*", destination: `${targets.product}/:path*` },
      { source: "/scm-inventory/:path*", destination: `${targets.inventory}/:path*` },
      { source: "/scm-order/:path*", destination: `${targets.order}/:path*` },
      { source: "/scm-warehouse/:path*", destination: `${targets.warehouse}/:path*` },
      { source: "/scm-logistics/:path*", destination: `${targets.logistics}/:path*` },
      { source: "/scm-supplier/:path*", destination: `${targets.supplier}/:path*` },
      { source: "/scm-purchase/:path*", destination: `${targets.purchase}/:path*` },
      { source: "/scm-finance/:path*", destination: `${targets.finance}/:path*` },
      { source: "/scm-approval/:path*", destination: `${targets.approval}/:path*` },
      { source: "/scm-audit/:path*", destination: `${targets.audit}/:path*` },
      { source: "/scm-notify/:path*", destination: `${targets.notify}/:path*` },
      { source: "/scm-tenant/:path*", destination: `${targets.tenant}/:path*` },
      { source: "/scm-document/:path*", destination: `${targets.document}/:path*` },
      { source: "/scm-file/:path*", destination: `${targets.file}/:path*` },
      { source: "/scm-analytics/:path*", destination: `${targets.analytics}/:path*` },
      { source: "/scm-mall/:path*", destination: `${targets.mall}/:path*` },
      { source: "/scm-member/:path*", destination: `${targets.member}/:path*` },
      { source: "/scm-promotion/:path*", destination: `${targets.promotion}/:path*` },
      { source: "/scm-payment/:path*", destination: `${targets.payment}/:path*` },
      { source: "/scm-order-center/:path*", destination: `${targets.orderCenter}/:path*` },
      { source: "/scm-fulfillment/:path*", destination: `${targets.fulfillment}/:path*` },
      { source: "/scm-search/:path*", destination: `${targets.search}/:path*` },
    ];
  },
};

export default withNextIntl(nextConfig);
