import type { Metadata, Viewport } from "next";
import { ReactNode } from "react";

import "./globals.css";

// AG Grid's stylesheets must be imported statically (not from the lazy grid
// wrapper) so they are present before the grid mounts.
import "ag-grid-community/styles/ag-grid.css";
import "ag-grid-community/styles/ag-theme-quartz.css";

// Dark Reader 等扩展会在 React 水合前改写内联样式（注入 --darkreader-inline-*、
// data-darkreader-* 属性），导致 hydration mismatch。darkreader-lock 是 Dark Reader
// 官方的整站退出标记；color-scheme 则声明本站自带明暗主题。
export const metadata: Metadata = {
  other: {
    "darkreader-lock": "true",
  },
};

export const viewport: Viewport = {
  colorScheme: "dark light",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="zh-CN" suppressHydrationWarning>
      <body>{children}</body>
    </html>
  );
}
