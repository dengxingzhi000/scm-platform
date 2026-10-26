import type {
  DeptDTO,
  DictItemDTO,
  DictTypeDTO,
  PageResult as ApiPageResult,
  PermissionDTO,
  RoleDTO,
  UserDTO,
} from "@/lib/api/contracts/system";
import type {
  Department,
  DictItem,
  DictType,
  PageResult,
  Permission,
  Role,
  User,
} from "../types";

type UiStatus = "active" | "disabled";

export type UserFormInput = Partial<User> & { password?: string };

const toUiStatus = (status?: number): UiStatus =>
  status === 1 ? "active" : "disabled";

const toUiUserStatus = (status?: number): User["status"] =>
  status === 2 ? "locked" : toUiStatus(status);

const toApiStatus = (status?: string, fallback = 1): number => {
  if (status === "active") return 1;
  if (status === "disabled") return 0;
  if (status === "locked") return 2;
  return fallback;
};

export const normalizeRoleCode = (code: string): string => {
  const upper = code.trim().toUpperCase();
  const withoutPrefix = upper.startsWith("ROLE_") ? upper.slice(5) : upper;
  const normalized = withoutPrefix
    .replace(/[^A-Z]+/g, "_")
    .replace(/^_+|_+$/g, "");
  return `ROLE_${normalized || "CUSTOM"}`;
};

export const toUser = (dto: UserDTO): User => ({
  id: dto.id,
  username: dto.username,
  displayName: dto.realName || dto.username,
  email: dto.email ?? "",
  phone: dto.phone,
  avatar: dto.avatar,
  status: toUiUserStatus(dto.status),
  roles: dto.roleNames ?? [],
  roleIds: dto.roleIds,
  deptId: dto.deptId,
  deptName: dto.deptName,
  createdAt: dto.createTime ?? "",
  updatedAt: dto.updateTime ?? dto.createTime ?? "",
});

export const toUserPayload = (
  values: UserFormInput,
  existing?: User
): UserDTO => ({
  id: values.id ?? existing?.id ?? "",
  username: values.username ?? existing?.username ?? "",
  password: values.password,
  realName:
    values.displayName ?? existing?.displayName ?? values.username ?? "",
  email: values.email ?? existing?.email,
  phone: values.phone ?? existing?.phone,
  avatar: values.avatar ?? existing?.avatar,
  status: toApiStatus(values.status ?? existing?.status),
  deptId: values.deptId ?? existing?.deptId,
  deptName: values.deptName ?? existing?.deptName,
  roleIds: values.roleIds ?? existing?.roleIds,
});

export const toRole = (dto: RoleDTO): Role => ({
  id: dto.id,
  name: dto.roleName,
  code: dto.roleCode,
  description: dto.roleDesc,
  status: toUiStatus(dto.status),
  permissionIds: dto.permissionIds ?? [],
  roleLevel: dto.roleLevel ?? 1,
  dataScope: dto.dataScope ?? 5,
  roleType: dto.roleType,
  roleCategory: dto.roleCategory,
  sortOrder: dto.sortOrder,
  userCount: dto.userCount ?? 0,
  createdAt: dto.createTime ?? "",
});

export const toRolePayload = (
  values: Partial<Role>,
  existing?: Role
): RoleDTO => ({
  id: values.id ?? existing?.id ?? "",
  roleCode: normalizeRoleCode(values.code ?? existing?.code ?? ""),
  roleName: values.name ?? existing?.name ?? "",
  roleDesc: values.description ?? existing?.description,
  roleType: values.roleType ?? existing?.roleType ?? "CUSTOM",
  roleCategory: values.roleCategory ?? existing?.roleCategory ?? "BUSINESS",
  roleLevel: values.roleLevel ?? existing?.roleLevel ?? 1,
  dataScope: values.dataScope ?? existing?.dataScope ?? 5,
  status: toApiStatus(values.status ?? existing?.status),
  sortOrder: values.sortOrder ?? existing?.sortOrder ?? 0,
  permissionIds: values.permissionIds ?? existing?.permissionIds ?? [],
});

export const toDepartment = (dto: DeptDTO): Department => ({
  id: dto.id,
  name: dto.deptName,
  code: dto.deptCode,
  parentId: dto.parentId ?? null,
  sort: dto.sortOrder ?? 0,
  status: toUiStatus(dto.status),
  leaderId: dto.leaderId,
  leaderName: dto.leaderName,
  phone: dto.phone,
  email: dto.email,
  userCount: dto.userCount,
  children: dto.children?.map(toDepartment),
});

