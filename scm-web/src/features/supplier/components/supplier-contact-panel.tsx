"use client";

import { useState } from "react";
import type { ReactNode } from "react";
import { Pencil, Plus, Star, Trash2 } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import {
  useCreateSupplierContact,
  useDeleteSupplierContact,
  useSetPrimarySupplierContact,
  useSupplierContacts,
  useUpdateSupplierContact,
} from "../hooks";
import { errorMessage } from "../lib/api-error";
import { CONTACT_ROLES, labelOf } from "../lib/constants";
import type { SupplierContact } from "../types";

interface SupplierContactPanelProps {
  supplierId: string;
}

/** 缺失值的统一占位 —— 与概览分区一致。 */
const MISSING = "-";

/** 「未指定角色」的下拉占位值 —— 与后端枚举值不会冲突。 */
const NO_ROLE = "__none__";

/** 行内错误条。 */
const ERROR_STRIP =
  "border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm";

function Field({
  label,
  required,
  htmlFor,
  className,
  children,
}: {
  label: string;
  required?: boolean;
  htmlFor: string;
  className?: string;
  children: ReactNode;
}) {
  return (
    <div className={className}>
      <div className="mb-1.5 flex items-center gap-1">
        <Label className="text-[12px]" htmlFor={htmlFor}>
          {label}
        </Label>
        {required && <span className="text-destructive text-[12px]">*</span>}
      </div>
      {children}
    </div>
  );
}

interface ContactFormDialogProps {
  open: boolean;
  supplierId: string;
  /** 传 null 表示新增。 */
  contact: SupplierContact | null;
  onOpenChange: (open: boolean) => void;
}

/**
 * 表单弹窗。表单只在打开时挂载 —— 新增/编辑的变更 hook 只在真正编辑时才有意义，
 * 关着的时候不需要持有。
 */
