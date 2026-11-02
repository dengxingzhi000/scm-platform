"use client";

import { useState } from "react";
import {
  MoreHorizontal,
  KeyRound,
  Lock,
  LogOut,
  Pencil,
  Plus,
  BarChart3,
  Search,
  Trash2,
  Unlock,
  UserCog,
} from "lucide-react";
import { Descriptions, Popconfirm, Select as AntSelect } from "antd";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
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
import { Textarea } from "@/components/ui/textarea";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  useDeleteUser,
  useForceLogoutUser,
  useGrantUserRoles,
  useLockUser,
  useResetUserPassword,
  useRoleList,
  useUserList,
  useUserStatistics,
} from "../../hooks";
import { userService } from "../../services/user.service";
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
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [formOpen, setFormOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);

  const [rolesUser, setRolesUser] = useState<User | null>(null);
  const [roleIds, setRoleIds] = useState<string[]>([]);
  const [logoutUser, setLogoutUser] = useState<User | null>(null);
  const [logoutReason, setLogoutReason] = useState("");
  const [statsUser, setStatsUser] = useState<User | null>(null);
  const [passwordResult, setPasswordResult] = useState<{
    username: string;
    password: string;
  } | null>(null);

  const { data, isLoading } = useUserList({ current, keyword, status });
  const deleteUser = useDeleteUser();
  const resetPassword = useResetUserPassword();
  const lockUser = useLockUser();
  const grantRoles = useGrantUserRoles();
  const forceLogout = useForceLogoutUser();
  const { data: roles } = useRoleList();
  const { data: statistics, isLoading: statisticsLoading } = useUserStatistics(
    statsUser?.id ?? null,
  );

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const openRoles = async (user: User) => {
    try {
      // 列表接口不返回 roleIds，先取详情再打开弹窗，避免直接保存把已有角色清空。
      const detail = await userService.getById(user.id);
      setRoleIds(detail.roleIds ?? []);
      setRolesUser(detail);
    } catch {
      setRoleIds([]);
      setRolesUser(user);
    }
  };

  const handleResetPassword = (user: User) => {
    resetPassword
      .mutateAsync(user.id)
      .then((password) =>
        setPasswordResult({ username: user.username, password }),
      )
      .catch(() => undefined);
  };

  const submitRoles = async () => {
    if (!rolesUser) return;
    await grantRoles.mutateAsync({ id: rolesUser.id, roleIds });
    setRolesUser(null);
  };

  const submitLogout = async () => {
    if (!logoutUser || !logoutReason.trim()) return;
    await forceLogout.mutateAsync({
      id: logoutUser.id,
      reason: logoutReason.trim(),
    });
    setLogoutUser(null);
    setLogoutReason("");
  };

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
          <AntSelect
            allowClear
            placeholder="全部状态"
            style={{ width: 120 }}
            value={status}
            onChange={(value) => {
              setStatus(value);
              setCurrent(1);
            }}
            options={[
              { label: "启用", value: "active" },
              { label: "禁用", value: "disabled" },
              { label: "锁定", value: "locked" },
            ]}
          />
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
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              aria-label="更多操作"
                            >
                              <MoreHorizontal />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onSelect={() => openRoles(user)}>
                              <UserCog />
                              分配角色
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onSelect={() =>
                                lockUser.mutate({
                                  id: user.id,
                                  lock: user.status !== "locked",
                                })
                              }
                            >
                              {user.status === "locked" ? <Unlock /> : <Lock />}
                              {user.status === "locked" ? "解锁账号" : "锁定账号"}
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onSelect={() => handleResetPassword(user)}
                            >
                              <KeyRound />
                              重置密码
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onSelect={() => {
                                setLogoutReason("");
                                setLogoutUser(user);
                              }}
                            >
                              <LogOut />
                              强制下线
                            </DropdownMenuItem>
                            <DropdownMenuItem onSelect={() => setStatsUser(user)}>
                              <BarChart3 />
                              查看统计
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
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

      <Dialog
        open={!!rolesUser}
        onOpenChange={(open) => !open && setRolesUser(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>分配角色</DialogTitle>
            <DialogDescription>
              {rolesUser?.displayName || rolesUser?.username}
            </DialogDescription>
          </DialogHeader>
          <AntSelect
            mode="multiple"
            style={{ width: "100%" }}
            value={roleIds}
            onChange={(value) => setRoleIds(value)}
            placeholder="选择角色"
            options={(roles ?? []).map((role) => ({
              label: role.name,
              value: role.id,
            }))}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setRolesUser(null)}>
              取消
            </Button>
            <Button disabled={grantRoles.isPending} onClick={submitRoles}>
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!logoutUser}
        onOpenChange={(open) => !open && setLogoutUser(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>强制下线</DialogTitle>
            <DialogDescription>
              将 {logoutUser?.displayName || logoutUser?.username} 的所有会话强制失效。
            </DialogDescription>
          </DialogHeader>
          <Textarea
            value={logoutReason}
            onChange={(event) => setLogoutReason(event.target.value)}
            placeholder="请输入下线原因"
            maxLength={200}
            rows={3}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setLogoutUser(null)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={!logoutReason.trim() || forceLogout.isPending}
              onClick={submitLogout}
            >
              确认下线
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={!!statsUser} onOpenChange={(open) => !open && setStatsUser(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>用户统计</DialogTitle>
            <DialogDescription>
              {statsUser?.displayName || statsUser?.username}
            </DialogDescription>
          </DialogHeader>
          {statisticsLoading ? (
            <p className="text-muted-foreground text-sm">加载中…</p>
          ) : (
            <Descriptions
              column={1}
              size="small"
              items={Object.entries(statistics ?? {}).map(([key, value]) => ({
                key,
                label: key,
                children:
                  value !== null && typeof value === "object"
                    ? JSON.stringify(value)
                    : String(value ?? "-"),
              }))}
            />
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setStatsUser(null)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!passwordResult}
        onOpenChange={(open) => !open && setPasswordResult(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>密码已重置</DialogTitle>
            <DialogDescription>
              请将新密码告知 {passwordResult?.username}，并提示其登录后修改。
            </DialogDescription>
          </DialogHeader>
          <div className="bg-muted rounded-lg p-3 font-mono text-sm">
            {passwordResult?.password || "(接口未返回新密码)"}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setPasswordResult(null)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
