"use client";

import { AgGridReact } from "ag-grid-react";
import type { ColDef, GridOptions } from "ag-grid-community";

import { cn } from "@/lib/utils";

export interface DataGridClientProps<TData extends object = object> {
  rowData: TData[];
  columnDefs: ColDef<TData>[];
  loading?: boolean;
  height?: number;
  className?: string;
  gridOptions?: GridOptions<TData>;
}

/**
 * AG Grid renderer. Always loaded through `@/components/data-grid/data-grid`
 * (dynamic + `ssr: false`). The AG Grid stylesheets are imported statically in
 * the root layout.
 */
export default function DataGridClient<TData extends object>({
  rowData,
  columnDefs,
  loading,
  height = 600,
  className,
  gridOptions,
}: DataGridClientProps<TData>) {
  return (
    <div
      className={cn("ag-theme-quartz w-full", className)}
      style={{ height }}
    >
      <AgGridReact<TData>
        theme="legacy"
        rowData={rowData}
        columnDefs={columnDefs}
        loading={loading}
        gridOptions={gridOptions}
        defaultColDef={{
          flex: 1,
          minWidth: 120,
          sortable: true,
          resizable: true,
          filter: true,
        }}
      />
    </div>
  );
}
