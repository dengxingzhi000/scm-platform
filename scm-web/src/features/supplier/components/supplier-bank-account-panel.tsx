"use client";

import { useState } from "react";
import { Eye, EyeOff, Pencil, Plus, Star, Trash2 } from "lucide-react";

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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";
import {
  useCreateSupplierBankAccount,
  useDeleteSupplierBankAccount,
  useSetDefaultSupplierBankAccount,
  useSupplierBankAccounts,
  useUpdateSupplierBankAccount,
} from "../hooks";
import { errorMessage } from "../lib/api-error";
import { maskBankAccount } from "../lib/mappers";
import type { SupplierBankAccount } from "../types";
import SupplierField from "./supplier-field";

interface SupplierBankAccountPanelProps {
  supplierId: string;
}

/** 缺失值的统一占位 —— 与概览分区一致。 */
const MISSING = "-";

/** 行内错误条。 */
const ERROR_STRIP =
  "border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm";

interface BankAccountFormDialogProps {
  open: boolean;
  supplierId: string;
  /** 传 null 表示新增。 */
  account: SupplierBankAccount | null;
  onOpenChange: (open: boolean) => void;
}

/**
 * 表单弹窗。表单只在打开时挂载 —— 新增/编辑的变更 hook 只在真正编辑时才有意义，
 * 关着的时候不需要持有。
 */
