export interface User {
  id: string
  username: string
  displayName: string
  email: string
  phone?: string
  avatar?: string
  status: 'active' | 'disabled' | 'locked'
  roles: string[]
  roleIds?: string[]
  deptId?: string
  deptName?: string
  createdAt: string
  updatedAt: string
}

export interface Role {
  id: string
  name: string
  code: string
  description?: string
  status: 'active' | 'disabled'
  permissionIds: string[]
  roleLevel: number
  dataScope: number
  roleType?: string
  roleCategory?: string
  sortOrder?: number
  userCount: number
  createdAt: string
}

export interface Permission {
  id: string
  name: string
  code: string
  type: 'menu' | 'button' | 'data'
  parentId: string | null
  icon?: string
  path?: string
  apiPath?: string
  httpMethod?: string
  sort: number
  status: 'active' | 'disabled'
  visible?: boolean
  children?: Permission[]
}

export interface Department {
  id: string
  name: string
  code: string
  parentId: string | null
  sort: number
  status: 'active' | 'disabled'
  leaderId?: string
  leaderName?: string
  phone?: string
  email?: string
  userCount?: number
  children?: Department[]
}

export interface DictType {
  id: string
  name: string
  code: string
  description?: string
  status: 'active' | 'disabled'
  itemCount: number
  sortOrder?: number
  isSystem?: boolean
}

export interface DictItem {
  id: string
  dictTypeId: string
  dictCode?: string
  label: string
  value: string
  sort: number
  status: 'active' | 'disabled'
  isDefault?: boolean
  remark?: string
}

export interface PageParams {
  current?: number
  pageSize?: number
  keyword?: string
  status?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  pageSize: number
  pages: number
}
