import {
  dehydrate,
  HydrationBoundary,
  QueryClient,
} from "@tanstack/react-query";

import { QUERY_KEYS } from "@/constants/query-keys";
import { dashboardService } from "@/features/dashboard/services/dashboard.service";
import DashboardClient from "./dashboard-client";

/**
 * Server Component entry point: warm the React Query cache before the client
 * renders. The dashboard stats service is a local/mock aggregation today; when
 * it moves to the gateway, the prefetch must forward the auth + tenant cookies.
 */
export default async function DashboardPage() {
  const queryClient = new QueryClient();

  await queryClient.prefetchQuery({
    queryKey: QUERY_KEYS.DASHBOARD.STATS,
    queryFn: () => dashboardService.getStats(),
  });

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <DashboardClient />
    </HydrationBoundary>
  );
}
