"use client";

import type { ReactNode } from "react";

import { Label } from "@/components/ui/label";

interface SupplierFieldProps {
  label: string;
  required?: boolean;
  htmlFor: string;
  className?: string;
  children: ReactNode;
}

/**
 * 表单字段的外层包装：标签 + 必填星号 + 内容。
 * 三个详情页分区共用一份 —— `className` 用来跨列（如 `sm:col-span-2`）。
 */
export default function SupplierField({
  label,
  required,
  htmlFor,
  className,
  children,
}: SupplierFieldProps) {
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
