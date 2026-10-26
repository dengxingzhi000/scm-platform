import { systemService } from "@/lib/api/services/system";
import { toRole, toRolePayload } from "../lib/mappers";
import type { Role } from "../types";

export const roleService = {
  list: async (): Promise<Role[]> =>
    (await systemService.roles.listAll()).map(toRole),

  getById: async (id: string): Promise<Role> => {
    const roles = await systemService.roles.listAll();
    const role = roles.find((item) => item.id === id);
    if (!role) throw new Error("Role not found");
    const permissionIds = await systemService.roles.permissions(id);
    return toRole({ ...role, permissionIds });
  },

  create: async (data: Partial<Role>): Promise<void> => {
    const payload = toRolePayload(data);
    await systemService.roles.create(payload);

    if (payload.permissionIds?.length) {
      const created = (await systemService.roles.listAll()).find(
        (role) => role.roleCode === payload.roleCode
      );
      if (created) {
        await systemService.roles.grantPermissions(
          created.id,
          payload.permissionIds
        );
      }
    }
  },

  update: async (id: string, data: Partial<Role>): Promise<void> => {
    const payload = toRolePayload(data);
    await systemService.roles.update(id, payload);
    await systemService.roles.grantPermissions(
      id,
      payload.permissionIds ?? []
    );
  },

  delete: async (id: string): Promise<void> => {
    await systemService.roles.delete(id);
  },

  permissions: (id: string): Promise<string[]> =>
    systemService.roles.permissions(id),
};
