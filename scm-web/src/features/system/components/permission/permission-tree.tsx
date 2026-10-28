'use client'

import { useState } from 'react'
import { Tree, Button, Tag, Space, Modal, Form, Input, Select } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { Card, CardContent } from '@/components/ui/card'
import { PageHeader } from '@/components/ui/page-header'
import { usePermissionList, useCreatePermission } from '../../hooks'
import type { Permission } from '../../types'

function buildTreeData(permissions: Permission[]): any[] {
  return permissions.map(p => ({
    key: p.id,
    title: (
      <Space>
        <span>{p.name}</span>
        <Tag color={p.type === 'menu' ? 'blue' : p.type === 'button' ? 'green' : 'orange'}>{p.type}</Tag>
        <span style={{ color: 'var(--color-text-tertiary)', fontSize: 12 }}>{p.code}</span>
      </Space>
    ),
    children: p.children ? buildTreeData(p.children) : undefined,
  }))
}

export default function PermissionTree() {
  const { data: permissions, isLoading } = usePermissionList()
  const createPermission = useCreatePermission()
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm()

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      await createPermission.mutateAsync(values)
      setModalOpen(false)
      form.resetFields()
    } catch {}
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="系统管理"
        title="权限管理"
        description="维护菜单、按钮与数据权限点"
        actions={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setModalOpen(true)}
          >
            新建权限
          </Button>
        }
      />
      <Card>
        <CardContent>
          {isLoading ? (
            <div className="text-muted-foreground py-12 text-center text-sm">
              加载中…
            </div>
          ) : (
            <Tree
              treeData={buildTreeData(permissions || [])}
              defaultExpandAll
              showLine
            />
          )}
        </CardContent>
      </Card>
      <Modal title="新建权限" open={modalOpen} onOk={handleCreate} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="权限名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="code" label="权限编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="type" label="类型" rules={[{ required: true }]}><Select options={[{ label: '菜单', value: 'menu' }, { label: '按钮', value: 'button' }, { label: '数据', value: 'data' }]} /></Form.Item>
          <Form.Item name="path" label="路由路径"><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
