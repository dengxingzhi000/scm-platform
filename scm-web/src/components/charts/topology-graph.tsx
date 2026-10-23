"use client";

import dynamic from "next/dynamic";

import { Skeleton } from "@/components/ui/skeleton";

/**
 * Browser-only G6 wrapper (topology / supply-chain relationship graphs).
 */
const TopologyGraph = dynamic(() => import("./graph-client"), {
  ssr: false,
  loading: () => <Skeleton className="h-80 w-full" />,
});

export default TopologyGraph;
