"use client";

import dynamic from "next/dynamic";

import { Skeleton } from "@/components/ui/skeleton";

/**
 * Browser-only Recharts wrapper used by KPI cards and compact trend widgets.
 */
const Sparkline = dynamic(() => import("./sparkline-client"), {
  ssr: false,
  loading: () => <Skeleton className="h-10 w-full" />,
});

export default Sparkline;
