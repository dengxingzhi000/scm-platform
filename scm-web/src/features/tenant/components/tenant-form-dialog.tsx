"use client";

import * as React from "react";

import { Button } from "@/components/ui/button";
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
import { Textarea } from "@/components/ui/textarea";
import type { TenantDTO } from "@/lib/api/contracts/tenant";

import {
  useActivePackages,
  useProvisionTenant,
  useUpdateTenant,
} from "../hooks";
import { TENANT_STATUS, TENANT_TYPE } from "../lib/format";

const NONE = "__none__";

function Field({
  label,
  required,
  children,
  className,
}: {
  label: string;
  required?: boolean;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={className}>
      <Label className="mb-1.5 text-[12px]">
        {label}
        {required && <span className="text-destructive ml-0.5">*</span>}
      </Label>
      {children}
    </div>
  );
}

interface TenantFormDialogProps {
  open: boolean;
  tenant?: TenantDTO | null;
  onOpenChange: (open: boolean) => void;
}

/**
 * Creating a tenant goes through `/tenant/provisioning`, which also creates the
 * trial subscription and default quotas; editing reuses PUT `/tenant`.
 */
export function TenantFormDialog({
  open,
  tenant,
  onOpenChange,
}: TenantFormDialogProps) {
  const isEdit = Boolean(tenant?.id);
  const [form, setForm] = React.useState({
    tenantName: tenant?.tenantName ?? "",
    tenantNameEn: tenant?.tenantNameEn ?? "",
    tenantCode: tenant?.tenantCode ?? "",
    tenantType: String(tenant?.tenantType ?? 1),
    companyName: tenant?.companyName ?? "",
    industry: tenant?.industry ?? "",
    contactName: tenant?.contactName ?? "",
    contactPhone: tenant?.contactPhone ?? "",
    contactEmail: tenant?.contactEmail ?? "",
    adminUsername: tenant?.adminUsername ?? "",
    adminEmail: tenant?.adminEmail ?? "",
    status: String(tenant?.status ?? 1),
    packageId: "",
    remark: tenant?.remark ?? "",
  });

  const set = (key: keyof typeof form, value: string) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const { data: packages } = useActivePackages();
  const provision = useProvisionTenant();
  const update = useUpdateTenant();
  const pending = provision.isPending || update.isPending;

  const canSubmit = form.tenantName.trim() && (isEdit || form.tenantCode.trim());

  const handleSubmit = async () => {
    if (!canSubmit) return;
    if (isEdit && tenant) {
      await update.mutateAsync({
        ...tenant,
        tenantName: form.tenantName.trim(),
        tenantNameEn: form.tenantNameEn.trim() || null,
        tenantType: Number(form.tenantType),
        companyName: form.companyName.trim() || null,
        industry: form.industry.trim() || null,
        contactName: form.contactName.trim() || null,
        contactPhone: form.contactPhone.trim() || null,
        contactEmail: form.contactEmail.trim() || null,
        adminUsername: form.adminUsername.trim() || null,
        adminEmail: form.adminEmail.trim() || null,
        status: Number(form.status),
        remark: form.remark.trim() || null,
      });
      onOpenChange(false);
      return;
    }

    const result = await provision.mutateAsync({
      tenantName: form.tenantName.trim(),
      tenantNameEn: form.tenantNameEn.trim() || undefined,
      tenantCode: form.tenantCode.trim(),
      tenantType: Number(form.tenantType),
      companyName: form.companyName.trim() || undefined,
      industry: form.industry.trim() || undefined,
      contactName: form.contactName.trim() || undefined,
      contactPhone: form.contactPhone.trim() || undefined,
      contactEmail: form.contactEmail.trim() || undefined,
      adminUsername: form.adminUsername.trim() || undefined,
      adminEmail: form.adminEmail.trim() || undefined,
      packageId:
        form.packageId && form.packageId !== NONE ? form.packageId : undefined,
    });
    if (result?.success === false) return;
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? "编辑租户" : "开通租户"}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? "更新租户档案与状态；租户编码创建后不可修改。"
              : "开通流程会同时创建租户、试用订阅与默认资源配额，需要先存在启用的试用套餐。"}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="租户名称" required>
            <Input
              value={form.tenantName}
              onChange={(event) => set("tenantName", event.target.value)}
              placeholder="例如：华东仓储有限公司"
            />
          </Field>
          <Field label="租户编码" required={!isEdit}>
            <Input
              value={form.tenantCode}
              disabled={isEdit}
              onChange={(event) => set("tenantCode", event.target.value)}
              placeholder="例如：east-warehouse"
            />
          </Field>
          <Field label="英文名称">
            <Input
              value={form.tenantNameEn}
              onChange={(event) => set("tenantNameEn", event.target.value)}
            />
          </Field>
          <Field label="租户类型">
            <Select
              value={form.tenantType}
              onValueChange={(value) => set("tenantType", value)}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {Object.entries(TENANT_TYPE).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
          <Field label="公司名称">
            <Input
              value={form.companyName}
              onChange={(event) => set("companyName", event.target.value)}
            />
          </Field>
          <Field label="所属行业">
            <Input
              value={form.industry}
              onChange={(event) => set("industry", event.target.value)}
            />
          </Field>
          <Field label="联系人">
            <Input
              value={form.contactName}
              onChange={(event) => set("contactName", event.target.value)}
            />
          </Field>
          <Field label="联系电话">
            <Input
              value={form.contactPhone}
              onChange={(event) => set("contactPhone", event.target.value)}
            />
          </Field>
          <Field label="联系邮箱">
            <Input
              type="email"
              value={form.contactEmail}
              onChange={(event) => set("contactEmail", event.target.value)}
            />
          </Field>
          <Field label="管理员账号">
            <Input
              value={form.adminUsername}
              onChange={(event) => set("adminUsername", event.target.value)}
            />
          </Field>
          <Field label="管理员邮箱">
            <Input
              type="email"
              value={form.adminEmail}
              onChange={(event) => set("adminEmail", event.target.value)}
            />
          </Field>
          {isEdit ? (
            <Field label="租户状态">
              <Select
                value={form.status}
                onValueChange={(value) => set("status", value)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(TENANT_STATUS).map(([value, status]) => (
                    <SelectItem key={value} value={value}>
                      {status.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>
          ) : (
            <Field label="开通套餐" className="sm:col-span-2">
              <Select
                value={form.packageId || NONE}
                onValueChange={(value) => set("packageId", value)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="使用默认试用套餐" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={NONE}>默认试用套餐</SelectItem>
                  {(packages ?? []).map((item) => (
                    <SelectItem key={item.id} value={item.id}>
                      {item.packageName}（{item.packageCode}）
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </Field>
          )}
          <Field label="备注" className="sm:col-span-2">
            <Textarea
              rows={2}
              value={form.remark}
              onChange={(event) => set("remark", event.target.value)}
            />
          </Field>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button disabled={!canSubmit || pending} onClick={handleSubmit}>
            {pending ? "保存中…" : isEdit ? "保存" : "开通"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
