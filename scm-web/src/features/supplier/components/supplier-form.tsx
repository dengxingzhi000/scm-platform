"use client";

import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";

import { SUPPLIER_TYPES } from "../lib/constants";
import { errorMessage } from "../lib/api-error";
import {
  CREDIT_RATINGS,
  supplierFormDefaults,
  supplierFormSchema,
  toSupplierFormValues,
  toSupplierPayload,
  type SupplierFormValues,
} from "../lib/supplier-form-schema";
import {
  useCreateSupplier,
  useSupplierFormDetail,
  useUpdateSupplier,
} from "../hooks";

interface SupplierFormProps {
  id?: string;
}

/**
 * Radix 不允许 `<SelectItem value="">`（空串 value 会直接抛错），
 * 「未选择」用这个哨兵表示，`onValueChange` 时再映射回空串。
 */
const NO_OPTION = "__none__";

export default function SupplierForm({ id }: SupplierFormProps) {
  const router = useRouter();
  const isEdit = !!id;

  // 新建模式传空串，查询保持禁用，不发请求。
  const { data: dto, isLoading, isError, error } = useSupplierFormDetail(id ?? "");
  const createSupplier = useCreateSupplier();
  const updateSupplier = useUpdateSupplier();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const form = useForm<SupplierFormValues>({
    resolver: zodResolver(supplierFormSchema),
    defaultValues: supplierFormDefaults,
    mode: "onBlur",
  });

  useEffect(() => {
    if (dto) form.reset(toSupplierFormValues(dto));
  }, [dto, form]);

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitError(null);

    try {
      const payload = toSupplierPayload(values);
      if (id) {
        await updateSupplier.mutateAsync({ id, data: payload });
      } else {
        await createSupplier.mutateAsync(payload);
      }
      router.push("/supplier");
    } catch (error) {
      setSubmitError(errorMessage(error) || "保存失败");
    }
  });

  if (isEdit && isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-9 w-48" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (isEdit && isError) {
    return (
      <div className="space-y-4">
        <p className="border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm">
          {errorMessage(error) || "加载供应商失败"}
        </p>
        <Button variant="outline" onClick={() => router.push("/supplier")}>
          返回列表
        </Button>
      </div>
    );
  }

  const isPending = createSupplier.isPending || updateSupplier.isPending;
  const title = isEdit ? "编辑供应商" : "新建供应商";
  // Radix Select 受控值不能是空串 —— 空值时挂哨兵。
  const supplierTypeValue = form.watch("supplierType") || NO_OPTION;
  const currentCreditRating = form.watch("creditRating");
  const creditRatingValue = currentCreditRating || NO_OPTION;
  // 编辑模式下已有信用等级时不提供「未评级」：后端 CHECK 约束拒绝空串、
  // null 被 MyBatis-Plus 的 NOT_NULL 跳过，这个 API 清不掉信用等级。
  const creditRatingOptions = currentCreditRating ? [...CREDIT_RATINGS] : ["", ...CREDIT_RATINGS];

  return (
    <Form {...form}>
      <form onSubmit={onSubmit} className="space-y-4">
        <div className="flex items-center justify-between gap-4">
          <h1 className="text-xl font-semibold">{title}</h1>
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push("/supplier")}
            >
              取消
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending && <Loader2 className="size-4 animate-spin" />}
              保存
            </Button>
          </div>
        </div>

        {submitError && (
          <p className="border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm">
            {submitError}
          </p>
        )}

        <Card>
          <CardHeader>
            <CardTitle>基本信息</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <FormField
              control={form.control}
              name="supplierCode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>供应商编码</FormLabel>
                  <FormControl>
                    <Input placeholder="留空自动生成" disabled={isEdit} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="supplierName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>供应商名称</FormLabel>
                  <FormControl>
                    <Input placeholder="例如：深圳华强电子有限公司" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="supplierNameEn"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>英文名称</FormLabel>
                  <FormControl>
                    <Input placeholder="例如：Shenzhen Huaqiang Electronics" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="supplierType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>供应商类型</FormLabel>
                  <Select
                    value={supplierTypeValue}
                    onValueChange={(value) =>
                      field.onChange(value === NO_OPTION ? "" : value)
                    }
                  >
                    <FormControl>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="选择供应商类型" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value={NO_OPTION}>未选择</SelectItem>
                      {SUPPLIER_TYPES.map((type) => (
                        <SelectItem key={type.value} value={String(type.value)}>
                          {type.label}
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
              name="businessType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>业务类型</FormLabel>
                  <FormControl>
                    <Input placeholder="例如：电子元器件" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="legalPerson"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>法人</FormLabel>
                  <FormControl>
                    <Input placeholder="法人姓名" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="registeredCapital"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>注册资本（元）</FormLabel>
                  <FormControl>
                    <Input type="number" min="0" placeholder="例如：5000000" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="registrationNo"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>统一社会信用代码</FormLabel>
                  <FormControl>
                    <Input placeholder="18 位统一社会信用代码" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="taxNo"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>税号</FormLabel>
                  <FormControl>
                    <Input placeholder="纳税人识别号" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="establishmentDate"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>成立日期</FormLabel>
                  <FormControl>
                    <Input type="date" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>联系信息</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <FormField
              control={form.control}
              name="contactName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>联系人</FormLabel>
                  <FormControl>
                    <Input placeholder="联系人姓名" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="contactPhone"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>联系电话</FormLabel>
                  <FormControl>
                    <Input placeholder="例如：138****2210" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="contactEmail"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>联系邮箱</FormLabel>
                  <FormControl>
                    <Input type="email" placeholder="例如：contact@example.com" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="contactAddress"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>联系地址</FormLabel>
                  <FormControl>
                    <Input placeholder="详细地址" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>结算信息</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <FormField
              control={form.control}
              name="bankName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>开户行</FormLabel>
                  <FormControl>
                    <Input placeholder="开户银行名称" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="bankAccount"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>银行账号</FormLabel>
                  <FormControl>
                    <Input placeholder="对公银行账号" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="bankAccountName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>账户名称</FormLabel>
                  <FormControl>
                    <Input placeholder="银行开户名" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="creditRating"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>信用等级</FormLabel>
                  <Select
                    value={creditRatingValue}
                    onValueChange={(value) =>
                      field.onChange(value === NO_OPTION ? "" : value)
                    }
                  >
                    <FormControl>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="选择信用等级" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {creditRatingOptions.map((rating) =>
                        rating === "" ? (
                          <SelectItem key={NO_OPTION} value={NO_OPTION}>
                            未评级
                          </SelectItem>
                        ) : (
                          <SelectItem key={rating} value={rating}>
                            {rating}
                          </SelectItem>
                        ),
                      )}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="paymentTerms"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>付款条件</FormLabel>
                  <FormControl>
                    <Input placeholder="例如：月结 30 天" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="paymentMethod"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>付款方式</FormLabel>
                  <FormControl>
                    <Input placeholder="例如：银行转账" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>其他</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <FormField
              control={form.control}
              name="businessLicense"
              render={({ field }) => (
                <FormItem className="md:col-span-2">
                  <FormLabel>营业执照</FormLabel>
                  <FormControl>
                    <Textarea rows={3} placeholder="营业执照编号或描述" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="certificates"
              render={({ field }) => (
                <FormItem className="md:col-span-2">
                  <FormLabel>资质证照</FormLabel>
                  <FormControl>
                    <Textarea rows={3} placeholder="资质证照描述" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="website"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>官网地址</FormLabel>
                  <FormControl>
                    <Input placeholder="https://" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="sortOrder"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>排序</FormLabel>
                  <FormControl>
                    <Input type="number" placeholder="例如：100" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="remark"
              render={({ field }) => (
                <FormItem className="md:col-span-2">
                  <FormLabel>备注</FormLabel>
                  <FormControl>
                    <Textarea rows={3} placeholder="备注信息" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>
      </form>
    </Form>
  );
}
