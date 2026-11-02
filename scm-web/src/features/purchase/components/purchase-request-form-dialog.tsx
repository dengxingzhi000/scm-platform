"use client";

import { useEffect, useMemo } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery } from "@tanstack/react-query";
import { Plus, Trash2 } from "lucide-react";
import { useFieldArray, useForm, useFormContext } from "react-hook-form";
import { z } from "zod";

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
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import type { DeptDTO } from "@/lib/api/contracts/system";
import type { PurRequestUpsertPayload } from "@/lib/api/contracts/purchase";
import { systemService } from "@/lib/api/services/system";

import {
  useCreateRequest,
  useRequestDetail,
  useUpdateRequest,
} from "../hooks";
import { toDateTimeLocal, toDateTimeParam } from "../lib/request-meta";

const itemSchema = z.object({
  materialName: z.string().min(1, "物料名称必填"),
  materialCode: z.string().optional(),
  materialSpec: z.string().optional(),
  quantity: z.string().refine((value) => Number(value) > 0, "数量需大于 0"),
  unit: z.string().min(1, "单位必填"),
  budgetPrice: z
    .string()
    .optional()
    .refine(
      (value) => !value || Number(value) >= 0,
      "预算单价不能为负数",
    ),
});

const requestSchema = z.object({
  requestType: z.string(),
  priority: z.string(),
  deptId: z.string().min(1, "请选择申请部门"),
  expectedDelivery: z.string().min(1, "请选择期望到货时间"),
  purpose: z.string().min(1, "请填写采购用途"),
  remark: z.string().optional(),
  items: z.array(itemSchema).min(1, "至少添加一条采购明细"),
});

type RequestItemValues = z.infer<typeof itemSchema>;
type RequestFormValues = z.infer<typeof requestSchema>;

const makeBlankItem = (): RequestItemValues => ({
  materialName: "",
  materialCode: "",
  materialSpec: "",
  quantity: "1",
  unit: "件",
  budgetPrice: "",
});

const makeBlankValues = (): RequestFormValues => ({
  requestType: "1",
  priority: "2",
  deptId: "",
  expectedDelivery: "",
  purpose: "",
  remark: "",
  items: [makeBlankItem()],
});

interface DeptOption {
  id: string;
  name: string;
  depth: number;
}

const flattenDepts = (nodes: DeptDTO[], depth = 0): DeptOption[] =>
  nodes.flatMap((node) => [
    { id: node.id, name: node.deptName, depth },
    ...flattenDepts(node.children ?? [], depth + 1),
  ]);

