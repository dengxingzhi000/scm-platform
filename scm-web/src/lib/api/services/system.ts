import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  ApiPermissionDTO,
  AvailableActionDTO,
  ConflictDTO,
  DecisionRuleDTO,
  DeptDTO,
  DictItemDTO,
  DictTypeDTO,
  DictTypeQuery,
  PageResult,
  PermissionDTO,
  RoleDTO,
  RoleQuery,
  StatusDictDTO,
  StatusTransitionDTO,
  TemporaryRoleGrantDTO,
  TransitionCheckDTO,
  TransitionResultDTO,
  UserDTO,
  UserQuery,
  WeightProfileDTO,
} from "../contracts/system";

const SYSTEM_BASE = "/system/api/system";

/** scm-system's decision-matrix controllers are mounted on `/api/v1`, not `/api/system`. */
const DECISION_BASE = "/system/api/v1/decision";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, data?: unknown) =>
  unwrap<T>(apiClient.post(url, data));

const postWithParams = <T>(url: string, params: object) =>
  unwrap<T>(apiClient.post(url, null, { params }));

const put = <T>(url: string, data?: unknown) =>
  unwrap<T>(apiClient.put(url, data));

const putWithParams = <T>(url: string, params: object) =>
  unwrap<T>(apiClient.put(url, null, { params }));

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

  /**
   * The status machine lives under `/api/system/status/**`, so it shares the
   * gateway's explicit `/system/**` route.
   */
  status: {
    listDict: (bizType: string) =>
      get<StatusDictDTO[]>(`${SYSTEM_BASE}/status/dict/${bizType}`),
    getDict: (bizType: string, statusCode: string) =>
      get<StatusDictDTO>(
        `${SYSTEM_BASE}/status/dict/${bizType}/${statusCode}`
      ),
    /** Update takes the whole row, `id` included — it is not a path variable. */
    createDict: (data: StatusDictDTO) =>
      post<void>(`${SYSTEM_BASE}/status/dict`, data),
    updateDict: (data: StatusDictDTO) =>
      put<void>(`${SYSTEM_BASE}/status/dict`, data),
    deleteDict: (id: string) => del<void>(`${SYSTEM_BASE}/status/dict/${id}`),

    listTransitions: (bizType: string) =>
      get<StatusTransitionDTO[]>(
        `${SYSTEM_BASE}/status/transitions/${bizType}`
      ),
    createTransition: (data: StatusTransitionDTO) =>
      post<void>(`${SYSTEM_BASE}/status/transitions`, data),
    updateTransition: (data: StatusTransitionDTO) =>
      put<void>(`${SYSTEM_BASE}/status/transitions`, data),
    deleteTransition: (id: string) =>
      del<void>(`${SYSTEM_BASE}/status/transitions/${id}`),

    nextStatuses: (bizType: string, currentStatus: string) =>
      get<string[]>(`${SYSTEM_BASE}/status/next-statuses`, {
        bizType,
        currentStatus,
      }),
    actions: (bizType: string, currentStatus: string) =>
      get<AvailableActionDTO[]>(`${SYSTEM_BASE}/status/actions`, {
        bizType,
        currentStatus,
      }),
    check: (bizType: string, fromStatus: string, toStatus: string) =>
      get<TransitionCheckDTO>(`${SYSTEM_BASE}/status/check`, {
        bizType,
        fromStatus,
        toStatus,
      }),
    /** The transition itself is a POST whose arguments are query params. */
    transition: (bizType: string, fromStatus: string, actionCode: string) =>
      postWithParams<TransitionResultDTO>(`${SYSTEM_BASE}/status/transition`, {
        bizType,
        fromStatus,
        actionCode,
      }),
  },

  /**
   * Decision-matrix endpoints sit at `/api/v1/decision/**` on scm-system, not
   * under `/api/system/**`, so the gateway prefix differs from SYSTEM_BASE even
   * though `/system/**` still matches and strips to the same path.
   */
  decision: {
    listRules: (engineType: string) =>
      get<DecisionRuleDTO[]>(`${DECISION_BASE}/rules/${engineType}`),
    createRule: (data: DecisionRuleDTO) =>
      post<DecisionRuleDTO>(`${DECISION_BASE}/rules`, data),
    updateRule: (id: string, data: DecisionRuleDTO) =>
      put<DecisionRuleDTO>(`${DECISION_BASE}/rules/${id}`, data),
    deleteRule: (id: string) => del<void>(`${DECISION_BASE}/rules/${id}`),
    toggleRule: (id: string, enabled: boolean) =>
      putWithParams<void>(`${DECISION_BASE}/rules/${id}/toggle`, { enabled }),
    detectConflicts: (engineType: string) =>
      post<ConflictDTO[]>(`${DECISION_BASE}/rules/conflicts/${engineType}`),

    listWeights: (engineType: string, scene?: string) =>
      get<WeightProfileDTO[]>(`${DECISION_BASE}/weights/${engineType}`, {
        scene,
      }),
    weightVersions: (engineType: string) =>
      get<WeightProfileDTO[]>(`${DECISION_BASE}/weights/${engineType}/versions`),
    createWeight: (data: WeightProfileDTO) =>
      post<WeightProfileDTO>(`${DECISION_BASE}/weights`, data),
    activateWeight: (id: string) =>
      put<void>(`${DECISION_BASE}/weights/${id}/activate`),
    deleteWeight: (id: string) => del<void>(`${DECISION_BASE}/weights/${id}`),
  },
};
