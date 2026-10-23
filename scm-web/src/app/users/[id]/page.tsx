'use client'

import { useState, useEffect } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { Form, Input, Button, Card, message, Select } from 'antd'
import client from '@/lib/api/client'

export default function UserFormPage() {
  const router = useRouter()
  const params = useParams()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const isEdit = params.id !== 'add'

  const loadUser = async () => {
    try {
      const response = await client.get(`/users/${params.id}`)
      form.setFieldsValue(response.data.data)
    } catch (error) {
      message.error('加载用户失败')
    }
  }

  useEffect(() => {
    if (isEdit) {
      loadUser()
    }
  }, [params.id])

  const onFinish = async (values: any) => {
    setLoading(true)
    try {
      if (isEdit) {
        await client.put(`/users/${params.id}`, values)
        message.success('更新成功')
      } else {
        await client.post('/users', values)
        message.success('创建成功')
      }
      router.push('/users')
    } catch (error: any) {
      message.error(error.response?.data?.message || '操作失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card title={isEdit ? '编辑用户' : '新增用户'}>
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
      >
        <Form.Item
          name="username"
          label="用户名"
          rules={[{ required: true, message: '请输入用户名' }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="email"
          label="邮箱"
          rules={[
            { required: true, message: '请输入邮箱' },
            { type: 'email', message: '请输入正确的邮箱' },
          ]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="phone"
          label="手机号"
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="department"
          label="部门"
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="roleIds"
          label="角色"
        >
          <Select
            mode="multiple"
            placeholder="请选择角色"
            options={[
              { label: '管理员', value: 'admin' },
              { label: '操作员', value: 'operator' },
              { label: '查看者', value: 'viewer' },
            ]}
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loading}>
            {isEdit ? '更新' : '创建'}
          </Button>
          <Button style={{ marginLeft: 8 }} onClick={() => router.push('/users')}>
            取消
          </Button>
        </Form.Item>
      </Form>
    </Card>
  )
}
