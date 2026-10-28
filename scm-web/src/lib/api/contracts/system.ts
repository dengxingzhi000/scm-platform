export interface PageResult<T> {
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
  records: T[];
}

export interface PageQuery {
  page?: number;
  size?: number;
}

export interface UserDTO {
  id: string;
  username: string;
  password?: string;
  realName: string;
  idCard?: string;
  email?: string;
  phone?: string;
  avatar?: string;
  status?: number;
  deptId?: string;
  deptName?: string;
  userLevel?: number;
  accountType?: number;
  loginAttempts?: number;
  lockedUntil?: string;
  passwordExpireTime?: string;
  forceChangePassword?: boolean;
  twoFactorEnabled?: boolean;
  createTime?: string;
  updateTime?: string;
  lastLoginTime?: string;
  lastLoginIp?: string;
  remark?: string;
  roleIds?: string[];
  roleNames?: string[];
  permissions?: string[];
}

export interface UserQuery extends PageQuery {
  username?: string;
  status?: number;
}

export interface RoleDTO {
  id: string;
  tenantId?: string;
  roleCode: string;
  roleName: string;
  roleDesc?: string;
  roleType?: string;
  roleCategory?: string;
  roleLevel: number;
  dataScope: number;
  maxApprovalAmount?: number;
  businessScope?: string;
  customDeptIds?: string[];
  status?: number;
  sortOrder?: number;
  createTime?: string;
  updateTime?: string;
  permissionIds?: string[];
  deptIds?: string[];
  userCount?: number;
}

export interface RoleQuery extends PageQuery {
  roleName?: string;
}

export interface DeptDTO {
  id: string;
  parentId?: string;
  deptCode: string;
  deptName: string;
  deptType?: number;
  leaderId?: string;
  leaderName?: string;
  phone?: string;
  email?: string;
  isolationLevel?: number;
  sortOrder?: number;
  status?: number;
  children?: DeptDTO[];
  userCount?: number;
  childCount?: number;
}

export interface PermissionDTO {
  id: string;
  parentId?: string;
  permissionCode: string;
  permissionName: string;
  permissionType?: number;
  routePath?: string;
  component?: string;
  redirect?: string;
  icon?: string;
  apiPath?: string;
  httpMethod?: string;
  permissionScope?: string;
  permissionLevel?: number;
  riskLevel?: number;
  needApproval?: boolean;
  needTwoFactor?: boolean;
  sortOrder?: number;
  visible?: boolean;
  status?: number;
  createTime?: string;
  updateTime?: string;
  children?: PermissionDTO[];
}

export interface DictTypeDTO {
  id: string;
  tenantId?: string;
  dictCode: string;
  dictName: string;
  dictNameEn?: string;
  description?: string;
  status?: number;
  isSystem?: boolean;
  sortOrder?: number;
  createTime?: string;
  updateTime?: string;
  remark?: string;
}

export interface DictItemDTO {
  id: string;
  tenantId?: string;
  dictTypeId: string;
  dictCode: string;
  itemCode: string;
  itemName: string;
  itemNameEn?: string;
  itemValue?: string;
  cssClass?: string;
  labelClass?: string;
  icon?: string;
  color?: string;
  sortOrder?: number;
  isDefault?: boolean;
  status?: number;
  createTime?: string;
  updateTime?: string;
  remark?: string;
}

export interface DictTypeQuery {
  dictCode?: string;
  status?: number;
}

export interface TemporaryRoleGrantDTO {
  roleIds: string[];
  effectiveTime: string;
  expireTime: string;
  reason: string;
}

export interface ApiPermissionDTO {
  apiPath: string;
  httpMethod: string;
  permissionCode: string;
}

/** Row of `sys_status_dict` — the display metadata for one business status. */
export interface StatusDictDTO {
  id: string;
  tenantId?: string;
  bizType: string;
  statusCode: string;
  statusName: string;
  statusNameEn?: string;
  color?: string;
  icon?: string;
  sortOrder?: number;
  isInitial?: boolean;
  isTerminal?: boolean;
  isCancellable?: boolean;
  enabled?: boolean;
  createTime?: string;
  updateTime?: string;
  remark?: string;
}

/** Row of `sys_status_transition` — one allowed edge of the status machine. */
export interface StatusTransitionDTO {
  id: string;
  tenantId?: string;
  bizType: string;
  fromStatus: string;
  toStatus: string;
  actionCode: string;
  actionName?: string;
  actionNameEn?: string;
  needApproval?: boolean;
  enabled?: boolean;
  conditionExpression?: string;
  preAction?: string;
  postAction?: string;
  sortOrder?: number;
  createTime?: string;
  updateTime?: string;
  remark?: string;
}

/** An action the current user may take from `currentStatus`. */
export interface AvailableActionDTO {
  actionCode: string;
  actionName?: string;
  actionNameEn?: string;
  targetStatus?: string;
  targetStatusName?: string;
  needApproval?: boolean;
  conditionExpression?: string;
}

export interface TransitionCheckDTO {
  allowed: boolean;
  bizType?: string;
  fromStatus?: string;
  toStatus?: string;
  reason?: string;
}

export interface TransitionResultDTO {
  success: boolean;
  bizType?: string;
  fromStatus?: string;
  toStatus?: string;
  actionCode?: string;
  needApproval?: boolean;
  postAction?: string;
  errorMessage?: string;
}

/** Decision-matrix rule. `expression` is evaluated by the engine, not parsed here. */
export interface DecisionRuleDTO {
  id: string;
  engineType: string;
  scene?: string;
  ruleType?: string;
  expression?: string;
  description?: string;
  enabled?: boolean;
  priority?: number;
  version?: number;
}

export interface WeightConditionDTO {
  field: string;
  operator: string;
  value?: unknown;
}

/** `weights` is a free-form `{dimension: number}` map whose keys are engine-specific. */
export interface WeightProfileDTO {
  id: string;
  engineType: string;
  scene?: string;
  version?: number;
  weights?: Record<string, number>;
  conditions?: WeightConditionDTO[];
  active?: boolean;
}

export interface ConflictDTO {
  type?: string;
  ruleId1?: string;
  ruleId2?: string;
  description?: string;
  severity?: string;
}
