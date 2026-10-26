import { systemService } from "@/lib/api/services/system";
import { toPermission, toPermissionPayload } from "../lib/mappers";
import type { Permission } from "../types";

export const permissionService = {
  list: async (): Promise<Permission[]> =>
    (await systemService.permissions.tree()).map(toPermission),

  create: async (data: Partial<Permission>): Promise<void> => {
    await systemService.permissions.create(toPermissionPayload(data));
  },

  update: async (id: string, data: Partial<Permission>): Promise<void> => {
    await systemService.permissions.update(
      id,
      toPermissionPayload({ ...data, id })
    );
  },

  delete: async (id: string): Promise<void> => {
    await systemService.permissions.delete(id);
  },
};
