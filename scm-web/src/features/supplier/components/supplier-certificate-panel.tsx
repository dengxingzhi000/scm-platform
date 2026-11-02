"use client";

import { useState } from "react";
import type { ReactNode } from "react";
import dayjs from "dayjs";
import { CheckCircle2, Pencil, Plus, Trash2 } from "lucide-react";

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
  useCreateSupplierCertificate,
  useDeleteSupplierCertificate,
  useSupplierCertificates,
  useUpdateSupplierCertificate,
  useVerifySupplierCertificate,
} from "../hooks";
import { errorMessage } from "../lib/api-error";
import {
  CERTIFICATE_STATUSES,
  CERTIFICATE_TYPES,
  labelOf,
  variantOf,
} from "../lib/constants";
import { formatDate } from "../lib/mappers";
import type { SupplierCertificate } from "../types";

interface SupplierCertificatePanelProps {
  supplierId: string;
}

/** 缺失值的统一占位 —— 与概览分区一致。 */
const MISSING = "-";

/** 行内错误条。 */
const ERROR_STRIP =
  "border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm";

/** `<input type="date">` 的值 —— 后端给的是日期串，取前 10 位即可。 */
const toDateValue = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD") : "";

/**
 * 剩余天数的着色：已过期（<= 0）与一周内到期（1..7）都是红色，
 * 一个月内（8..30）是琥珀色，更远的是中性色。
 */
const daysClassOf = (days: number | null): string => {
  if (days === null) return "text-muted-foreground";
  if (days <= 7) return "text-[var(--color-error-text)]";
  if (days <= 30) return "text-[var(--color-warning-text)]";
  return "text-muted-foreground";
};

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

interface CertificateFormDialogProps {
  open: boolean;
  supplierId: string;
  /** 传 null 表示新增。 */
  certificate: SupplierCertificate | null;
  onOpenChange: (open: boolean) => void;
}

/**
 * 表单弹窗。表单只在打开时挂载 —— 新增/编辑的变更 hook 只在真正编辑时才有意义，
 * 关着的时候不需要持有。
 */
