"use client";

import { useState } from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/button";
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
import {
  TableEmpty,
  TableLoading,
  TablePagination,
  TablePanel,
} from "@/components/ui/table-shell";
import { Textarea } from "@/components/ui/textarea";
import type { SupplierEvaluationDTO } from "@/lib/api/contracts/supplier";
import { supplierApiService } from "@/lib/api/services/supplier";
import { cn } from "@/lib/utils";
import {
  useCreateSupplierEvaluation,
  useDeleteSupplierEvaluation,
  useSupplierAverageScore,
  useSupplierEvaluations,
  useUpdateSupplierEvaluation,
} from "../hooks";
import { errorMessage } from "../lib/api-error";
import { formatDate, toSupplierEvaluation } from "../lib/mappers";
import type { SupplierEvaluation } from "../types";
import SupplierField from "./supplier-field";

interface SupplierEvaluationPanelProps {
  supplierId: string;
}

/** 与 `useSupplierEvaluations` 的默认页大小保持一致。 */
const EVALUATION_PAGE_SIZE = 20;

/** 缺失值的统一占位 —— 与其他分区一致。 */
const MISSING = "-";

/** 行内错误条。 */
const ERROR_STRIP =
  "border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm";

/** `<input type="date">` 的值 —— 后端给的是日期串，取前 10 位即可。 */
const toDateValue = (value?: string | null): string =>
  value ? value.slice(0, 10) : "";

/** 数字字段在表单里保持字符串，避免受控输入被空串/0 干扰。 */
const toNumberValue = (value?: number | null): string =>
  value === null || value === undefined ? "" : String(value);

interface EvaluationFormValues {
  evaluationPeriod: string;
  startDate: string;
  endDate: string;
  qualityScore: string;
  deliveryScore: string;
  serviceScore: string;
  priceScore: string;
  purchaseCount: string;
  totalAmount: string;
  onTimeDeliveryRate: string;
  qualityPassRate: string;
  evaluatorName: string;
  improvementSuggestions: string;
}

/** 只在打开弹窗时初始化一次表单，不把服务端数据同步进已挂载的表单 state。 */
const toFormValues = (
  evaluation: SupplierEvaluation | null,
): EvaluationFormValues => ({
  evaluationPeriod: evaluation?.evaluationPeriod ?? "",
  startDate: toDateValue(evaluation?.startDate),
  endDate: toDateValue(evaluation?.endDate),
  qualityScore: toNumberValue(evaluation?.qualityScore),
  deliveryScore: toNumberValue(evaluation?.deliveryScore),
  serviceScore: toNumberValue(evaluation?.serviceScore),
  priceScore: toNumberValue(evaluation?.priceScore),
  purchaseCount: toNumberValue(evaluation?.purchaseCount),
  totalAmount: toNumberValue(evaluation?.totalAmount),
  onTimeDeliveryRate: toNumberValue(evaluation?.onTimeDeliveryRate),
  qualityPassRate: toNumberValue(evaluation?.qualityPassRate),
  evaluatorName: evaluation?.evaluatorName ?? "",
  improvementSuggestions: evaluation?.improvementSuggestions ?? "",
});

/** 空数字落成 null，非空才转成数值。 */
const numberOrNull = (value: string): number | null => {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : Number(trimmed);
};

interface EvaluationFormDialogProps {
  open: boolean;
  supplierId: string;
  /** 传 null 表示新增。 */
  evaluation: SupplierEvaluation | null;
  onOpenChange: (open: boolean) => void;
}

/**
 * 表单弹窗。表单只在打开时挂载 —— `key` 由调用方按记录 id 指定，
 * 切换新增/编辑时会重新初始化 state。
 */
