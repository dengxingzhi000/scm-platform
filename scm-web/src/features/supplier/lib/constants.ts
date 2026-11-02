/**
 * 供应商模块的枚举字典 —— 列表徽标、筛选下拉、表单下拉共用的唯一来源。
 *
 * 每个条目的 `value` 与后端 `sup_supplier_*` 表里的整型/字符串枚举一一对应，
 * `variant` 与 `src/components/ui/badge.tsx` 的 `badgeVariants` 取值保持一致。
 */

/**
 * `components/ui/badge.tsx` 没有导出这个联合类型，这里按它的 `badgeVariants`
 * 取值手工同步一份。
 */
export type BadgeVariant =
  | "default"
  | "secondary"
  | "destructive"
  | "outline"
  | "success"
  | "warning"
  | "info";

/** 整型枚举字典（分级 / 准入状态 / 风险等级 / 合作状态 / 证照状态 / 供应商类型）。 */
export type NumericEnumDictionary = ReadonlyArray<{
  value: number;
  label: string;
  variant: BadgeVariant;
}>;

/** 字符串枚举字典（证照类型 / 联系角色）。 */
export type TextEnumDictionary = ReadonlyArray<{
  value: string;
  label: string;
  variant: BadgeVariant;
}>;

/** 供应商分级：1-战略，2-优选，3-合格，4-备选。 */
export const SUPPLIER_LEVELS: NumericEnumDictionary = [
  { value: 1, label: "战略", variant: "default" },
  { value: 2, label: "优选", variant: "success" },
  { value: 3, label: "合格", variant: "secondary" },
  { value: 4, label: "备选", variant: "secondary" },
];

/** 准入状态：0-草稿，1-待审核，2-已通过，3-已驳回。 */
export const AUDIT_STATUSES: NumericEnumDictionary = [
  { value: 0, label: "草稿", variant: "secondary" },
  { value: 1, label: "待审核", variant: "warning" },
  { value: 2, label: "已通过", variant: "success" },
  { value: 3, label: "已驳回", variant: "destructive" },
];

/** 风险等级：1-低，2-中，3-高。 */
export const RISK_LEVELS: NumericEnumDictionary = [
  { value: 1, label: "低", variant: "secondary" },
  { value: 2, label: "中", variant: "warning" },
  { value: 3, label: "高", variant: "destructive" },
];

/** 合作状态：0-潜在，1-合作中，2-暂停，3-终止。 */
export const COOPERATION_STATUSES: NumericEnumDictionary = [
  { value: 0, label: "潜在", variant: "secondary" },
  { value: 1, label: "合作中", variant: "default" },
  { value: 2, label: "暂停", variant: "warning" },
  { value: 3, label: "终止", variant: "destructive" },
];

/** 证照类型。 */
export const CERTIFICATE_TYPES: TextEnumDictionary = [
  { value: "BUSINESS_LICENSE", label: "营业执照", variant: "secondary" },
  { value: "TAX_REGISTRATION", label: "税务登记", variant: "secondary" },
  { value: "QUALITY_ISO", label: "质量体系", variant: "secondary" },
  { value: "PRODUCTION_LICENSE", label: "生产许可", variant: "secondary" },
  { value: "AGENT_AUTHORIZATION", label: "代理授权", variant: "secondary" },
  { value: "OTHER", label: "其他", variant: "secondary" },
];

/** 证照状态：0-待审核，1-有效，2-即将到期，3-已过期，4-已驳回。 */
export const CERTIFICATE_STATUSES: NumericEnumDictionary = [
  { value: 0, label: "待审核", variant: "secondary" },
  { value: 1, label: "有效", variant: "success" },
  { value: 2, label: "即将到期", variant: "warning" },
  { value: 3, label: "已过期", variant: "destructive" },
  { value: 4, label: "已驳回", variant: "destructive" },
];

/** 联系人角色。 */
export const CONTACT_ROLES: TextEnumDictionary = [
  { value: "SALES", label: "销售", variant: "secondary" },
  { value: "FINANCE", label: "财务", variant: "secondary" },
  { value: "LOGISTICS", label: "物流", variant: "secondary" },
  { value: "TECH", label: "技术", variant: "secondary" },
  { value: "OTHER", label: "其他", variant: "secondary" },
];

/** 供应商类型：1-生产型，2-贸易型，3-服务型，4-综合型。 */
export const SUPPLIER_TYPES: NumericEnumDictionary = [
  { value: 1, label: "生产型", variant: "secondary" },
  { value: 2, label: "贸易型", variant: "secondary" },
  { value: 3, label: "服务型", variant: "secondary" },
  { value: 4, label: "综合型", variant: "secondary" },
];

/** 变更留痕的变更类型 —— 徽标上直接显示中文。 */
export const CHANGE_TYPE_LABELS: Record<string, string> = {
  CREATE: "创建",
  UPDATE: "更新",
  DELETE: "删除",
  SUBMIT_AUDIT: "提交审核",
  AUDIT_APPROVE: "审核通过",
  AUDIT_REJECT: "审核驳回",
  ENABLE: "启用",
  DISABLE: "禁用",
  BLACKLIST: "列入黑名单",
  UNBLACKLIST: "移出黑名单",
  RISK_LEVEL: "风险等级变更",
  COOPERATION_STATUS: "合作状态变更",
  CATEGORY_ASSIGN: "分配分类",
};

/** 查不到对应条目时返回 "-"，避免徽标渲染成空白。 */
export const labelOf = <T extends { value: number | string; label: string }>(
  dictionary: ReadonlyArray<T>,
  value?: number | string | null
): string => dictionary.find((item) => item.value === value)?.label ?? "-";

/** 查不到对应条目时返回 "secondary"（中性色）。 */
export const variantOf = <T extends { value: number | string; variant: BadgeVariant }>(
  dictionary: ReadonlyArray<T>,
  value?: number | string | null
): BadgeVariant =>
  dictionary.find((item) => item.value === value)?.variant ?? "secondary";
