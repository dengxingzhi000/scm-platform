'use client'

import { useEffect, useRef } from 'react'
import { Form, Input, Select, Modal, Tree } from 'antd'
import { useCreateRole, useRoleDetail, useUpdateRole, usePermissionList } from '../../hooks'
import type { Role, Permission } from '../../types'

interface RoleFormProps {
  open: boolean
  onClose: () => void
  role?: Role | null
}

function buildTreeData(permissions: Permission[]): any[] {
  return permissions.map(p => ({
    key: p.id,
    title: p.name,
    children: p.children ? buildTreeData(p.children) : undefined,
  }))
}

export default function RoleForm({ open, onClose, role }: RoleFormProps) {
  const [form] = Form.useForm()
  const checkedKeysRef = useRef<string[]>([])
  const createRole = useCreateRole()
  const updateRole = useUpdateRole()
  const { data: permissions } = usePermissionList()
  const { data: roleDetail } = useRoleDetail(role?.id ?? '')
  const initialCheckedKeys = roleDetail?.permissionIds ?? role?.permissionIds ?? []

  useEffect(() => {
    if (!open) return
    if (role) {
      form.setFieldsValue(role)
      checkedKeysRef.current = roleDetail?.permissionIds ?? role.permissionIds ?? []
    } else {
      form.resetFields()
      checkedKeysRef.current = []
    }
  }, [open, role, roleDetail, form])

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      const payload = { ...values, permissionIds: checkedKeysRef.current }
      if (role) {
        await updateRole.mutateAsync({ id: role.id, data: payload })
      } else {
        await createRole.mutateAsync(payload)
      }
      onClose()
    } catch {}
  }

  return (
    <Modal title={role ? '编辑角色' : '新建角色'} open={open} onOk={handleOk} onCancel={onClose} width={600}>
      <Form form={form} layout="vertical">
        <Form.Item name="name" label="角色名称" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item
          name="code"
          label="角色编码"
          extra="后端要求格式：ROLE_ + 大写字母/下划线"
          rules={[
            { required: true },
            { pattern: /^ROLE_[A-Z_]+$/, message: '格式必须为 ROLE_XXX' },
          ]}
        >
          <Input placeholder="ROLE_WAREHOUSE_ADMIN" disabled={!!role} />
        </Form.Item>
        <Form.Item name="description" label="描述"><Input.TextArea /></Form.Item>
        <Form.Item name="status" label="状态" rules={[{ required: true }]}><Select options={[{ label: '启用', value: 'active' }, { label: '禁用', value: 'disabled' }]} /></Form.Item>
        <Form.Item label="权限">
          <Tree
            key={`${role?.id ?? 'new'}-${initialCheckedKeys.join(',')}`}
            checkable
            defaultCheckedKeys={initialCheckedKeys}
            onCheck={(keys) => {
              checkedKeysRef.current = (Array.isArray(keys) ? keys : keys.checked).map(String)
            }}
            treeData={buildTreeData(permissions || [])}
            defaultExpandAll
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}
