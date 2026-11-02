'use client'

import { useState } from 'react'
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Table, Tag, message } from 'antd'
import { Pencil, Plus, Trash2 } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button as UiButton } from '@/components/ui/button'
import { PageHeader } from '@/components/ui/page-header'
import {
  TableEmpty,
  TableLoading,
  TablePanel,
  TableToolbar,
  TableToolbarEnd,
} from '@/components/ui/table-shell'
import {
  Table as UiTable,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  useCreateDictItem,
  useCreateDictType,
  useDeleteDictItem,
  useDeleteDictType,
  useDictItems,
  useDictTypeList,
  useUpdateDictItem,
  useUpdateDictType,
} from '../../hooks'
import type { DictItem, DictType } from '../../types'

export default function DictList() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editingType, setEditingType] = useState<DictType | null>(null)
  const [itemsDict, setItemsDict] = useState<DictType | null>(null)
  const [editingItem, setEditingItem] = useState<DictItem | null>(null)
  const [form] = Form.useForm()
  const [itemForm] = Form.useForm()

  const { data: dictTypes, isLoading } = useDictTypeList()
  const createDictType = useCreateDictType()
  const updateDictType = useUpdateDictType()
  const deleteDictType = useDeleteDictType()
  const { data: items, isLoading: itemsLoading } = useDictItems(itemsDict?.code ?? '')
  const createDictItem = useCreateDictItem()
  const updateDictItem = useUpdateDictItem()
  const deleteDictItem = useDeleteDictItem()

  const rows = dictTypes ?? []

  const openCreate = () => {
    setEditingType(null)
    form.resetFields()
    form.setFieldsValue({ status: 'active' })
    setModalOpen(true)
  }

  const openEdit = (dict: DictType) => {
    setEditingType(dict)
    form.setFieldsValue({
      name: dict.name,
      code: dict.code,
      description: dict.description,
      status: dict.status,
    })
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingType) {
        await updateDictType.mutateAsync({ id: editingType.id, data: values })
      } else {
        await createDictType.mutateAsync(values)
      }
      setModalOpen(false)
      form.resetFields()
    } catch {}
  }

  const openItems = (dict: DictType) => {
    setItemsDict(dict)
    setEditingItem(null)
    itemForm.resetFields()
    itemForm.setFieldsValue({ status: 'active', sort: 0 })
  }

  const openEditItem = (item: DictItem) => {
    setEditingItem(item)
    itemForm.setFieldsValue({
      label: item.label,
      value: item.value,
      sort: item.sort,
      status: item.status,
      remark: item.remark,
    })
  }

  const submitItem = async () => {
    if (!itemsDict) return
    try {
      const values = await itemForm.validateFields()
      if (editingItem) {
        await updateDictItem.mutateAsync({
          data: { ...values, id: editingItem.id },
          dictTypeId: itemsDict.id,
          dictCode: itemsDict.code,
        })
      } else {
        await createDictItem.mutateAsync({
          data: values,
          dictTypeId: itemsDict.id,
          dictCode: itemsDict.code,
        })
      }
      setEditingItem(null)
      itemForm.resetFields()
      itemForm.setFieldsValue({ status: 'active', sort: 0 })
    } catch {}
  }

  const removeItem = async (item: DictItem) => {
    try {
      await deleteDictItem.mutateAsync(item.id)
    } catch (error: any) {
      message.error(error?.response?.data?.message || '删除失败')
    }
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="系统管理"
        title="字典管理"
        description="维护系统枚举字典与字典项，供各业务模块统一取值"
        actions={
          <UiButton onClick={openCreate}>
            <Plus />
            新建字典
          </UiButton>
        }
      />

      <TablePanel>
        <TableToolbar>
          <span className="text-muted-foreground text-[11px]">
            字典编码全局唯一，删除前请确认无业务引用
          </span>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {rows.length} 个字典
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无字典</TableEmpty>
        ) : (
          <UiTable>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[200px]">字典</TableHead>
                <TableHead>描述</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((dict: DictType) => (
                <TableRow key={dict.id}>
                  <TableCell>
                    <div
                      className="border-l-4 pl-2"
                      style={{
                        borderLeftColor:
                          dict.status === 'active'
                            ? 'var(--color-success)'
                            : 'var(--color-error)',
                      }}
                    >
                      <div className="font-semibold">{dict.name}</div>
                      <div className="text-muted-foreground mt-0.5 font-mono text-[10px]">
                        {dict.code}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-muted-foreground max-w-[320px] truncate">
                    {dict.description || '-'}
                  </TableCell>
                  <TableCell className="text-center">
                    <Badge
                      variant={dict.status === 'active' ? 'success' : 'destructive'}
                    >
                      {dict.status === 'active' ? '启用' : '禁用'}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-1">
                      <UiButton variant="ghost" size="icon" aria-label="字典项" onClick={() => openItems(dict)}>
                        <Plus />
                      </UiButton>
                      <UiButton variant="ghost" size="icon" aria-label="编辑字典" onClick={() => openEdit(dict)}>
                        <Pencil />
                      </UiButton>
                      <Popconfirm
                        title="确定删除该字典？"
                        onConfirm={() => deleteDictType.mutate(dict.id)}
                      >
                        <UiButton
                          variant="ghost"
                          size="icon"
                          aria-label="删除字典"
                          className="text-destructive hover:text-destructive"
                        >
                          <Trash2 />
                        </UiButton>
                      </Popconfirm>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </UiTable>
        )}

        <div className="text-muted-foreground border-t border-[var(--color-border-secondary)] px-3 py-2 text-[11px]">
          共 {rows.length} 个字典
        </div>
      </TablePanel>

      <Modal
        title={editingType ? '编辑字典' : '新建字典'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        confirmLoading={createDictType.isPending || updateDictType.isPending}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="字典名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="code"
            label="字典编码"
            rules={[{ required: true }, { pattern: /^[a-z0-9_]+$/, message: '仅支持小写字母、数字和下划线' }]}
          >
            <Input disabled={!!editingType} placeholder="例如 order_status" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
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

      <Modal
        title={itemsDict ? `字典项管理 — ${itemsDict.name} (${itemsDict.code})` : '字典项管理'}
        open={!!itemsDict}
        onCancel={() => setItemsDict(null)}
        footer={null}
        width={760}
      >
        <div className="space-y-3">
          <Table
            size="small"
            rowKey="id"
            loading={itemsLoading}
            dataSource={items ?? []}
            pagination={false}
            columns={[
              { title: '标签', dataIndex: 'label', key: 'label' },
              { title: '值', dataIndex: 'value', key: 'value', render: (value: string) => <span className="font-mono text-xs">{value}</span> },
              { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
              {
                title: '状态',
                dataIndex: 'status',
                key: 'status',
                width: 90,
                render: (status: string) =>
                  status === 'active' ? <Tag color="green">启用</Tag> : <Tag color="red">禁用</Tag>,
              },
              {
                title: '操作',
                key: 'action',
                width: 120,
                render: (_: unknown, item: DictItem) => (
                  <span className="flex items-center gap-1">
                    <Button type="link" size="small" onClick={() => openEditItem(item)}>
                      编辑
                    </Button>
                    <Popconfirm title="确定删除该字典项？" onConfirm={() => removeItem(item)}>
                      <Button type="link" size="small" danger>
                        删除
                      </Button>
                    </Popconfirm>
                  </span>
                ),
              },
            ]}
          />

          <Form form={itemForm} layout="inline" className="gap-2 border-t pt-3">
            <Form.Item name="label" label="标签" rules={[{ required: true }]}>
              <Input placeholder="显示名称" style={{ width: 140 }} />
            </Form.Item>
            <Form.Item name="value" label="值" rules={[{ required: true }]}>
              <Input placeholder="实际值" style={{ width: 120 }} />
            </Form.Item>
            <Form.Item name="sort" label="排序">
              <InputNumber min={0} max={9999} style={{ width: 80 }} />
            </Form.Item>
            <Form.Item name="status" label="状态">
              <Select
                style={{ width: 90 }}
                options={[
                  { label: '启用', value: 'active' },
                  { label: '禁用', value: 'disabled' },
                ]}
              />
            </Form.Item>
            <Form.Item>
              <Button type="primary" onClick={submitItem} loading={createDictItem.isPending || updateDictItem.isPending}>
                {editingItem ? '保存修改' : '新增字典项'}
              </Button>
            </Form.Item>
          </Form>
        </div>
      </Modal>
    </div>
  )
}