function BankAccountFormDialog({
  open,
  supplierId,
  account,
  onOpenChange,
}: BankAccountFormDialogProps) {
  const create = useCreateSupplierBankAccount();
  const update = useUpdateSupplierBankAccount();

  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    bankName: account?.bankName ?? "",
    bankBranch: account?.bankBranch ?? "",
    bankAccount: account?.bankAccount ?? "",
    accountName: account?.accountName ?? "",
    currency: account?.currency ?? "",
    swiftCode: account?.swiftCode ?? "",
  });

  const set = (key: keyof typeof form, value: string) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const pending = create.isPending || update.isPending;
  // 后端新增时校验开户行 / 账号 / 账户名三项必填，这里先挡住，省一次往返。
  const canSubmit =
    form.bankName.trim().length > 0 &&
    form.bankAccount.trim().length > 0 &&
    form.accountName.trim().length > 0;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setError(null);
    // 默认账户由「设为默认」单独操作决定，表单不代填。
    const data = {
      bankName: form.bankName.trim(),
      bankBranch: form.bankBranch.trim() || null,
      bankAccount: form.bankAccount.trim(),
      accountName: form.accountName.trim(),
      currency: form.currency.trim().toUpperCase() || null,
      swiftCode: form.swiftCode.trim() || null,
    };
    try {
      if (account) {
        await update.mutateAsync({ supplierId, id: account.id, data });
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
            {account ? "编辑银行账户" : "新增银行账户"}
          </DialogTitle>
          <DialogDescription>
            开户行、账号、账户名必填，其余信息可留空。
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-3 sm:grid-cols-2">
          <SupplierField label="开户行" required htmlFor="bank-name">
            <Input
              id="bank-name"
              value={form.bankName}
              onChange={(event) => set("bankName", event.target.value)}
              placeholder="例如：招商银行"
            />
          </SupplierField>
          <SupplierField label="支行" htmlFor="bank-branch">
            <Input
              id="bank-branch"
              value={form.bankBranch}
              onChange={(event) => set("bankBranch", event.target.value)}
              placeholder="例如：深圳分行科技园支行"
            />
          </SupplierField>
          <SupplierField label="账号" required htmlFor="bank-account">
            <Input
              id="bank-account"
              value={form.bankAccount}
              onChange={(event) => set("bankAccount", event.target.value)}
              placeholder="例如：6222021234567890"
              className="font-mono"
            />
          </SupplierField>
          <SupplierField label="账户名" required htmlFor="bank-account-name">
            <Input
              id="bank-account-name"
              value={form.accountName}
              onChange={(event) => set("accountName", event.target.value)}
              placeholder="例如：深圳华强电子有限公司"
            />
          </SupplierField>
          <SupplierField label="币种" htmlFor="bank-currency">
            <Input
              id="bank-currency"
              value={form.currency}
              onChange={(event) => set("currency", event.target.value)}
              placeholder="留空默认人民币，例如：CNY"
              className="font-mono"
            />
          </SupplierField>
          <SupplierField label="SWIFT" htmlFor="bank-swift">
            <Input
              id="bank-swift"
              value={form.swiftCode}
              onChange={(event) => set("swiftCode", event.target.value)}
              placeholder="例如：CMBCCNBS"
              className="font-mono"
            />
          </SupplierField>
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

export default function SupplierBankAccountPanel({
  supplierId,
}: SupplierBankAccountPanelProps) {
  const { data: accounts } = useSupplierBankAccounts(supplierId);
  const setDefault = useSetDefaultSupplierBankAccount();
  const remove = useDeleteSupplierBankAccount();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<SupplierBankAccount | null>(null);
  const [deleting, setDeleting] = useState<SupplierBankAccount | null>(null);
  const [error, setError] = useState<string | null>(null);
  /** 已展开原始账号的账户 id —— 账号默认脱敏。 */
  const [revealed, setRevealed] = useState<string[]>([]);

  const rows = accounts ?? [];

  const openCreate = () => {
    setEditing(null);
    setError(null);
    setFormOpen(true);
  };

  const openEdit = (account: SupplierBankAccount) => {
    setEditing(account);
    setError(null);
    setFormOpen(true);
  };

  const toggleReveal = (id: string) =>
    setRevealed((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id],
    );

  const handleSetDefault = async (account: SupplierBankAccount) => {
    setError(null);
    try {
      await setDefault.mutateAsync({ supplierId, id: account.id });
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
        <CardTitle>供应商银行账户</CardTitle>
        <CardDescription>
          该供应商的结算账户；账号默认脱敏，展开后显示原始账号。
        </CardDescription>
        <CardAction>
          <Button onClick={openCreate}>
            <Plus />
            新增银行账户
          </Button>
        </CardAction>
      </CardHeader>

      <CardContent>
        {error && <div className={cn(ERROR_STRIP, "mb-3")}>{error}</div>}

        {rows.length === 0 ? (
          <p className="text-muted-foreground text-xs">暂无银行账户。</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>开户行</TableHead>
                <TableHead>支行</TableHead>
                <TableHead>账号</TableHead>
                <TableHead>账户名</TableHead>
                <TableHead>币种</TableHead>
                {/* 徽标用的是「默认」两个字，列头不重名，按文本查询才不会命中两个节点。 */}
                <TableHead>默认账户</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((account) => {
                const name = account.accountName ?? account.id;
                const isRevealed = revealed.includes(account.id);
                return (
                  <TableRow key={account.id}>
                    <TableCell>{account.bankName ?? MISSING}</TableCell>
                    <TableCell>{account.bankBranch ?? MISSING}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1">
                        <span className="font-mono">
                          {isRevealed
                            ? (account.bankAccount ?? MISSING)
                            : maskBankAccount(account.bankAccount)}
                        </span>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={
                            isRevealed ? `隐藏${name}的账号` : `显示${name}的账号`
                          }
                          onClick={() => toggleReveal(account.id)}
                        >
                          {isRevealed ? <EyeOff /> : <Eye />}
                        </Button>
                      </div>
                    </TableCell>
                    <TableCell>{account.accountName ?? MISSING}</TableCell>
                    <TableCell>{account.currency ?? MISSING}</TableCell>
                    <TableCell>
                      {account.isDefault ? (
                        <Badge variant="default">默认</Badge>
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
                          onClick={() => openEdit(account)}
                        >
                          <Pencil />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`设为默认${name}`}
                          disabled={account.isDefault === true}
                          onClick={() => void handleSetDefault(account)}
                        >
                          <Star />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`删除${name}`}
                          onClick={() => setDeleting(account)}
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
        <BankAccountFormDialog
          key={editing?.id ?? "new"}
          open={formOpen}
          supplierId={supplierId}
          account={editing}
          onOpenChange={setFormOpen}
        />
      )}

      <ConfirmDialog
        open={deleting !== null}
        title="删除银行账户"
        description={`确定要删除银行账户「${deleting?.accountName ?? MISSING}」吗？`}
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
