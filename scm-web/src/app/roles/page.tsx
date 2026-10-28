'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Table, Button, Tag, Space, message, Popconfirm, Modal, Form, Input } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import client from '@/lib/api/client'

interface Role {
  id: string
  name: string
  code: string
  description: string
  permissions: string[]
  createdAt: string
}

export default function RoleListPage() {
  const router = useRouter()
  const [loading, setLoading] = useState(false)
  const [roles, setRoles] = useState<Role[]>([])
  const [modalVisible, setModalVisible] = useState(false)
  const [editingRole, setEditingRole] = useState<Role | null>(null)
  const [form] = Form.useForm()

  const loadRoles = async () => {
    setLoading(true)
    try {
      const response = await client.get('/roles')
      setRoles(response.data.data.records || [])
    } catch (error) {
      console.error('Failed to load roles:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadRoles()
  }, [])

  const columns = [
    {
      title: '角色名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '角色编码',
      dataIndex: 'code',
      key: 'code',
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '权限数',
      dataIndex: 'permissions',
      key: 'permissions',
      render: (permissions: string[]) => (
        <Tag color="processing">{permissions?.length || 0} 个权限</Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: Role) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除此角色？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const handleEdit = (role: Role) => {
    setEditingRole(role)
    form.setFieldsValue(role)
    setModalVisible(true)
  }

  const handleDelete = async (id: string) => {
    try {
      await client.delete(`/roles/${id}`)
      message.success('删除成功')
      loadRoles()
    } catch (error: any) {
      message.error(error.response?.data?.message || '删除失败')
    }
  }

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields()
      if (editingRole) {
        await client.put(`/roles/${editingRole.id}`, values)
        message.success('更新成功')
      } else {
        await client.post('/roles', values)
        message.success('创建成功')
      }
      setModalVisible(false)
      form.resetFields()
      setEditingRole(null)
      loadRoles()
    } catch (error: any) {
      if (error.response) {
        message.error(error.response?.data?.message || '操作失败')
      }
    }
  }

  return (
    <>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          size="large"
          onClick={() => {
            setEditingRole(null)
            form.resetFields()
            setModalVisible(true)
          }}
        >
          新增角色
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={roles}
        loading={loading}
        rowKey="id"
      />
      <Modal
        title={editingRole ? '编辑角色' : '新增角色'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => {
          setModalVisible(false)
          form.resetFields()
          setEditingRole(null)
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="角色名称"
            rules={[{ required: true, message: '请输入角色名称' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="code"
            label="角色编码"
            rules={[{ required: true, message: '请输入角色编码' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="description"
            label="描述"
          >
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
