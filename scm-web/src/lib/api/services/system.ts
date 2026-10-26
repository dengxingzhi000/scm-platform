import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  ApiPermissionDTO,
  DeptDTO,
  DictItemDTO,
  DictTypeDTO,
  DictTypeQuery,
  PageResult,
  PermissionDTO,
  RoleDTO,
  RoleQuery,
  TemporaryRoleGrantDTO,
  UserDTO,
  UserQuery,
} from "../contracts/system";

const SYSTEM_BASE = "/system/api/system";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, data?: unknown) =>
  unwrap<T>(apiClient.post(url, data));

const postWithParams = <T>(url: string, params: object) =>
  unwrap<T>(apiClient.post(url, null, { params }));

const put = <T>(url: string, data?: unknown) =>
  unwrap<T>(apiClient.put(url, data));

const del = <T>(url: string) => unwrap<T>(apiClient.delete(url));

export const systemService = {
  users: {
    list: (query?: UserQuery) =>
      get<PageResult<UserDTO>>(`${SYSTEM_BASE}/users`, query),
    getById: (id: string) => get<UserDTO>(`${SYSTEM_BASE}/users/${id}`),
    create: (data: UserDTO) => post<void>(`${SYSTEM_BASE}/users`, data),
    update: (id: string, data: UserDTO) =>
      put<void>(`${SYSTEM_BASE}/users/${id}`, data),
    delete: (id: string) => del<void>(`${SYSTEM_BASE}/users/${id}`),
    changePassword: (data: {
      oldPassword: string;
      newPassword: string;
      confirmPassword?: string;
    }) => post<void>(`${SYSTEM_BASE}/users/change-password`, data),
    resetPassword: (id: string) =>
      post<string>(`${SYSTEM_BASE}/users/${id}/reset-password`),
    grantRoles: (id: string, roleIds: string[]) =>
      post<void>(`${SYSTEM_BASE}/users/${id}/grant-roles`, roleIds),
    lock: (id: string, lock: boolean) =>
      postWithParams<void>(`${SYSTEM_BASE}/users/${id}/lock`, { lock }),
    forceLogout: (id: string, reason: string) =>
      postWithParams<void>(`${SYSTEM_BASE}/users/${id}/force-logout`, {
        reason,
      }),
    grantTemporaryRoles: (id: string, data: TemporaryRoleGrantDTO) =>
      post<void>(`${SYSTEM_BASE}/users/${id}/grant-temporary-roles`, data),
    extendTemporaryRole: (
      userId: string,
      roleId: string,
      newExpireTime: string
    ) =>
      postWithParams<void>(
        `${SYSTEM_BASE}/users/${userId}/extend-temporary-role/${roleId}`,
        { newExpireTime }
      ),
    terminateTemporaryRole: (userId: string, roleId: string) =>
      post<void>(
        `${SYSTEM_BASE}/users/${userId}/terminate-temporary-role/${roleId}`
      ),
    temporaryRoles: (id: string) =>
      get<Array<Record<string, unknown>>>(
        `${SYSTEM_BASE}/users/${id}/temporary-roles`
      ),
    statistics: (id: string) =>
      get<Record<string, unknown>>(`${SYSTEM_BASE}/users/${id}/statistics`),
    updateLastLogin: (userId: string, ipAddress: string) =>
      get<void>(
        `${SYSTEM_BASE}/users/${userId}/update-login`,
        { ipAddress }
      ),
  },

  roles: {
    list: (query?: RoleQuery) =>
      get<PageResult<RoleDTO>>(`${SYSTEM_BASE}/roles`, query),
    listAll: () => get<RoleDTO[]>(`${SYSTEM_BASE}/roles/all`),
    create: (data: RoleDTO) => post<void>(`${SYSTEM_BASE}/roles`, data),
    update: (id: string, data: RoleDTO) =>
      put<void>(`${SYSTEM_BASE}/roles/${id}`, data),
    delete: (id: string) => del<void>(`${SYSTEM_BASE}/roles/${id}`),
    grantPermissions: (id: string, permissionIds: string[]) =>
      post<void>(`${SYSTEM_BASE}/roles/${id}/grant-permissions`, permissionIds),
    permissions: (id: string) =>
      get<string[]>(`${SYSTEM_BASE}/roles/${id}/permissions`),
  },

  depts: {
    tree: () => get<DeptDTO[]>(`${SYSTEM_BASE}/depts/tree`),
    create: (data: DeptDTO) => post<void>(`${SYSTEM_BASE}/depts`, data),
    update: (id: string, data: DeptDTO) =>
      put<void>(`${SYSTEM_BASE}/depts/${id}`, data),
    delete: (id: string) => del<void>(`${SYSTEM_BASE}/depts/${id}`),
  },

  dicts: {
    listTypes: (query?: DictTypeQuery) =>
      get<DictTypeDTO[]>(`${SYSTEM_BASE}/dicts/types`, query),
    getTypeByCode: (dictCode: string) =>
      get<DictTypeDTO>(`${SYSTEM_BASE}/dicts/types/code/${dictCode}`),
    createType: (data: DictTypeDTO) =>
      post<void>(`${SYSTEM_BASE}/dicts/types`, data),
    updateType: (data: DictTypeDTO) =>
      put<void>(`${SYSTEM_BASE}/dicts/types`, data),
    deleteType: (id: string) =>
      del<void>(`${SYSTEM_BASE}/dicts/types/${id}`),
    listItems: (dictCode: string) =>
      get<DictItemDTO[]>(`${SYSTEM_BASE}/dicts/items/${dictCode}`),
    createItem: (data: DictItemDTO) =>
      post<void>(`${SYSTEM_BASE}/dicts/items`, data),
    updateItem: (data: DictItemDTO) =>
      put<void>(`${SYSTEM_BASE}/dicts/items`, data),
    deleteItem: (id: string) =>
      del<void>(`${SYSTEM_BASE}/dicts/items/${id}`),
  },

  permissions: {
    tree: () => get<PermissionDTO[]>(`${SYSTEM_BASE}/permissions/tree`),
    getById: (id: string) =>
      get<PermissionDTO>(`${SYSTEM_BASE}/permissions/${id}`),
    create: (data: PermissionDTO) =>
      post<void>(`${SYSTEM_BASE}/permissions`, data),
    update: (id: string, data: PermissionDTO) =>
      put<void>(`${SYSTEM_BASE}/permissions/${id}`, data),
    delete: (id: string) => del<void>(`${SYSTEM_BASE}/permissions/${id}`),
    userPermissions: (userId: string) =>
      get<string[]>(`${SYSTEM_BASE}/permissions/user/${userId}`),
    findByUrl: (url: string, method: string) =>
      get<string[]>(`${SYSTEM_BASE}/permissions/find-by-url`, {
        url,
        method,
      }),
    apiPermissions: () =>
      get<ApiPermissionDTO[]>(`${SYSTEM_BASE}/permissions/api`),
  },
};
