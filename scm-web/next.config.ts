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
};

const nextConfig: NextConfig = {
  output: "standalone",
  reactStrictMode: true,
  async rewrites() {
    return [
      { source: "/auth/:path*", destination: `${targets.auth}/:path*` },
      { source: "/system/:path*", destination: `${targets.system}/:path*` },
      { source: "/common/:path*", destination: `${targets.system}/:path*` },
      { source: "/scm-product/:path*", destination: `${targets.product}/:path*` },
      { source: "/scm-inventory/:path*", destination: `${targets.inventory}/:path*` },
      { source: "/scm-order/:path*", destination: `${targets.order}/:path*` },
    ];
  },
};

export default withNextIntl(nextConfig);
