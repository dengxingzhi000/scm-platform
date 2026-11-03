"use client";

import { usePathname } from "next/navigation";
import { SupplierForm } from "@/features/supplier/components";

export default function SupplierEditPage() {
  const pathname = usePathname();
  // 路径是 /{locale}/supplier/{id}/edit，最后一段是 "edit"，id 在倒数第二段。
  // 不能照抄详情页的 split("/").pop() —— 那会拿到 "edit"。
  const segments = pathname.split("/").filter(Boolean);
  const id = segments[segments.length - 2] ?? "";
  return <SupplierForm id={id} />;
}
