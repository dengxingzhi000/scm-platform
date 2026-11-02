"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  AUDIT_STATUSES,
  COOPERATION_STATUSES,
  RISK_LEVELS,
  SUPPLIER_LEVELS,
  SUPPLIER_TYPES,
} from "../lib/constants";
import type { SupplierCategory, SupplierSearchParams } from "../types";

/** "全部" 的占位值 —— 与后端枚举值不会冲突。 */
const ALL = "__all__";

interface SupplierFiltersProps {
  value: SupplierSearchParams;
  categories: SupplierCategory[];
  onChange: (patch: Partial<SupplierSearchParams>) => void;
}

interface FilterOption {
  value: string;
  label: string;
}

interface FilterSelectProps {
  label: string;
  value: string;
  options: FilterOption[];
  onChange: (value: string) => void;
}

/** 一个带字段名的下拉 —— trigger 里先放字段名，再放当前值。 */
function FilterSelect({ label, value, options, onChange }: FilterSelectProps) {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger size="sm" className="w-32 text-xs">
        <span className="text-muted-foreground">{label}</span>
        <SelectValue placeholder="全部" />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value={ALL}>全部</SelectItem>
        {options.map((option) => (
          <SelectItem key={option.value} value={option.value}>
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

export default function SupplierFilters({
  value,
  categories,
  onChange,
}: SupplierFiltersProps) {
  // 关键字是「提交才生效」的：草稿与已生效的值分开存。
  const [keywordDraft, setKeywordDraft] = useState(value.keyword ?? "");
  const [syncedKeyword, setSyncedKeyword] = useState(value.keyword);

  // 外部改了关键字（清空筛选等）时同步草稿 —— 渲染期调整，不用 effect。
  if (syncedKeyword !== value.keyword) {
    setSyncedKeyword(value.keyword);
    setKeywordDraft(value.keyword ?? "");
  }

  const categoryOptions = useMemo<FilterOption[]>(
    () =>
      [...categories]
        .sort(
          (a, b) =>
            (a.sortOrder ?? 0) - (b.sortOrder ?? 0) ||
            (a.categoryName ?? "").localeCompare(b.categoryName ?? ""),
        )
        .map((category) => ({
          value: category.id,
          // 子分类用全角空格缩进，与商品分类的下拉写法一致。
          label: `${category.parentId ? "　" : ""}${category.categoryName ?? "-"}`,
        })),
    [categories],
  );

  return (
    <div className="flex flex-wrap items-center gap-2">
      <form
        onSubmit={(event) => {
          event.preventDefault();
          onChange({ keyword: keywordDraft.trim() || undefined });
        }}
        className="flex items-center gap-2"
      >
        <Input
          value={keywordDraft}
          onChange={(event) => setKeywordDraft(event.target.value)}
          placeholder="搜索供应商名称 / 编码"
          className="h-8 w-[230px] text-xs"
        />
        <Button
          type="submit"
          variant="outline"
          size="icon"
          className="size-8"
          aria-label="搜索"
        >
          <Search />
        </Button>
      </form>

      <FilterSelect
        label="供应商类型"
        value={value.supplierType === undefined ? ALL : String(value.supplierType)}
        options={SUPPLIER_TYPES.map((item) => ({
          value: String(item.value),
          label: item.label,
        }))}
        onChange={(next) =>
          onChange({
            supplierType: next === ALL ? undefined : Number(next),
          })
        }
      />

      <FilterSelect
        label="合作状态"
        value={
          value.cooperationStatus === undefined
            ? ALL
            : String(value.cooperationStatus)
        }
        options={COOPERATION_STATUSES.map((item) => ({
          value: String(item.value),
          label: item.label,
        }))}
        onChange={(next) =>
          onChange({
            cooperationStatus: next === ALL ? undefined : Number(next),
          })
        }
      />

      <FilterSelect
        label="准入状态"
        value={value.auditStatus === undefined ? ALL : String(value.auditStatus)}
        options={AUDIT_STATUSES.map((item) => ({
          value: String(item.value),
          label: item.label,
        }))}
        onChange={(next) =>
          onChange({ auditStatus: next === ALL ? undefined : Number(next) })
        }
      />

      <FilterSelect
        label="分级"
        value={value.supplierLevel === undefined ? ALL : String(value.supplierLevel)}
        options={SUPPLIER_LEVELS.map((item) => ({
          value: String(item.value),
          label: item.label,
        }))}
        onChange={(next) =>
          onChange({ supplierLevel: next === ALL ? undefined : Number(next) })
        }
      />

      <FilterSelect
        label="风险等级"
        value={value.riskLevel === undefined ? ALL : String(value.riskLevel)}
        options={RISK_LEVELS.map((item) => ({
          value: String(item.value),
          label: item.label,
        }))}
        onChange={(next) =>
          onChange({ riskLevel: next === ALL ? undefined : Number(next) })
        }
      />

      <FilterSelect
        label="黑名单"
        value={value.blacklisted === undefined ? ALL : String(value.blacklisted)}
        options={[
          { value: "true", label: "在黑名单中" },
          { value: "false", label: "不在黑名单" },
        ]}
        onChange={(next) =>
          onChange({ blacklisted: next === ALL ? undefined : next === "true" })
        }
      />

      <FilterSelect
        label="分类"
        value={value.categoryId ?? ALL}
        options={categoryOptions}
        onChange={(next) =>
          onChange({ categoryId: next === ALL ? undefined : next })
        }
      />
    </div>
  );
}
