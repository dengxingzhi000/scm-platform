"use client";

import dynamic from "next/dynamic";

import { Skeleton } from "@/components/ui/skeleton";

/**
 * Browser-only AG Grid wrapper. Use TanStack Table for ordinary lists and this
 * component only for the heavyweight grids (large datasets, column groups,
 * pivoting) where AG Grid is explicitly required.
 */
const DataGrid = dynamic(() => import("./ag-grid-client"), {
  ssr: false,
  loading: () => <Skeleton className="h-96 w-full" />,
});

export default DataGrid;
