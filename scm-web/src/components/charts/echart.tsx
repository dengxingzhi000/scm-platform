"use client";

import dynamic from "next/dynamic";

import { Skeleton } from "@/components/ui/skeleton";

/**
 * Browser-only ECharts wrapper.
 *
 * `ssr: false` is required: ECharts touches `window` during init. Import this
 * component from other client components; server components cannot use
 * `next/dynamic` with `ssr: false`.
 */
const EChart = dynamic(() => import("./echart-client"), {
  ssr: false,
  loading: () => <Skeleton className="h-80 w-full" />,
});

export default EChart;
