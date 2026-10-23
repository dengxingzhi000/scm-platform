"use client";

import InventoryAlerts from "@/features/dashboard/components/inventory-alerts";
import KPICards from "@/features/dashboard/components/kpi-cards";
import OrderStatusChart from "@/features/dashboard/components/order-status-chart";
import RecentOrders from "@/features/dashboard/components/recent-orders";
import SalesChart from "@/features/dashboard/components/sales-chart";
import { useDashboardStats } from "@/features/dashboard/hooks";

export default function DashboardClient() {
  const { data, isLoading } = useDashboardStats();

  return (
    <div className="flex flex-col gap-4">
      <KPICards kpis={data?.kpis ?? []} loading={isLoading} />

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <SalesChart
            data={data?.salesTrend ?? { dates: [], series: [] }}
            loading={isLoading}
          />
        </div>
        <div>
          <OrderStatusChart
            data={data?.orderStatus ?? []}
            loading={isLoading}
          />
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <RecentOrders orders={data?.recentOrders ?? []} loading={isLoading} />
        <InventoryAlerts alerts={data?.inventoryAlerts ?? []} loading={isLoading} />
      </div>
    </div>
  );
}
