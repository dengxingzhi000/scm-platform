"use client";

import { useEffect, useMemo, useState } from "react";
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
import { Textarea } from "@/components/ui/textarea";
import {
  useBrandList,
  useCategoryList,
  useCreateProduct,
  useProductDetail,
  useUpdateProduct,
} from "../hooks";
import {
  productFormSchema,
  toProductFormValues,
  toProductPayload,
  type ProductFormValues,
} from "../lib/product-form-schema";

interface ProductFormProps {
  id?: string;
}

const defaultValues: ProductFormValues = {
  name: "",
  code: "",
  categoryId: "",
  brandId: undefined,
  price: "",
  status: "draft",
  description: "",
};

const NO_BRAND = "__none__";

export default function ProductForm({ id }: ProductFormProps) {
  const router = useRouter();
  const { data: product } = useProductDetail(id || "");
  const { data: categories } = useCategoryList();
  const { data: brands } = useBrandList();
  const createProduct = useCreateProduct();
  const updateProduct = useUpdateProduct();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const form = useForm<ProductFormValues>({
    resolver: zodResolver(productFormSchema),
    defaultValues,
    mode: "onBlur",
  });

  useEffect(() => {
    if (product) form.reset(toProductFormValues(product));
  }, [product, form]);

  const categoryOptions = useMemo(
    () =>
      categories?.flatMap((category) => [
        { label: category.name, value: category.id },
        ...(category.children ?? []).map((child) => ({
          label: `　${child.name}`,
          value: child.id,
        })),
      ]) ?? [],
    [categories],
  );

  const onSubmit = form.handleSubmit(async (values) => {
    setSubmitError(null);

    try {
      const payload = toProductPayload(values);
      if (id) {
        await updateProduct.mutateAsync({ id, data: payload });
      } else {
        await createProduct.mutateAsync(payload);
      }
      router.push("/product");
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : "保存失败");
    }
  });

  const isPending = createProduct.isPending || updateProduct.isPending;

  return (
    <Form {...form}>
      <form onSubmit={onSubmit} className="space-y-4">
        <div className="flex items-center justify-between gap-4">
          <h1 className="text-xl font-semibold">
            {id ? "编辑商品" : "新建商品"}
          </h1>
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push("/product")}
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
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>商品名称</FormLabel>
                  <FormControl>
                    <Input placeholder="例如：iPhone 15 Pro" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="code"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>商品编码</FormLabel>
                  <FormControl>
                    <Input placeholder="PRD-001" disabled={!!id} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="categoryId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>商品分类</FormLabel>
                  <Select
                    value={field.value}
                    onValueChange={field.onChange}
                  >
                    <FormControl>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="选择分类" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {categoryOptions.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                          {option.label}
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
              name="brandId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>品牌</FormLabel>
                  <Select
                    value={field.value ?? NO_BRAND}
                    onValueChange={(value) =>
                      field.onChange(value === NO_BRAND ? undefined : value)
                    }
                  >
                    <FormControl>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="选择品牌" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value={NO_BRAND}>不指定品牌</SelectItem>
                      {brands?.map((brand) => (
                        <SelectItem key={brand.id} value={brand.id}>
                          {brand.name}
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
              name="price"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>售价（元）</FormLabel>
                  <FormControl>
                    <Input
                      type="number"
                      min="0"
                      step="0.01"
                      placeholder="0.00"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="status"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>状态</FormLabel>
                  <Select
                    value={field.value}
                    onValueChange={field.onChange}
                  >
                    <FormControl>
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="选择状态" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="on_sale">在售</SelectItem>
                      <SelectItem value="off_sale">下架</SelectItem>
                      <SelectItem value="draft">草稿</SelectItem>
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem className="md:col-span-2">
                  <FormLabel>商品描述</FormLabel>
                  <FormControl>
                    <Textarea rows={4} placeholder="可选" {...field} />
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
