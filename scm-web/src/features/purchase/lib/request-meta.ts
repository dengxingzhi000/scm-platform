import dayjs from "dayjs";

export type BadgeTone =
  | "secondary"
  | "info"
  | "warning"
  | "success"
  | "destructive";

export const REQUEST_STATUS: Record<number, { label: string; tone: BadgeTone }> = {
  0: { label: "草稿", tone: "secondary" },
  1: { label: "待审批", tone: "warning" },
  2: { label: "已审批", tone: "info" },
  3: { label: "已驳回", tone: "destructive" },
  4: { label: "已转采购单", tone: "success" },
  5: { label: "已关闭", tone: "secondary" },
};

export const REQUEST_TYPES: Record<number, string> = {
  1: "常规采购",
  2: "紧急采购",
  3: "补货采购",
  4: "工程采购",
};

export const PRIORITIES: Record<number, string> = {
  1: "紧急",
  2: "普通",
  3: "低",
};

export const requestStatusMeta = (status?: number | null) =>
  REQUEST_STATUS[status ?? 0] ?? REQUEST_STATUS[0];

export const formatDateTime = (value?: string | null) =>
  value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";

export const toDateTimeLocal = (value?: string | null) =>
  value ? dayjs(value).format("YYYY-MM-DDTHH:mm") : "";

export const toDateTimeParam = (value: string) => {
  if (!value) return "";
  return value.length === 16 ? `${value}:00` : value;
};

export const formatMoney = (value?: number | null) =>
  `¥${(value ?? 0).toLocaleString()}`;
