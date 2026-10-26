'use client'

import { useState } from 'react'
import { Button, Tag, Space, Popconfirm, Table, Card } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { useRoleList, useDeleteRole } from '../../hooks'
import RoleForm from './role-form'
import type { Role } from '../../types'

export default function RoleList() {
  const [formOpen, setFormOpen] = useState(false)
  const [editingRole, setEditingRole] = useState<Role | null>(null)
  const { data: roles, isLoading } = useRoleList()
  const deleteRole = useDeleteRole()

  const columns = [
    { title: '角色名称', dataIndex: 'name', key: 'name' },
    { title: '角色编码', dataIndex: 'code', key: 'code' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (status: string) => <Tag color={status === 'active' ? 'success' : 'error'}>{status === 'active' ? '启用' : '禁用'}</Tag> },
    { title: '用户数', dataIndex: 'userCount', key: 'userCount' },
    {
      title: '操作', key: 'action', render: (_: unknown, record: Role) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => { setEditingRole(record); setFormOpen(true) }}>编辑</Button>
          <Popconfirm title="确定删除?" onConfirm={() => deleteRole.mutate(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>角色管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingRole(null); setFormOpen(true) }}>新建角色</Button>
      </div>
      <Card>
        <Table columns={columns} dataSource={roles || []} loading={isLoading} rowKey="id" pagination={false} />
      </Card>
      <RoleForm key={editingRole?.id ?? 'new'} open={formOpen} onClose={() => setFormOpen(false)} role={editingRole} />
    </div>
  )
}
