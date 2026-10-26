import { systemService } from "@/lib/api/services/system";
import { toDepartment, toDeptPayload } from "../lib/mappers";
import type { Department } from "../types";

export const deptService = {
  list: async (): Promise<Department[]> =>
    (await systemService.depts.tree()).map(toDepartment),

  create: async (data: Partial<Department>): Promise<void> => {
    await systemService.depts.create(toDeptPayload(data));
  },

  update: async (id: string, data: Partial<Department>): Promise<void> => {
    await systemService.depts.update(id, toDeptPayload(data));
  },

  delete: async (id: string): Promise<void> => {
    await systemService.depts.delete(id);
  },
};