function CertificateFormDialog({
  open,
  supplierId,
  certificate,
  onOpenChange,
}: CertificateFormDialogProps) {
  const create = useCreateSupplierCertificate();
  const update = useUpdateSupplierCertificate();

  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    certificateType: certificate?.certificateType ?? "",
    certificateName: certificate?.certificateName ?? "",
    certificateNo: certificate?.certificateNo ?? "",
    issuingAuthority: certificate?.issuingAuthority ?? "",
    validFrom: toDateValue(certificate?.validFrom),
    validTo: toDateValue(certificate?.validTo),
    fileUrl: certificate?.fileUrl ?? "",
  });

  const set = (key: keyof typeof form, value: string) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const pending = create.isPending || update.isPending;
  // 后端校验证照类型与证照名称必填。
  const canSubmit =
    form.certificateType.trim().length > 0 &&
    form.certificateName.trim().length > 0;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setError(null);
    // 状态交给后端按有效期判定 —— 前端不代填，免得把审核结论覆盖掉。
    const data = {
      certificateType: form.certificateType.trim(),
      certificateName: form.certificateName.trim(),
      certificateNo: form.certificateNo.trim() || null,
      issuingAuthority: form.issuingAuthority.trim() || null,
      validFrom: form.validFrom || null,
      validTo: form.validTo || null,
      fileUrl: form.fileUrl.trim() || null,
    };
    try {
      if (certificate) {
        await update.mutateAsync({ supplierId, id: certificate.id, data });
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
          <DialogTitle>
            {certificate ? "编辑资质证照" : "新增资质证照"}
          </DialogTitle>
          <DialogDescription>
            证照类型与证照名称必填；状态由后端按有效期与审核结论判定。
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="证照类型" required htmlFor="certificate-type">
            <Select
              value={form.certificateType}
              onValueChange={(value) => set("certificateType", value)}
            >
              <SelectTrigger id="certificate-type">
                <SelectValue placeholder="请选择证照类型" />
              </SelectTrigger>
              <SelectContent>
                {CERTIFICATE_TYPES.map((type) => (
                  <SelectItem key={type.value} value={type.value}>
                    {type.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
          <Field label="证照名称" required htmlFor="certificate-name">
            <Input
              id="certificate-name"
              value={form.certificateName}
              onChange={(event) => set("certificateName", event.target.value)}
              placeholder="例如：食品生产许可证"
            />
          </Field>
          <Field label="证照编号" htmlFor="certificate-no">
            <Input
              id="certificate-no"
              value={form.certificateNo}
              onChange={(event) => set("certificateNo", event.target.value)}
              placeholder="例如：SC12345678901234"
              className="font-mono"
            />
          </Field>
          <Field label="发证机关" htmlFor="certificate-authority">
            <Input
              id="certificate-authority"
              value={form.issuingAuthority}
              onChange={(event) => set("issuingAuthority", event.target.value)}
              placeholder="例如：广东省市场监督管理局"
            />
          </Field>
          <Field label="有效期起" htmlFor="certificate-valid-from">
            <Input
              id="certificate-valid-from"
              type="date"
              value={form.validFrom}
              onChange={(event) => set("validFrom", event.target.value)}
            />
          </Field>
          <Field label="有效期止" htmlFor="certificate-valid-to">
            <Input
              id="certificate-valid-to"
              type="date"
              value={form.validTo}
              onChange={(event) => set("validTo", event.target.value)}
            />
          </Field>
          <Field label="文件地址" htmlFor="certificate-file" className="sm:col-span-2">
            <Input
              id="certificate-file"
              value={form.fileUrl}
              onChange={(event) => set("fileUrl", event.target.value)}
              placeholder="例如：https://files.example.com/cert-1.pdf"
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

interface CertificateVerifyDialogProps {
  open: boolean;
  supplierId: string;
  certificate: SupplierCertificate | null;
  onOpenChange: (open: boolean) => void;
}

/** 审核弹窗 —— 通过 / 驳回 + 备注。 */
function CertificateVerifyDialog({
  open,
  supplierId,
  certificate,
  onOpenChange,
}: CertificateVerifyDialogProps) {
  const verify = useVerifySupplierCertificate();

  const [approved, setApproved] = useState(true);
  const [remark, setRemark] = useState("");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!certificate) return;
    setError(null);
    try {
      await verify.mutateAsync({
        supplierId,
        id: certificate.id,
        approved,
        remark: remark.trim() || undefined,
      });
      onOpenChange(false);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>审核资质证照</DialogTitle>
          <DialogDescription>
            通过后由后端按有效期判定状态；驳回则标记为已驳回。
          </DialogDescription>
        </DialogHeader>

        <div role="radiogroup" aria-label="审核结论" className="flex gap-6">
          <label className="flex items-center gap-2 text-sm">
            <input
              type="radio"
              name="certificate-verify-action"
              className="accent-[var(--color-primary)]"
              checked={approved}
              onChange={() => setApproved(true)}
            />
            通过
          </label>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="radio"
              name="certificate-verify-action"
              className="accent-[var(--color-primary)]"
              checked={!approved}
              onChange={() => setApproved(false)}
            />
            驳回
          </label>
        </div>

        <Field label="审核备注" htmlFor="certificate-remark">
          <Textarea
            id="certificate-remark"
            rows={3}
            value={remark}
            onChange={(event) => setRemark(event.target.value)}
            placeholder="可填写审核意见"
          />
        </Field>

        {error && <div className={ERROR_STRIP}>{error}</div>}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button disabled={verify.isPending} onClick={handleSubmit}>
            {verify.isPending ? "处理中…" : "确认审核"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default function SupplierCertificatePanel({
  supplierId,
}: SupplierCertificatePanelProps) {
  const { data: certificates } = useSupplierCertificates(supplierId);
  const remove = useDeleteSupplierCertificate();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<SupplierCertificate | null>(null);
  const [verifying, setVerifying] = useState<SupplierCertificate | null>(null);
  const [deleting, setDeleting] = useState<SupplierCertificate | null>(null);
  const [error, setError] = useState<string | null>(null);

  const rows = certificates ?? [];

  const openCreate = () => {
    setEditing(null);
    setError(null);
    setFormOpen(true);
  };

  const openEdit = (certificate: SupplierCertificate) => {
    setEditing(certificate);
    setError(null);
    setFormOpen(true);
  };

  const openVerify = (certificate: SupplierCertificate) => {
    setVerifying(certificate);
    setError(null);
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
        <CardTitle>供应商资质证照</CardTitle>
        <CardDescription>
          该供应商的资质证照；状态与剩余天数以服务端返回为准。
        </CardDescription>
        <CardAction>
          <Button onClick={openCreate}>
            <Plus />
            新增资质证照
          </Button>
        </CardAction>
      </CardHeader>

      <CardContent>
        {error && <div className={cn(ERROR_STRIP, "mb-3")}>{error}</div>}

        {rows.length === 0 ? (
          <p className="text-muted-foreground text-xs">暂无资质证照。</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>证照名称</TableHead>
                <TableHead>类型</TableHead>
                <TableHead>证照编号</TableHead>
                <TableHead>发证机关</TableHead>
                <TableHead>有效期起</TableHead>
                <TableHead>有效期止</TableHead>
                <TableHead>剩余天数</TableHead>
                <TableHead>状态</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((certificate) => {
                const name = certificate.certificateName ?? certificate.id;
                const days = certificate.daysToExpiry;
                return (
                  <TableRow key={certificate.id}>
                    <TableCell>{certificate.certificateName ?? MISSING}</TableCell>
                    <TableCell>
                      {labelOf(CERTIFICATE_TYPES, certificate.certificateType)}
                    </TableCell>
                    <TableCell className="font-mono">
                      {certificate.certificateNo ?? MISSING}
                    </TableCell>
                    <TableCell>
                      {certificate.issuingAuthority ?? MISSING}
                    </TableCell>
                    <TableCell className="font-mono">
                      {formatDate(certificate.validFrom)}
                    </TableCell>
                    <TableCell className="font-mono">
                      {formatDate(certificate.validTo)}
                    </TableCell>
                    <TableCell
                      className={cn("font-mono tabular-nums", daysClassOf(days))}
                    >
                      {days === null ? MISSING : days}
                    </TableCell>
                    <TableCell>
                      <Badge variant={variantOf(CERTIFICATE_STATUSES, certificate.status)}>
                        {labelOf(CERTIFICATE_STATUSES, certificate.status)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`编辑${name}`}
                          onClick={() => openEdit(certificate)}
                        >
                          <Pencil />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`审核${name}`}
                          onClick={() => openVerify(certificate)}
                        >
                          <CheckCircle2 />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`删除${name}`}
                          onClick={() => setDeleting(certificate)}
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
        <CertificateFormDialog
          key={editing?.id ?? "new"}
          open={formOpen}
          supplierId={supplierId}
          certificate={editing}
          onOpenChange={setFormOpen}
        />
      )}

      {verifying && (
        <CertificateVerifyDialog
          key={verifying.id}
          open
          supplierId={supplierId}
          certificate={verifying}
          onOpenChange={(open) => {
            if (!open) setVerifying(null);
          }}
        />
      )}

      <ConfirmDialog
        open={deleting !== null}
        title="删除资质证照"
        description={`确定要删除资质证照「${deleting?.certificateName ?? MISSING}」吗？`}
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
