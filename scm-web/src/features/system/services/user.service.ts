import { systemService } from "@/lib/api/services/system";
import {
  toPageResult,
  toUser,
  toUserPayload,
  type UserFormInput,
} from "../lib/mappers";
import type { PageParams, PageResult, User } from "../types";

const toUserStatus = (status?: string): number | undefined => {
  if (status === "active") return 1;
  if (status === "disabled") return 0;
  if (status === "locked") return 2;
  return undefined;
};

export const userService = {
  list: async (params?: PageParams): Promise<PageResult<User>> => {
    const page = await systemService.users.list({
      page: params?.current ?? 1,
      size: params?.pageSize ?? 20,
      username: params?.keyword,
      status: toUserStatus(params?.status),
    });
    return toPageResult(page, toUser);
  },

  getById: async (id: string): Promise<User> =>
    toUser(await systemService.users.getById(id)),

  create: async (data: UserFormInput): Promise<void> => {
    await systemService.users.create(toUserPayload(data));
  },

  update: async (id: string, data: UserFormInput): Promise<void> => {
    await systemService.users.update(id, toUserPayload(data));
  },

  delete: async (id: string): Promise<void> => {
    await systemService.users.delete(id);
  },

  resetPassword: (id: string): Promise<string> =>
    systemService.users.resetPassword(id),

  grantRoles: (id: string, roleIds: string[]): Promise<void> =>
    systemService.users.grantRoles(id, roleIds),

  lock: (id: string, lock: boolean): Promise<void> =>
    systemService.users.lock(id, lock),

  forceLogout: (id: string, reason: string): Promise<void> =>
    systemService.users.forceLogout(id, reason),

  temporaryRoles: (id: string): Promise<Array<Record<string, unknown>>> =>
    systemService.users.temporaryRoles(id),

  statistics: (id: string): Promise<Record<string, unknown>> =>
    systemService.users.statistics(id),
};
