"use client";

import { useState } from "react";
import { Pencil, Plus, Search, Trash2 } from "lucide-react";
import { Popconfirm } from "antd";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PageHeader } from "@/components/ui/page-header";
import {
  TableEmpty,
  TableLoading,
  TablePanel,
  TablePagination,
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
import { useDeleteUser, useUserList } from "../../hooks";
import type { User } from "../../types";
import UserForm from "./user-form";

const PAGE_SIZE = 20;

const statusConfig: Record<
  User["status"],
  {
    label: string;
    variant: "success" | "warning" | "destructive";
    accent: string;
  }
> = {
  active: {
    label: "启用",
    variant: "success",
    accent: "var(--color-success)",
  },
  disabled: {
    label: "禁用",
    variant: "destructive",
    accent: "var(--color-error)",
  },
  locked: {
    label: "锁定",
    variant: "warning",
    accent: "var(--color-warning)",
  },
};

export default function UserList() {
  const [current, setCurrent] = useState(1);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [formOpen, setFormOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const { data, isLoading } = useUserList({ current, keyword });
  const deleteUser = useDeleteUser();

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="系统管理"
        title="用户管理"
        description="维护账号、部门归属、角色与登录状态"
        actions={
          <Button
            onClick={() => {
              setEditingUser(null);
              setFormOpen(true);
            }}
          >
            <Plus />
            新建用户
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <form
            onSubmit={(event) => {
              event.preventDefault();
              setKeyword(keywordDraft.trim());
              setCurrent(1);
            }}
            className="flex items-center gap-2"
          >
            <Input
              value={keywordDraft}
              onChange={(event) => setKeywordDraft(event.target.value)}
              placeholder="搜索用户名 / 姓名"
              className="h-8 w-56 text-xs"
            />
            <Button
              type="submit"
              variant="outline"
              size="icon"
              className="size-8"
              aria-label="搜索"
            >
              <Search />
            </Button>
          </form>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 个用户
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无用户</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[220px]">用户</TableHead>
                <TableHead>部门</TableHead>
                <TableHead>邮箱</TableHead>
                <TableHead>角色</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((user) => {
                const config = statusConfig[user.status];
                return (
                  <TableRow key={user.id}>
                    <TableCell>
                      <div
                        className="border-l-4 pl-2"
                        style={{ borderLeftColor: config.accent }}
                      >
                        <div className="font-semibold">
                          {user.displayName || user.username}
                        </div>
                        <div className="text-muted-foreground mt-0.5 font-mono text-[10px]">
                          {user.username}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>{user.deptName || "-"}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {user.email || "-"}
                    </TableCell>
                    <TableCell>
                      <div className="flex flex-wrap items-center gap-1">
                        {(user.roles ?? []).slice(0, 2).map((role) => (
                          <Badge key={role} variant="secondary">
                            {role}
                          </Badge>
                        ))}
                        {(user.roles?.length ?? 0) > 2 && (
                          <Badge variant="outline">
                            +{(user.roles?.length ?? 0) - 2}
                          </Badge>
                        )}
                        {(user.roles?.length ?? 0) === 0 && "-"}
                      </div>
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={config.variant}>{config.label}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {user.createdAt}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              aria-label="编辑用户"
                              onClick={() => {
                                setEditingUser(user);
                                setFormOpen(true);
                              }}
                            >
                              <Pencil />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>编辑</TooltipContent>
                        </Tooltip>
                        <Popconfirm
                          title="确定删除此用户？"
                          onConfirm={() => deleteUser.mutate(user.id)}
                        >
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label="删除用户"
                            className="text-destructive hover:text-destructive"
                          >
                            <Trash2 />
                          </Button>
                        </Popconfirm>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}

        <TablePagination
          page={current}
          pageSize={PAGE_SIZE}
          total={total}
          pageCount={pageCount}
          onPageChange={setCurrent}
        />
      </TablePanel>

      <UserForm
        open={formOpen}
        onClose={() => setFormOpen(false)}
        user={editingUser}
      />
    </div>
  );
}
