"use client";

import { useState } from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { Popconfirm } from "antd";

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
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { useDeleteRole, useRoleList } from "../../hooks";
import type { Role } from "../../types";
import RoleForm from "./role-form";

export default function RoleList() {
  const [formOpen, setFormOpen] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const { data: roles, isLoading } = useRoleList();
  const deleteRole = useDeleteRole();
  const rows = roles ?? [];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="系统管理"
        title="角色管理"
        description="配置角色、数据范围与功能权限"
        actions={
          <Button
            onClick={() => {
              setEditingRole(null);
              setFormOpen(true);
            }}
          >
            <Plus />
            新建角色
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <span className="text-muted-foreground text-[11px]">
            角色编码以 ROLE_ 开头，权限在编辑弹窗中勾选
          </span>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {rows.length} 个角色
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无角色</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[200px]">角色</TableHead>
                <TableHead>描述</TableHead>
                <TableHead numeric>用户数</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((role) => (
                <TableRow key={role.id}>
                  <TableCell>
                    <div
                      className="border-l-4 pl-2"
                      style={{
                        borderLeftColor:
                          role.status === "active"
                            ? "var(--color-success)"
                            : "var(--color-error)",
                      }}
                    >
                      <div className="font-semibold">{role.name}</div>
                      <div className="text-muted-foreground mt-0.5 font-mono text-[10px]">
                        {role.code}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-muted-foreground max-w-[280px] truncate">
                    {role.description || "-"}
                  </TableCell>
                  <TableCell numeric>{role.userCount ?? 0}</TableCell>
                  <TableCell className="text-center">
                    <Badge
                      variant={
                        role.status === "active" ? "success" : "destructive"
                      }
                    >
                      {role.status === "active" ? "启用" : "禁用"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {role.createdAt}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-1">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label="编辑角色"
                            onClick={() => {
                              setEditingRole(role);
                              setFormOpen(true);
                            }}
                          >
                            <Pencil />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>编辑</TooltipContent>
                      </Tooltip>
                      <Popconfirm
                        title="确定删除该角色？"
                        onConfirm={() => deleteRole.mutate(role.id)}
                      >
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="删除角色"
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
          共 {rows.length} 个角色
        </div>
      </TablePanel>

      <RoleForm
        key={editingRole?.id ?? "new"}
        open={formOpen}
        onClose={() => setFormOpen(false)}
        role={editingRole}
      />
    </div>
  );
}