function ContactFormDialog({
  open,
  supplierId,
  contact,
  onOpenChange,
}: ContactFormDialogProps) {
  const create = useCreateSupplierContact();
  const update = useUpdateSupplierContact();

  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    contactName: contact?.contactName ?? "",
    contactTitle: contact?.contactTitle ?? "",
    contactRole: contact?.contactRole ?? NO_ROLE,
    contactPhone: contact?.contactPhone ?? "",
    contactEmail: contact?.contactEmail ?? "",
    contactAddress: contact?.contactAddress ?? "",
  });

  const set = (key: keyof typeof form, value: string) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const pending = create.isPending || update.isPending;
  const canSubmit = form.contactName.trim().length > 0;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setError(null);
    const data = {
      contactName: form.contactName.trim(),
      contactTitle: form.contactTitle.trim() || null,
      contactRole: form.contactRole === NO_ROLE ? null : form.contactRole,
      contactPhone: form.contactPhone.trim() || null,
      contactEmail: form.contactEmail.trim() || null,
      contactAddress: form.contactAddress.trim() || null,
    };
    try {
      if (contact) {
        await update.mutateAsync({ supplierId, id: contact.id, data });
      } else {
        await create.mutateAsync({ supplierId, data });
      }
      onOpenChange(false);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{contact ? "编辑联系人" : "新增联系人"}</DialogTitle>
          <DialogDescription>姓名必填，其余信息可留空。</DialogDescription>
        </DialogHeader>

        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="姓名" required htmlFor="contact-name">
            <Input
              id="contact-name"
              value={form.contactName}
              onChange={(event) => set("contactName", event.target.value)}
              placeholder="例如：张伟"
            />
          </Field>
          <Field label="职务" htmlFor="contact-title">
            <Input
              id="contact-title"
              value={form.contactTitle}
              onChange={(event) => set("contactTitle", event.target.value)}
              placeholder="例如：销售经理"
            />
          </Field>
          <Field label="角色" htmlFor="contact-role">
            <Select
              value={form.contactRole}
              onValueChange={(value) => set("contactRole", value)}
            >
              <SelectTrigger id="contact-role">
                <SelectValue placeholder="请选择角色" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NO_ROLE}>未指定</SelectItem>
                {CONTACT_ROLES.map((role) => (
                  <SelectItem key={role.value} value={role.value}>
                    {role.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
          <Field label="电话" htmlFor="contact-phone">
            <Input
              id="contact-phone"
              value={form.contactPhone}
              onChange={(event) => set("contactPhone", event.target.value)}
              placeholder="例如：13800002210"
            />
          </Field>
          <Field label="邮箱" htmlFor="contact-email">
            <Input
              id="contact-email"
              type="email"
              value={form.contactEmail}
              onChange={(event) => set("contactEmail", event.target.value)}
              placeholder="例如：zhang@example.com"
            />
          </Field>
          <Field label="地址" htmlFor="contact-address" className="sm:col-span-2">
            <Textarea
              id="contact-address"
              rows={2}
              value={form.contactAddress}
              onChange={(event) => set("contactAddress", event.target.value)}
            />
          </Field>
        </div>

        {error && <div className={ERROR_STRIP}>{error}</div>}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button disabled={!canSubmit || pending} onClick={handleSubmit}>
            {pending ? "保存中…" : "保存"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default function SupplierContactPanel({
  supplierId,
}: SupplierContactPanelProps) {
  const { data: contacts } = useSupplierContacts(supplierId);
  const setPrimary = useSetPrimarySupplierContact();
  const remove = useDeleteSupplierContact();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<SupplierContact | null>(null);
  const [deleting, setDeleting] = useState<SupplierContact | null>(null);
  const [error, setError] = useState<string | null>(null);

  const rows = contacts ?? [];

  const openCreate = () => {
    setEditing(null);
    setError(null);
    setFormOpen(true);
  };

  const openEdit = (contact: SupplierContact) => {
    setEditing(contact);
    setError(null);
    setFormOpen(true);
  };

  const handleSetPrimary = async (contact: SupplierContact) => {
    setError(null);
    try {
      await setPrimary.mutateAsync({ supplierId, id: contact.id });
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const handleDelete = async () => {
    if (!deleting) return;
    setError(null);
    try {
      await remove.mutateAsync({ supplierId, id: deleting.id });
      setDeleting(null);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>供应商联系人</CardTitle>
        <CardDescription>
          该供应商的联系人；删除主联系人后，后端会自动提升下一位。
        </CardDescription>
        <CardAction>
          <Button onClick={openCreate}>
            <Plus />
            新增联系人
          </Button>
        </CardAction>
      </CardHeader>

      <CardContent>
        {error && <div className={cn(ERROR_STRIP, "mb-3")}>{error}</div>}

        {rows.length === 0 ? (
          <p className="text-muted-foreground text-xs">暂无联系人。</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>姓名</TableHead>
                <TableHead>职务</TableHead>
                <TableHead>角色</TableHead>
                <TableHead>电话</TableHead>
                <TableHead>邮箱</TableHead>
                {/*
                  列头不叫「主联系人」：单元格里的徽标用的就是这四个字，
                  同名文本会让按文本查询的用例命中两个节点。
                */}
                <TableHead>是否主联系人</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((contact) => {
                const name = contact.contactName ?? contact.id;
                return (
                  <TableRow key={contact.id}>
                    <TableCell>{contact.contactName ?? MISSING}</TableCell>
                    <TableCell>{contact.contactTitle ?? MISSING}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">
                        {labelOf(CONTACT_ROLES, contact.contactRole)}
                      </Badge>
                    </TableCell>
                    <TableCell className="font-mono">
                      {contact.contactPhone ?? MISSING}
                    </TableCell>
                    <TableCell>{contact.contactEmail ?? MISSING}</TableCell>
                    <TableCell>
                      {contact.isPrimary ? (
                        <Badge variant="default">主联系人</Badge>
                      ) : (
                        <span className="text-muted-foreground">{MISSING}</span>
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`编辑${name}`}
                          onClick={() => openEdit(contact)}
                        >
                          <Pencil />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`将${name}设为主联系人`}
                          disabled={contact.isPrimary === true}
                          onClick={() => void handleSetPrimary(contact)}
                        >
                          <Star />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`删除${name}`}
                          onClick={() => setDeleting(contact)}
                        >
                          <Trash2 />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </CardContent>

      {formOpen && (
        <ContactFormDialog
          key={editing?.id ?? "new"}
          open={formOpen}
          supplierId={supplierId}
          contact={editing}
          onOpenChange={setFormOpen}
        />
      )}

      <ConfirmDialog
        open={deleting !== null}
        title="删除联系人"
        description={`确定要删除联系人「${deleting?.contactName ?? MISSING}」吗？`}
        confirmText="删除"
        destructive
        pending={remove.isPending}
        onConfirm={() => void handleDelete()}
        onOpenChange={(open) => {
          if (!open) setDeleting(null);
        }}
      />
    </Card>
  );
}
