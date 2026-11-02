'use client'

import { useMemo, useState } from 'react'
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Tree, TreeSelect, message } from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { Card, CardContent } from '@/components/ui/card'
import { PageHeader } from '@/components/ui/page-header'
import { useCreateDept, useDeleteDept, useDeptList, useUpdateDept } from '../../hooks'
import type { Department } from '../../types'

interface DeptTreeOption {
  title: string
  value: string
  children?: DeptTreeOption[]
}

function toTreeSelectData(depts: Department[], excludeId?: string): DeptTreeOption[] {
  return depts
    .filter((dept) => dept.id !== excludeId)
    .map((dept) => ({
      title: dept.name,
      value: dept.id,
      children: dept.children?.length ? toTreeSelectData(dept.children, excludeId) : undefined,
    }))
}

function buildTreeData(depts: Department[], onAdd: (d: Department) => void, onEdit: (d: Department) => void, onDelete: (d: Department) => void): any[] {
  return depts.map((dept) => ({
    key: dept.id,
    title: (
      <Space size={8}>
        <span>{dept.name}</span>
        <span style={{ color: 'var(--color-text-tertiary)', fontSize: 12 }}>{dept.code}</span>
        {dept.leaderName && (
          <span style={{ color: 'var(--color-text-tertiary)', fontSize: 12 }}>负责人: {dept.leaderName}</span>
        )}
        <span style={{ color: 'var(--color-text-tertiary)', fontSize: 12 }}>
          {(dept.userCount ?? 0)} 人 / {(dept.children?.length ?? 0)} 子部门
        </span>
        <Button
          type="link"
          size="small"
          icon={<PlusOutlined />}
          onClick={(event) => {
            event.stopPropagation()
            onAdd(dept)
          }}
        >
          子部门
        </Button>
        <Button
          type="link"
          size="small"
          icon={<EditOutlined />}
          onClick={(event) => {
            event.stopPropagation()
            onEdit(dept)
          }}
        >
          编辑
        </Button>
        <Popconfirm
          title="确定删除该部门？"
          description="删除前需先移走子部门与用户"
          onConfirm={() => onDelete(dept)}
        >
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={(event) => event.stopPropagation()}
          >
            删除
          </Button>
        </Popconfirm>
      </Space>
    ),
    children: dept.children?.length
      ? buildTreeData(dept.children, onAdd, onEdit, onDelete)
      : undefined,
  }))
}

export default function DeptTree() {
  const { data: depts, isLoading } = useDeptList()
  const createDept = useCreateDept()
  const updateDept = useUpdateDept()
  const deleteDept = useDeleteDept()

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Department | null>(null)
  const [form] = Form.useForm()

  const parentOptions = useMemo(() => toTreeSelectData(depts ?? [], editing?.id), [depts, editing])

  const openCreate = (parent?: Department) => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ parentId: parent?.id, status: 'active', sort: 0 })
    setModalOpen(true)
  }

  const openEdit = (dept: Department) => {
    setEditing(dept)
    form.setFieldsValue({
      name: dept.name,
      code: dept.code,
      parentId: dept.parentId ?? undefined,
      leaderName: dept.leaderName,
      phone: dept.phone,
      email: dept.email,
      sort: dept.sort,
      status: dept.status,
    })
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editing) {
        await updateDept.mutateAsync({ id: editing.id, data: values })
      } else {
        await createDept.mutateAsync(values)
      }
      setModalOpen(false)
      form.resetFields()
    } catch {}
  }

  const handleDelete = async (dept: Department) => {
    try {
      await deleteDept.mutateAsync(dept.id)
    } catch (error: any) {
      message.error(error?.response?.data?.message || '删除失败')
    }
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="系统管理"
        title="部门管理"
        description="维护组织架构、部门编码、负责人与数据权限归属"
        actions={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate()}>
            新建部门
          </Button>
        }
      />
      <Card>
        <CardContent>
          {isLoading ? (
            <div className="text-muted-foreground py-12 text-center text-sm">加载中…</div>
          ) : (depts ?? []).length === 0 ? (
            <div className="text-muted-foreground py-12 text-center text-sm">暂无部门</div>
          ) : (
            <Tree
              treeData={buildTreeData(depts ?? [], openCreate, openEdit, handleDelete)}
              defaultExpandAll
              showLine
            />
          )}
        </CardContent>
      </Card>

      <Modal
        title={editing ? '编辑部门' : '新建部门'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={createDept.isPending || updateDept.isPending}
        width={520}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="parentId" label="上级部门">
            <TreeSelect
              treeData={parentOptions}
              allowClear
              placeholder="不选则为顶级部门"
              treeDefaultExpandAll
              showSearch
              treeNodeFilterProp="title"
            />
          </Form.Item>
          <Form.Item name="name" label="部门名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="code"
            label="部门编码"
            rules={[{ required: true }, { pattern: /^[A-Z0-9_]+$/, message: '仅支持大写字母、数字和下划线' }]}
          >
            <Input disabled={!!editing} placeholder="例如 PURCHASE" />
          </Form.Item>
          <Form.Item name="leaderName" label="负责人">
            <Input />
          </Form.Item>
          <Form.Item name="phone" label="联系电话">
            <Input />
          </Form.Item>
          <Form.Item name="email" label="邮箱" rules={[{ type: 'email' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <InputNumber min={0} max={9999} style={{ width: '100%' }} />
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
