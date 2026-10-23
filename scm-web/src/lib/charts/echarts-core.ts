import * as echarts from "echarts/core";
import { BarChart, LineChart, PieChart } from "echarts/charts";
import {
  DatasetComponent,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  TransformComponent,
} from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";

/**
 * Tree-shaken ECharts entry point.
 *
 * Never import `echarts` or `echarts-for-react` directly — the full bundle is
 * ~1 MB. Register only the charts/components a screen needs here and import
 * this module from the lazy chart wrapper.
 */
echarts.use([
  BarChart,
  LineChart,
  PieChart,
  DatasetComponent,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  TransformComponent,
  CanvasRenderer,
]);

export { echarts };
export type { EChartsCoreOption } from "echarts/core";
