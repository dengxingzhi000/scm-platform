"use client";

import InventoryAlerts from "@/features/dashboard/components/inventory-alerts";
import KPICards from "@/features/dashboard/components/kpi-cards";
import OrderStatusChart from "@/features/dashboard/components/order-status-chart";
import RecentOrders from "@/features/dashboard/components/recent-orders";
import SalesChart from "@/features/dashboard/components/sales-chart";
import { useDashboardStats } from "@/features/dashboard/hooks";
import { PageHeader } from "@/components/ui/page-header";

export default function DashboardClient() {
  const { data, isLoading } = useDashboardStats();

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        eyebrow="运营总览"
        title="仪表盘"
        description="今日经营指标、订单结构与库存风险"
      />

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
