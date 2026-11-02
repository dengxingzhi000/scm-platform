import dayjs from "dayjs";

export type BadgeTone =
  | "default"
  | "secondary"
  | "success"
  | "warning"
  | "destructive"
  | "info";

export interface StatusMeta {
  label: string;
  tone: BadgeTone;
}

const fallback = (value: unknown): StatusMeta => ({
  label: value === null || value === undefined ? "—" : `状态 ${value}`,
  tone: "secondary",
});

export const memberStatusMap: Record<number, StatusMeta> = {
  1: { label: "正常", tone: "success" },
  0: { label: "停用", tone: "secondary" },
};

export const couponStatusMap: Record<number, StatusMeta> = {
  1: { label: "未使用", tone: "warning" },
  2: { label: "已使用", tone: "success" },
  3: { label: "已过期", tone: "secondary" },
};

export const groupBuyStatusMap: Record<number, StatusMeta> = {
  1: { label: "拼团中", tone: "warning" },
  2: { label: "已成团", tone: "success" },
  3: { label: "已失效", tone: "secondary" },
};

export const flashSaleStatusMap: Record<number, StatusMeta> = {
  1: { label: "进行中", tone: "success" },
  0: { label: "未开始", tone: "secondary" },
};

export const paymentStatusMap: Record<number, StatusMeta> = {
  1: { label: "待支付", tone: "warning" },
  2: { label: "已支付", tone: "success" },
  3: { label: "支付失败", tone: "destructive" },
  4: { label: "已取消", tone: "secondary" },
};

export const refundStatusMap: Record<number, StatusMeta> = {
  1: { label: "待审批", tone: "warning" },
  2: { label: "退款成功", tone: "success" },
  3: { label: "已驳回", tone: "destructive" },
};

export const orderCenterStatusMap: Record<number, StatusMeta> = {
  1: { label: "待支付", tone: "warning" },
  2: { label: "已支付", tone: "info" },
  3: { label: "备货中", tone: "info" },
  4: { label: "已发货", tone: "info" },
  5: { label: "已送达", tone: "info" },
  6: { label: "已取消", tone: "secondary" },
  7: { label: "已完成", tone: "success" },
};

export const fulfillmentStatusMap: Record<number, StatusMeta> = {
  1: { label: "待拣货", tone: "warning" },
  2: { label: "已拣货", tone: "info" },
  3: { label: "已打包", tone: "info" },
  4: { label: "已发货", tone: "info" },
  5: { label: "已签收", tone: "success" },
  6: { label: "已取消", tone: "destructive" },
};

export function statusMeta(
  map: Record<number, StatusMeta>,
  value: number | null | undefined
): StatusMeta {
  if (value === null || value === undefined) return fallback(value);
  return map[value] ?? fallback(value);
}

export function formatDateTime(value?: string | null): string {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm:ss") : "—";
}

export function formatDate(value?: string | null): string {
  return value ? dayjs(value).format("YYYY-MM-DD") : "—";
}

export function formatAmount(value?: number | string | null): string {
  if (value === null || value === undefined || value === "") return "¥0.00";
  return `¥${Number(value).toFixed(2)}`;
}

export function toNumberOrUndefined(value: string): number | undefined {
  if (!value.trim()) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}
