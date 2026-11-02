'use client'

import { useEffect, useMemo } from 'react'
import { Form, Input, Select, Modal, TreeSelect } from 'antd'
import { useCreateUser, useUpdateUser, useUserDetail, useDeptList, useRoleList } from '../../hooks'
import type { Department, User } from '../../types'

interface UserFormProps {
  open: boolean
  onClose: () => void
  user?: User | null
}

interface DeptTreeOption {
  title: string
  value: string
  children?: DeptTreeOption[]
}

function toDeptTreeData(depts: Department[]): DeptTreeOption[] {
  return depts.map((dept) => ({
    title: dept.name,
    value: dept.id,
    children: dept.children?.length ? toDeptTreeData(dept.children) : undefined,
  }))
}

export default function UserForm({ open, onClose, user }: UserFormProps) {
  const [form] = Form.useForm()
  const createUser = useCreateUser()
  const updateUser = useUpdateUser()
  const { data: depts } = useDeptList()
  const { data: roles } = useRoleList()
  const { data: detail } = useUserDetail(open && user ? user.id : '')

  const deptTreeData = useMemo(() => toDeptTreeData(depts ?? []), [depts])

  useEffect(() => {
    if (!open) return
    if (user) {
      form.setFieldsValue({
        username: user.username,
        displayName: user.displayName,
        email: user.email || undefined,
        phone: user.phone || undefined,
        status: user.status,
        deptId: detail?.deptId ?? user.deptId,
        roleIds: detail?.roleIds ?? user.roleIds ?? [],
      })
    } else {
      form.resetFields()
      form.setFieldsValue({ status: 'active', roleIds: [] })
    }
  }, [open, user, detail, form])

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      if (user) {
        await updateUser.mutateAsync({ id: user.id, data: values })
      } else {
        await createUser.mutateAsync(values)
      }
      onClose()
    } catch {}
  }

  return (
    <Modal
      title={user ? '编辑用户' : '新建用户'}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      width={520}
      confirmLoading={createUser.isPending || updateUser.isPending}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="username"
          label="用户名"
          rules={[
            { required: true },
            { min: 4, max: 32 },
            { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字和下划线' },
          ]}
        >
          <Input disabled={!!user} />
        </Form.Item>
        {!user && (
          <Form.Item name="password" label="初始密码" rules={[{ required: true }, { min: 6, max: 20 }]}>
            <Input.Password />
          </Form.Item>
        )}
        <Form.Item name="displayName" label="姓名" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="deptId" label="所属部门">
          <TreeSelect
            treeData={deptTreeData}
            allowClear
            placeholder="选择部门"
            treeDefaultExpandAll
            showSearch
            treeNodeFilterProp="title"
          />
        </Form.Item>
        <Form.Item name="roleIds" label="角色">
          <Select
            mode="multiple"
            allowClear
            placeholder="选择角色"
            options={(roles ?? []).map((role) => ({ label: `${role.name} (${role.code})`, value: role.id }))}
          />
        </Form.Item>
        <Form.Item name="email" label="邮箱" rules={[{ type: 'email' }]}>
          <Input />
        </Form.Item>
        <Form.Item name="phone" label="手机号" rules={[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}>
          <Input />
        </Form.Item>
        <Form.Item name="status" label="状态" rules={[{ required: true }]}>
          <Select
            options={[
              { label: '启用', value: 'active' },
              { label: '禁用', value: 'disabled' },
              { label: '锁定', value: 'locked' },
            ]}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}
