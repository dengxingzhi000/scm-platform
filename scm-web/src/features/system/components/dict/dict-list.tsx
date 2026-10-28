"use client";

import { useState } from "react";
import { Form, Input, Modal, Popconfirm } from "antd";
import { Pencil, Plus, Trash2 } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import {
  TableEmpty,
  TableLoading,
  TablePanel,
  TableToolbar,
  TableToolbarEnd,
} from "@/components/ui/table-shell";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useCreateDictType, useDeleteDictType, useDictTypeList } from "../../hooks";
import type { DictType } from "../../types";

export default function DictList() {
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm();
  const { data: dictTypes, isLoading } = useDictTypeList();
  const createDictType = useCreateDictType();
  const deleteDictType = useDeleteDictType();
  const rows = dictTypes ?? [];

  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      await createDictType.mutateAsync(values);
      setModalOpen(false);
      form.resetFields();
    } catch {}
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="系统管理"
        title="字典管理"
        description="维护系统枚举字典与字典项"
        actions={
          <Button onClick={() => setModalOpen(true)}>
            <Plus />
            新建字典
          </Button>
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
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[200px]">字典</TableHead>
                <TableHead>描述</TableHead>
                <TableHead numeric>字典项</TableHead>
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
                          dict.status === "active"
                            ? "var(--color-success)"
                            : "var(--color-error)",
                      }}
                    >
                      <div className="font-semibold">{dict.name}</div>
                      <div className="text-muted-foreground mt-0.5 font-mono text-[10px]">
                        {dict.code}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-muted-foreground max-w-[320px] truncate">
                    {dict.description || "-"}
                  </TableCell>
                  <TableCell numeric>{dict.itemCount ?? 0}</TableCell>
                  <TableCell className="text-center">
                    <Badge
                      variant={
                        dict.status === "active" ? "success" : "destructive"
                      }
                    >
                      {dict.status === "active" ? "启用" : "禁用"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label="编辑字典"
                      >
                        <Pencil />
                      </Button>
                      <Popconfirm
                        title="确定删除该字典？"
                        onConfirm={() => deleteDictType.mutate(dict.id)}
                      >
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="删除字典"
                          className="text-destructive hover:text-destructive"
                        >
                          <Trash2 />
                        </Button>
                      </Popconfirm>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        <div className="text-muted-foreground border-t border-[var(--color-border-secondary)] px-3 py-2 text-[11px]">
          共 {rows.length} 个字典
        </div>
      </TablePanel>

      <Modal
        title="新建字典"
        open={modalOpen}
        onOk={handleCreate}
        onCancel={() => setModalOpen(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="字典名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="code" label="字典编码" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