function EvaluationFormDialog({
  open,
  supplierId,
  evaluation,
  onOpenChange,
}: EvaluationFormDialogProps) {
  const create = useCreateSupplierEvaluation();
  const update = useUpdateSupplierEvaluation();

  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<EvaluationFormValues>(() =>
    toFormValues(evaluation),
  );

  const set = (key: keyof EvaluationFormValues, value: string) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const pending = create.isPending || update.isPending;
  // 后端对空评价周期返回 400「评价周期不能为空」。
  const canSubmit = form.evaluationPeriod.trim().length > 0;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setError(null);

    // 更新时同样回传空字符串而不是 null —— MyBatis-Plus 的 NOT_NULL 会把 null
    // 当成「保持原值」，只有 "" 才能真正清空评价人/改进建议。
    const data: SupplierEvaluationDTO = {
      evaluationPeriod: form.evaluationPeriod.trim(),
      startDate: form.startDate || null,
      endDate: form.endDate || null,
      qualityScore: numberOrNull(form.qualityScore),
      deliveryScore: numberOrNull(form.deliveryScore),
      serviceScore: numberOrNull(form.serviceScore),
      priceScore: numberOrNull(form.priceScore),
      purchaseCount: numberOrNull(form.purchaseCount),
      totalAmount: numberOrNull(form.totalAmount),
      onTimeDeliveryRate: numberOrNull(form.onTimeDeliveryRate),
      qualityPassRate: numberOrNull(form.qualityPassRate),
      evaluatorName: form.evaluatorName.trim(),
      improvementSuggestions: form.improvementSuggestions.trim(),
    };
    // 数字字段清空后只能落成 null，而后端把 null 视为「保持原值」——
    // 这是后端的更新语义限制，前端不填 0 伪造清空。
    // totalScore 由后端计算、evaluatedAt 由后端默认，两者都不回传。

    try {
      if (evaluation) {
        await update.mutateAsync({ id: evaluation.id, data });
      } else {
        await create.mutateAsync({ data: { supplierId, ...data } });
      }
      onOpenChange(false);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>
            {evaluation ? "编辑绩效评价" : "新增绩效评价"}
          </DialogTitle>
          <DialogDescription>
            评价周期为必填项；总评与评价时间由后端写入，前端不计算。
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-3 sm:grid-cols-2">
          <SupplierField label="评价周期" required htmlFor="evaluation-period">
            <Input
              id="evaluation-period"
              value={form.evaluationPeriod}
              onChange={(event) => set("evaluationPeriod", event.target.value)}
              placeholder="2026-09"
              className="font-mono"
            />
          </SupplierField>
          <SupplierField label="评价人" htmlFor="evaluation-evaluator">
            <Input
              id="evaluation-evaluator"
              value={form.evaluatorName}
              onChange={(event) => set("evaluatorName", event.target.value)}
              placeholder="例如：王五"
            />
          </SupplierField>
          <SupplierField label="周期开始" htmlFor="evaluation-start">
            <Input
              id="evaluation-start"
              type="date"
              value={form.startDate}
              onChange={(event) => set("startDate", event.target.value)}
            />
          </SupplierField>
          <SupplierField label="周期结束" htmlFor="evaluation-end">
            <Input
              id="evaluation-end"
              type="date"
              value={form.endDate}
              onChange={(event) => set("endDate", event.target.value)}
            />
          </SupplierField>
          <SupplierField label="质量得分" htmlFor="evaluation-quality">
            <Input
              id="evaluation-quality"
              type="number"
              min={0}
              max={100}
              value={form.qualityScore}
              onChange={(event) => set("qualityScore", event.target.value)}
            />
          </SupplierField>
          <SupplierField label="交付得分" htmlFor="evaluation-delivery">
            <Input
              id="evaluation-delivery"
              type="number"
              min={0}
              max={100}
              value={form.deliveryScore}
              onChange={(event) => set("deliveryScore", event.target.value)}
            />
          </SupplierField>
          <SupplierField label="服务得分" htmlFor="evaluation-service">
            <Input
              id="evaluation-service"
              type="number"
              min={0}
              max={100}
              value={form.serviceScore}
              onChange={(event) => set("serviceScore", event.target.value)}
            />
          </SupplierField>
          <SupplierField label="价格得分" htmlFor="evaluation-price">
            <Input
              id="evaluation-price"
              type="number"
              min={0}
              max={100}
              value={form.priceScore}
              onChange={(event) => set("priceScore", event.target.value)}
            />
          </SupplierField>
          <SupplierField label="采购次数" htmlFor="evaluation-purchase-count">
            <Input
              id="evaluation-purchase-count"
              type="number"
              min={0}
              value={form.purchaseCount}
              onChange={(event) => set("purchaseCount", event.target.value)}
            />
          </SupplierField>
          <SupplierField label="总金额" htmlFor="evaluation-total-amount">
            <Input
              id="evaluation-total-amount"
              type="number"
              min={0}
              value={form.totalAmount}
              onChange={(event) => set("totalAmount", event.target.value)}
            />
          </SupplierField>
          <SupplierField label="准时交付率" htmlFor="evaluation-on-time">
            <Input
              id="evaluation-on-time"
              type="number"
              min={0}
              max={100}
              value={form.onTimeDeliveryRate}
              onChange={(event) =>
                set("onTimeDeliveryRate", event.target.value)
              }
            />
          </SupplierField>
          <SupplierField label="质量合格率" htmlFor="evaluation-quality-rate">
            <Input
              id="evaluation-quality-rate"
              type="number"
              min={0}
              max={100}
              value={form.qualityPassRate}
              onChange={(event) => set("qualityPassRate", event.target.value)}
            />
          </SupplierField>
          <SupplierField
            label="改进建议"
            htmlFor="evaluation-suggestions"
            className="sm:col-span-2"
          >
            <Textarea
              id="evaluation-suggestions"
              rows={3}
              value={form.improvementSuggestions}
              onChange={(event) =>
                set("improvementSuggestions", event.target.value)
              }
              placeholder="可填写下一周期的改进要求"
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

export default function SupplierEvaluationPanel({
  supplierId,
}: SupplierEvaluationPanelProps) {
  const [page, setPage] = useState(1);
  const { data, isLoading } = useSupplierEvaluations(supplierId, page);
  const { data: averageScore } = useSupplierAverageScore(supplierId);
  const remove = useDeleteSupplierEvaluation();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<SupplierEvaluation | null>(null);
  const [deleting, setDeleting] = useState<SupplierEvaluation | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loadingDetailId, setLoadingDetailId] = useState<string | null>(null);

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageSize = data?.pageSize || EVALUATION_PAGE_SIZE;
  const pageCount = Math.max(1, Math.ceil(total / pageSize));

  const openCreate = () => {
    setEditing(null);
    setError(null);
    setFormOpen(true);
  };

  /**
   * 列表行不保证带全部字段，编辑前先取详情。失败时保持弹窗关闭，
   * 把后端原因放在分区内的错误条里。
   */
  const openEdit = async (row: SupplierEvaluation) => {
    setError(null);
    setLoadingDetailId(row.id);
    try {
      const dto = await supplierApiService.evaluations.getById(row.id);
      setEditing(toSupplierEvaluation(dto));
      setFormOpen(true);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoadingDetailId(null);
    }
  };

  const handleDelete = async () => {
    if (!deleting) return;
    setError(null);
    try {
      await remove.mutateAsync({ id: deleting.id });
      setDeleting(null);
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <TablePanel>
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border-secondary)] px-3 py-2">
        <div>
          <div className="text-[13px] font-semibold">供应商绩效评价</div>
          <div className="text-muted-foreground text-[11px]">
            平均分 {averageScore ?? MISSING} · 总评以服务端回写为准
          </div>
        </div>
        <Button onClick={openCreate}>
          <Plus />
          新增评价
        </Button>
      </div>

      {error && <div className={cn(ERROR_STRIP, "m-3")}>{error}</div>}

      {isLoading ? (
        <TableLoading />
      ) : rows.length === 0 ? (
        <TableEmpty>暂无绩效评价。</TableEmpty>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>评价周期</TableHead>
              <TableHead numeric>质量</TableHead>
              <TableHead numeric>交付</TableHead>
              <TableHead numeric>服务</TableHead>
              <TableHead numeric>价格</TableHead>
              <TableHead numeric>总评</TableHead>
              <TableHead numeric>采购次数</TableHead>
              <TableHead numeric>准时交付率</TableHead>
              <TableHead numeric>质量合格率</TableHead>
              <TableHead>评价人</TableHead>
              <TableHead>评价时间</TableHead>
              <TableHead className="text-right">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((row) => {
              const period = row.evaluationPeriod ?? row.id;
              return (
                <TableRow key={row.id}>
                  <TableCell className="font-mono">{period}</TableCell>
                  <TableCell numeric>{row.qualityScore ?? MISSING}</TableCell>
                  <TableCell numeric>{row.deliveryScore ?? MISSING}</TableCell>
                  <TableCell numeric>{row.serviceScore ?? MISSING}</TableCell>
                  <TableCell numeric>{row.priceScore ?? MISSING}</TableCell>
                  <TableCell numeric>{row.totalScore ?? MISSING}</TableCell>
                  <TableCell numeric>{row.purchaseCount ?? MISSING}</TableCell>
                  <TableCell numeric>
                    {row.onTimeDeliveryRate ?? MISSING}
                  </TableCell>
                  <TableCell numeric>
                    {row.qualityPassRate ?? MISSING}
                  </TableCell>
                  <TableCell>{row.evaluatorName ?? MISSING}</TableCell>
                  <TableCell className="font-mono">
                    {formatDate(row.evaluatedAt)}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label={`编辑${period}`}
                        disabled={loadingDetailId === row.id}
                        onClick={() => void openEdit(row)}
                      >
                        <Pencil />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label={`删除${period}`}
                        onClick={() => setDeleting(row)}
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

      <TablePagination
        page={page}
        pageSize={pageSize}
        total={total}
        pageCount={pageCount}
        onPageChange={setPage}
      />

      {formOpen && (
        <EvaluationFormDialog
          key={editing?.id ?? "new"}
          open={formOpen}
          supplierId={supplierId}
          evaluation={editing}
          onOpenChange={setFormOpen}
        />
      )}

      <ConfirmDialog
        open={deleting !== null}
        title="删除绩效评价"
        description={`确定要删除「${deleting?.evaluationPeriod ?? MISSING}」的绩效评价吗？`}
        confirmText="删除"
        destructive
        pending={remove.isPending}
        onConfirm={() => void handleDelete()}
        onOpenChange={(open) => {
          if (!open) setDeleting(null);
        }}
      />
    </TablePanel>
  );
}
