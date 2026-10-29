'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Table, Button, Tag, Space, message, Popconfirm, Input } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons'
import client from '@/lib/api/client'

interface User {
  id: string
  username: string
  email: string
  phone: string
  department: string
  status: number
  roles: string[]
  createdAt: string
}

export default function UserListPage() {
  const router = useRouter()
  const [loading, setLoading] = useState(false)
  const [users, setUsers] = useState<User[]>([])
  const [searchText] = useState('')

  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '部门',
      dataIndex: 'department',
      key: 'department',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '角色',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles: string[]) => (
        <Space>
          {roles?.map((role) => (
            <Tag key={role} color="processing">{role}</Tag>
          ))}
        </Space>
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
      render: (_: any, record: User) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => router.push(`/users/edit/${record.id}`)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除此用户？"
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

  const handleDelete = async (id: string) => {
    try {
      await client.delete(`/users/${id}`)
      message.success('删除成功')
      loadUsers()
    } catch (error: any) {
      message.error(error.response?.data?.message || '删除失败')
    }
  }

  const loadUsers = async () => {
    setLoading(true)
    try {
      const response = await client.get('/users', { params: { search: searchText } })
      setUsers(response.data.data.records || [])
    } catch (error) {
      console.error('Failed to load users:', error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <Input.Search
          placeholder="搜索用户"
          allowClear
          enterButton={<SearchOutlined />}
          size="large"
          onSearch={loadUsers}
          style={{ width: 400 }}
        />
        <Button
          type="primary"
          icon={<PlusOutlined />}
          size="large"
          onClick={() => router.push('/users/add')}
        >
          新增用户
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={users}
        loading={loading}
        rowKey="id"
      />
    </>
  )
}
