'use client'

import { useMemo, useState } from 'react'
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Tag, Tree, TreeSelect, message } from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { Card, CardContent } from '@/components/ui/card'
import { PageHeader } from '@/components/ui/page-header'
import { useCreatePermission, useDeletePermission, usePermissionList, useUpdatePermission } from '../../hooks'
import type { Permission } from '../../types'

interface PermissionTreeOption {
  title: string
  value: string
  children?: PermissionTreeOption[]
}

function toParentOptions(permissions: Permission[], excludeId?: string): PermissionTreeOption[] {
  return permissions
    .filter((permission) => permission.id !== excludeId)
    .map((permission) => ({
      title: `${permission.name} (${permission.code})`,
      value: permission.id,
      children: permission.children?.length ? toParentOptions(permission.children, excludeId) : undefined,
    }))
}

const typeMeta: Record<Permission['type'], { label: string; color: string }> = {
  directory: { label: '目录', color: 'purple' },
  menu: { label: '菜单', color: 'blue' },
  button: { label: '按钮', color: 'green' },
  data: { label: '数据', color: 'orange' },
}

function buildTreeData(
  permissions: Permission[],
  onAdd: (permission: Permission) => void,
  onEdit: (permission: Permission) => void,
  onDelete: (permission: Permission) => void,
): any[] {
  return permissions.map((permission) => ({
    key: permission.id,
    title: (
      <Space size={8}>
        <span>{permission.name}</span>
        <Tag color={typeMeta[permission.type].color}>{typeMeta[permission.type].label}</Tag>
        <span style={{ color: 'var(--color-text-tertiary)', fontSize: 12 }}>{permission.code}</span>
        {permission.apiPath && (
          <span style={{ color: 'var(--color-text-tertiary)', fontSize: 12 }}>
            {permission.httpMethod || '*'} {permission.apiPath}
          </span>
        )}
        <Button
          type="link"
          size="small"
          icon={<PlusOutlined />}
          onClick={(event) => {
            event.stopPropagation()
            onAdd(permission)
          }}
        >
          子权限
        </Button>
        <Button
          type="link"
          size="small"
          icon={<EditOutlined />}
          onClick={(event) => {
            event.stopPropagation()
            onEdit(permission)
          }}
        >
          编辑
        </Button>
        <Popconfirm
          title="确定删除该权限？"
          description="存在子权限或已被角色引用时不可删除"
          onConfirm={() => onDelete(permission)}
        >
          <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={(event) => event.stopPropagation()}>
            删除
          </Button>
        </Popconfirm>
      </Space>
    ),
    children: permission.children?.length
      ? buildTreeData(permission.children, onAdd, onEdit, onDelete)
      : undefined,
  }))
}

export default function PermissionTree() {
  const { data: permissions, isLoading } = usePermissionList()
  const createPermission = useCreatePermission()
  const updatePermission = useUpdatePermission()
  const deletePermission = useDeletePermission()

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Permission | null>(null)
  const [form] = Form.useForm()

  const parentOptions = useMemo(() => toParentOptions(permissions ?? [], editing?.id), [permissions, editing])

  const openCreate = (parent?: Permission) => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ parentId: parent?.id, type: 'menu', status: 'active', visible: true, sort: 0 })
    setModalOpen(true)
  }

  const openEdit = (permission: Permission) => {
    setEditing(permission)
    form.setFieldsValue({
      parentId: permission.parentId ?? undefined,
      name: permission.name,
      code: permission.code,
      type: permission.type,
      path: permission.path,
      icon: permission.icon,
      apiPath: permission.apiPath,
      httpMethod: permission.httpMethod,
      sort: permission.sort,
      visible: permission.visible ?? true,
      status: permission.status,
    })
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editing) {
        await updatePermission.mutateAsync({ id: editing.id, data: values })
      } else {
        await createPermission.mutateAsync(values)
      }
      setModalOpen(false)
      form.resetFields()
    } catch {}
  }

  const handleDelete = async (permission: Permission) => {
    try {
      await deletePermission.mutateAsync(permission.id)
    } catch (error: any) {
      message.error(error?.response?.data?.message || '删除失败')
    }
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="系统管理"
        title="权限管理"
        description="维护目录、菜单、按钮与 API 权限点，作为角色授权与接口鉴权的依据"
        actions={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate()}>
            新建权限
          </Button>
        }
      />
      <Card>
        <CardContent>
          {isLoading ? (
            <div className="text-muted-foreground py-12 text-center text-sm">加载中…</div>
          ) : (permissions ?? []).length === 0 ? (
            <div className="text-muted-foreground py-12 text-center text-sm">暂无权限</div>
          ) : (
            <Tree
              treeData={buildTreeData(permissions ?? [], openCreate, openEdit, handleDelete)}
              defaultExpandAll
              showLine
            />
          )}
        </CardContent>
      </Card>

      <Modal
        title={editing ? '编辑权限' : '新建权限'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={createPermission.isPending || updatePermission.isPending}
        width={560}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="parentId" label="上级权限">
            <TreeSelect
              treeData={parentOptions}
              allowClear
              placeholder="不选则为顶级目录"
              treeDefaultExpandAll
              showSearch
              treeNodeFilterProp="title"
            />
          </Form.Item>
          <Form.Item name="name" label="权限名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="code"
            label="权限编码"
            tooltip="菜单用 menu:xxx，按钮/接口用 system:user:list 这类编码，需全局唯一"
            rules={[
              { required: true },
              { pattern: /^[a-zA-Z0-9:_-]+$/, message: '仅支持字母、数字、冒号、下划线和中划线' },
            ]}
          >
            <Input disabled={!!editing} placeholder="例如 system:user:list" />
          </Form.Item>
          <Form.Item name="type" label="权限类型" rules={[{ required: true }]}>
            <Select
              options={[
                { label: '目录', value: 'directory' },
                { label: '菜单', value: 'menu' },
                { label: '按钮 / 操作', value: 'button' },
                { label: '数据权限', value: 'data' },
              ]}
            />
          </Form.Item>
          <Form.Item name="path" label="路由路径">
            <Input placeholder="例如 /system/user" />
          </Form.Item>
          <Form.Item name="icon" label="图标">
            <Input placeholder="例如 user / setting" />
          </Form.Item>
          <Form.Item name="apiPath" label="API 路径">
            <Input placeholder="例如 /api/system/users" />
          </Form.Item>
          <Form.Item name="httpMethod" label="HTTP 方法">
            <Select
              allowClear
              options={['GET', 'POST', 'PUT', 'DELETE', 'PATCH', '*'].map((method) => ({
                label: method,
                value: method,
              }))}
            />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber min={0} max={9999} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="visible" label="菜单可见" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <Select
              options={[
                { label: '启用', value: 'active' },
                { label: '禁用', value: 'disabled' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