export const toDeptPayload = (
  values: Partial<Department>,
  existing?: Department
): DeptDTO => ({
  id: values.id ?? existing?.id ?? "",
  parentId: values.parentId ?? existing?.parentId ?? undefined,
  deptCode: values.code ?? existing?.code ?? "",
  deptName: values.name ?? existing?.name ?? "",
  leaderId: values.leaderId ?? existing?.leaderId,
  phone: values.phone ?? existing?.phone,
  email: values.email ?? existing?.email,
  sortOrder: values.sort ?? existing?.sort ?? 0,
  status: toApiStatus(values.status ?? existing?.status),
});

const toUiPermissionType = (type?: number): Permission["type"] => {
  if (type === 3) return "button";
  if (type === 4 || type === 5) return "data";
  return "menu";
};

const toApiPermissionType = (type: Permission["type"]): number => {
  if (type === "button") return 3;
  if (type === "data") return 5;
  return 2;
};

export const toPermission = (dto: PermissionDTO): Permission => ({
  id: dto.id,
  name: dto.permissionName,
  code: dto.permissionCode,
  type: toUiPermissionType(dto.permissionType),
  parentId: dto.parentId ?? null,
  icon: dto.icon,
  path: dto.routePath,
  apiPath: dto.apiPath,
  httpMethod: dto.httpMethod,
  sort: dto.sortOrder ?? 0,
  status: toUiStatus(dto.status),
  visible: dto.visible,
  children: dto.children?.map(toPermission),
});

export const toPermissionPayload = (
  values: Partial<Permission>,
  existing?: Permission
): PermissionDTO => ({
  id: values.id ?? existing?.id ?? "",
  parentId: values.parentId ?? existing?.parentId ?? undefined,
  permissionCode: values.code ?? existing?.code ?? "",
  permissionName: values.name ?? existing?.name ?? "",
  permissionType: toApiPermissionType(values.type ?? existing?.type ?? "menu"),
  routePath: values.path ?? existing?.path,
  icon: values.icon ?? existing?.icon,
  apiPath: values.apiPath ?? existing?.apiPath,
  httpMethod: values.httpMethod ?? existing?.httpMethod,
  sortOrder: values.sort ?? existing?.sort ?? 0,
  status: toApiStatus(values.status ?? existing?.status),
  visible: values.visible ?? existing?.visible ?? true,
});

export const toDictType = (dto: DictTypeDTO): DictType => ({
  id: dto.id,
  name: dto.dictName,
  code: dto.dictCode,
  description: dto.description,
  status: toUiStatus(dto.status),
  itemCount: 0,
  sortOrder: dto.sortOrder,
  isSystem: dto.isSystem,
});

export const toDictTypePayload = (
  values: Partial<DictType>,
  existing?: DictType
): DictTypeDTO => ({
  id: values.id ?? existing?.id ?? "",
  dictCode: values.code ?? existing?.code ?? "",
  dictName: values.name ?? existing?.name ?? "",
  description: values.description ?? existing?.description,
  status: toApiStatus(values.status ?? existing?.status),
  sortOrder: values.sortOrder ?? existing?.sortOrder ?? 0,
  isSystem: values.isSystem ?? existing?.isSystem ?? false,
});

export const toDictItem = (dto: DictItemDTO): DictItem => ({
  id: dto.id,
  dictTypeId: dto.dictTypeId,
  dictCode: dto.dictCode,
  label: dto.itemName,
  value: dto.itemValue ?? dto.itemCode,
  sort: dto.sortOrder ?? 0,
  status: toUiStatus(dto.status),
  isDefault: dto.isDefault,
  remark: dto.remark,
});

export const toDictItemPayload = (
  values: Partial<DictItem>,
  dictTypeId: string,
  dictCode: string
): DictItemDTO => ({
  id: values.id ?? "",
  dictTypeId,
  dictCode,
  itemCode: values.value ?? "",
  itemName: values.label ?? "",
  itemValue: values.value,
  sortOrder: values.sort ?? 0,
  status: toApiStatus(values.status),
  isDefault: values.isDefault ?? false,
  remark: values.remark,
});

export const toPageResult = <TDto, TUi>(
  page: ApiPageResult<TDto>,
  map: (dto: TDto) => TUi
): PageResult<TUi> => ({
  records: page.records.map(map),
  total: page.total,
  current: page.pageNum,
  pageSize: page.pageSize,
  pages: page.pages,
});
