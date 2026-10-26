'use client'

import { useEffect } from 'react'
import { Form, Input, Select, Modal } from 'antd'
import { useCreateUser, useUpdateUser } from '../../hooks'
import type { User } from '../../types'

interface UserFormProps {
  open: boolean
  onClose: () => void
  user?: User | null
}

export default function UserForm({ open, onClose, user }: UserFormProps) {
  const [form] = Form.useForm()
  const createUser = useCreateUser()
  const updateUser = useUpdateUser()

  useEffect(() => {
    if (open) {
      if (user) { form.setFieldsValue(user) } else { form.resetFields() }
    }
  }, [open, user, form])

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      if (user) { await updateUser.mutateAsync({ id: user.id, data: values }) }
      else { await createUser.mutateAsync(values) }
      onClose()
    } catch {}
  }

  return (
    <Modal title={user ? '编辑用户' : '新建用户'} open={open} onOk={handleOk} onCancel={onClose} confirmLoading={createUser.isPending || updateUser.isPending}>
      <Form form={form} layout="vertical">
        <Form.Item name="username" label="用户名" rules={[{ required: true }, { min: 4, max: 32 }, { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字和下划线' }]}><Input /></Form.Item>
        {!user && (
          <Form.Item name="password" label="初始密码" rules={[{ required: true }, { min: 6, max: 20 }]}><Input.Password /></Form.Item>
        )}
        <Form.Item name="displayName" label="姓名" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="email" label="邮箱" rules={[{ type: 'email' }]}><Input /></Form.Item>
        <Form.Item name="phone" label="手机号" rules={[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]}><Input /></Form.Item>
        <Form.Item name="status" label="状态" rules={[{ required: true }]}><Select options={[{ label: '启用', value: 'active' }, { label: '禁用', value: 'disabled' }, { label: '锁定', value: 'locked' }]} /></Form.Item>
      </Form>
    </Modal>
  )
}
