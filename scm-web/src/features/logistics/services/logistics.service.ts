import { logisticsApiService } from "@/lib/api/services/logistics";

import {
  toTrackingEvent,
  toWaybillPage,
} from "../lib/mappers";
import type { TrackingEvent, Waybill } from "../types";

const LIST_PAGE_SIZE = 100;

export const logisticsService = {
  listWaybills: async (): Promise<Waybill[]> => {
    const page = await logisticsApiService.pageWaybills(1, LIST_PAGE_SIZE);
    return toWaybillPage(page).records;
  },

  getTracking: async (waybillId: string): Promise<TrackingEvent[]> =>
    (await logisticsApiService.listTracking(waybillId)).map(toTrackingEvent),
};