function RequestItemFields({
  index,
  onRemove,
  removable,
}: {
  index: number;
  onRemove: () => void;
  removable: boolean;
}) {
  const form = useFormContext<RequestFormValues>();
  const errors = form.formState.errors.items?.[index];

  return (
    <div className="rounded-lg border border-[var(--color-border-secondary)] p-3">
      <div className="mb-2 flex items-center justify-between">
        <span className="text-muted-foreground text-[11px] font-medium">
          明细 {index + 1}
        </span>
        {removable && (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="size-7"
            aria-label="删除明细"
            onClick={onRemove}
          >
            <Trash2 />
          </Button>
        )}
      </div>
      <div className="grid gap-3 md:grid-cols-12">
        <div className="md:col-span-4">
          <Label>物料名称</Label>
          <Input
            {...form.register(`items.${index}.materialName`)}
            placeholder="如：六角螺栓 M8"
          />
          {errors?.materialName && (
            <p className="text-destructive text-xs">{errors.materialName.message}</p>
          )}
        </div>
        <div className="md:col-span-3">
          <Label>物料编码</Label>
          <Input
            {...form.register(`items.${index}.materialCode`)}
            placeholder="可留空"
          />
        </div>
        <div className="md:col-span-3">
          <Label>规格型号</Label>
          <Input
            {...form.register(`items.${index}.materialSpec`)}
            placeholder="如：M8×40"
          />
        </div>
        <div className="md:col-span-2">
          <Label>单位</Label>
          <Input {...form.register(`items.${index}.unit`)} placeholder="件" />
          {errors?.unit && (
            <p className="text-destructive text-xs">{errors.unit.message}</p>
          )}
        </div>
        <div className="md:col-span-3">
          <Label>数量</Label>
          <Input
            type="number"
            min="0.001"
            step="0.001"
            {...form.register(`items.${index}.quantity`)}
          />
          {errors?.quantity && (
            <p className="text-destructive text-xs">{errors.quantity.message}</p>
          )}
        </div>
        <div className="md:col-span-3">
          <Label>预算单价</Label>
          <Input
            type="number"
            min="0"
            step="0.01"
            {...form.register(`items.${index}.budgetPrice`)}
            placeholder="0.00"
          />
          {errors?.budgetPrice && (
            <p className="text-destructive text-xs">
              {errors.budgetPrice.message}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  requestId?: string | null;
}

export function PurchaseRequestFormDialog({ open, onOpenChange, requestId }: Props) {
  const createRequest = useCreateRequest();
  const updateRequest = useUpdateRequest();

  const { data: detail, isError: detailError } = useRequestDetail(
    requestId ?? "",
    open && !!requestId,
  );

  const { data: depts = [] } = useQuery({
    queryKey: ["system", "depts", "tree"],
    queryFn: () => systemService.depts.tree(),
    enabled: open,
  });
  const deptOptions = useMemo(() => flattenDepts(depts), [depts]);

  const form = useForm<RequestFormValues>({
    resolver: zodResolver(requestSchema),
    defaultValues: makeBlankValues(),
  });
  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: "items",
  });

  useEffect(() => {
    if (!open) return;
    if (detail) {
      form.reset({
        requestType: String(detail.request.requestType ?? 1),
        priority: String(detail.request.priority ?? 2),
        deptId: detail.request.deptId ?? "",
        expectedDelivery: toDateTimeLocal(detail.request.expectedDelivery),
        purpose: detail.request.purpose ?? "",
        remark: detail.request.remark ?? "",
        items: detail.items.length
          ? detail.items.map((item) => ({
              materialName: item.materialName ?? "",
              materialCode: item.materialCode ?? "",
              materialSpec: item.materialSpec ?? "",
              quantity: item.quantity != null ? String(item.quantity) : "1",
              unit: item.unit ?? "件",
              budgetPrice:
                item.budgetPrice != null ? String(item.budgetPrice) : "",
            }))
          : [makeBlankItem()],
      });
    } else if (!requestId) {
      form.reset(makeBlankValues());
    }
  }, [open, detail, requestId, form]);

  const ready = !requestId || !!detail;
  const pending =
    createRequest.isPending || updateRequest.isPending || !ready;

  const onSubmit = form.handleSubmit(async (values) => {
    const dept = deptOptions.find((option) => option.id === values.deptId);
    const payload: PurRequestUpsertPayload = {
      request: {
        requestType: Number(values.requestType),
        priority: Number(values.priority),
        deptId: values.deptId,
        deptName: dept?.name,
        expectedDelivery: toDateTimeParam(values.expectedDelivery),
        purpose: values.purpose.trim(),
        remark: values.remark?.trim() || null,
      },
      items: values.items.map((item) => ({
        materialName: item.materialName.trim(),
        materialCode: item.materialCode?.trim() || null,
        materialSpec: item.materialSpec?.trim() || null,
        quantity: Number(item.quantity),
        unit: item.unit.trim(),
        budgetPrice: item.budgetPrice ? Number(item.budgetPrice) : null,
      })),
    };

    try {
      if (detail) {
        await updateRequest.mutateAsync({ id: detail.request.id, payload });
      } else {
        await createRequest.mutateAsync(payload);
      }
      onOpenChange(false);
    } catch {
      // Mutation hooks already surface the API error.
    }
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[88vh] overflow-y-auto sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>{detail ? "编辑采购申请" : "新建采购申请"}</DialogTitle>
          <DialogDescription>
            保存后为草稿状态；提交审批前可以随时修改申请与明细。
          </DialogDescription>
        </DialogHeader>

        {!ready ? (
          detailError ? (
            <div className="text-destructive py-10 text-center text-[13px]">
              采购申请加载失败，请关闭后重试
            </div>
          ) : (
            <div className="space-y-3">
              <Skeleton className="h-24 w-full rounded-lg" />
              <Skeleton className="h-40 w-full rounded-lg" />
            </div>
          )
        ) : (
        <Form {...form}>
          <form onSubmit={onSubmit} className="space-y-4">
            <div className="grid gap-3 md:grid-cols-2">
              <FormField
                control={form.control}
                name="requestType"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>申请类型</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger className="w-full">
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="1">常规采购</SelectItem>
                        <SelectItem value="2">紧急采购</SelectItem>
                        <SelectItem value="3">补货采购</SelectItem>
                        <SelectItem value="4">工程采购</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="priority"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>优先级</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger className="w-full">
                          <SelectValue />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="1">紧急</SelectItem>
                        <SelectItem value="2">普通</SelectItem>
                        <SelectItem value="3">低</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="deptId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>申请部门</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger className="w-full">
                          <SelectValue placeholder="请选择部门" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {deptOptions.map((dept) => (
                          <SelectItem key={dept.id} value={dept.id}>
                            {"　".repeat(dept.depth)}
                            {dept.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="expectedDelivery"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>期望到货时间</FormLabel>
                    <FormControl>
                      <Input type="datetime-local" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="purpose"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>采购用途</FormLabel>
                  <FormControl>
                    <Textarea
                      rows={2}
                      placeholder="说明采购背景与用途"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="remark"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>备注</FormLabel>
                  <FormControl>
                    <Input placeholder="可留空" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h4 className="text-[13px] font-semibold">采购明细</h4>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => append(makeBlankItem())}
                >
                  <Plus />
                  添加明细
                </Button>
              </div>
              {fields.map((field, index) => (
                <RequestItemFields
                  key={field.id}
                  index={index}
                  removable={fields.length > 1}
                  onRemove={() => remove(index)}
                />
              ))}
              {(form.formState.errors.items?.message ||
                form.formState.errors.items?.root?.message) && (
                <p className="text-destructive text-xs">
                  {form.formState.errors.items?.message ??
                    form.formState.errors.items?.root?.message}
                </p>
              )}
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
              >
                取消
              </Button>
              <Button type="submit" disabled={pending}>
                {pending ? "保存中..." : "保存草稿"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
        )}
      </DialogContent>
    </Dialog>
  );
}
