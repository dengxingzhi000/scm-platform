import DashboardClient from "./dashboard-client";

/**
 * The dashboard now aggregates real gateway data in the browser. The previous
 * server-side prefetch could not forward the auth/tenant cookies, so it is
 * intentionally a plain client-rendered page.
 */
export default function DashboardPage() {
  return <DashboardClient />;
}
