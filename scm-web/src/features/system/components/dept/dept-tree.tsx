'use client'

import { useState } from 'react'
import { Tree, Button, Modal, Form, Input, Space } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { Card, CardContent } from '@/components/ui/card'
import { PageHeader } from '@/components/ui/page-header'
import { useDeptList, useCreateDept } from '../../hooks'
import type { Department } from '../../types'

function buildTreeData(depts: Department[]): any[] {
  return depts.map(d => ({
    key: d.id,
    title: <Space><span>{d.name}</span>{d.leaderName && <span style={{ color: 'var(--color-text-tertiary)', fontSize: 12 }}>负责人: {d.leaderName}</span>}</Space>,
    children: d.children ? buildTreeData(d.children) : undefined,
  }))
}

export default function DeptTree() {
  const { data: depts, isLoading } = useDeptList()
  const createDept = useCreateDept()
  const [modalOpen, setModalOpen] = useState(false)
  const [form] = Form.useForm()

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      await createDept.mutateAsync(values)
      setModalOpen(false)
      form.resetFields()
    } catch {}
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="系统管理"
        title="部门管理"
        description="维护组织架构、部门编码与负责人"
        actions={
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setModalOpen(true)}
          >
            新建部门
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
              treeData={buildTreeData(depts || [])}
              defaultExpandAll
              showLine
            />
          )}
        </CardContent>
      </Card>
      <Modal title="新建部门" open={modalOpen} onOk={handleCreate} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="部门名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="code" label="部门编码" rules={[{ required: true }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
