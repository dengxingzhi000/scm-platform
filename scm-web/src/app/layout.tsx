import { ReactNode } from "react";

import "./globals.css";

// AG Grid's stylesheets must be imported statically (not from the lazy grid
// wrapper) so they are present before the grid mounts.
import "ag-grid-community/styles/ag-grid.css";
import "ag-grid-community/styles/ag-theme-quartz.css";

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="zh-CN" suppressHydrationWarning>
      <body>{children}</body>
    </html>
  );
}
