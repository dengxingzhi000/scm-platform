import dayjs from "dayjs";

export const TENANT_STATUS: Record<
  number,
  { label: string; variant: "success" | "warning" | "destructive" | "secondary" }
> = {
  0: { label: "试用中", variant: "warning" },
  1: { label: "正式", variant: "success" },
  2: { label: "已暂停", variant: "destructive" },
  3: { label: "已过期", variant: "secondary" },
};

export const TENANT_TYPE: Record<number, string> = {
  1: "企业版",
  2: "专业版",
  3: "个人版",
};

export const COMPANY_SIZE: Record<number, string> = {
  1: "<50 人",
  2: "50-200 人",
  3: "200-500 人",
  4: "500-2000 人",
  5: ">2000 人",
};

export const PACKAGE_LEVEL: Record<number, string> = {
  1: "基础版",
  2: "专业版",
  3: "企业版",
  4: "旗舰版",
};

export const SUBSCRIPTION_STATUS: Record<
  number,
  { label: string; variant: "success" | "warning" | "destructive" | "secondary" }
> = {
  0: { label: "待支付", variant: "warning" },
  1: { label: "生效中", variant: "success" },
  2: { label: "已过期", variant: "destructive" },
  3: { label: "已取消", variant: "secondary" },
};

export const SUBSCRIPTION_TYPE: Record<number, string> = {
  1: "月付",
  2: "年付",
  3: "一次性",
};

export const PAYMENT_STATUS: Record<number, string> = {
  0: "未支付",
  1: "部分支付",
  2: "已支付",
};

export const formatDate = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD") : "-";

export const formatDateTime = (value?: string | null): string =>
  value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";

export const formatMoney = (value?: number | null): string =>
  value === null || value === undefined ? "-" : `¥${value.toLocaleString()}`;

export const tenantStatus = (status?: number | null) =>
  TENANT_STATUS[status ?? 0] ?? TENANT_STATUS[0];

export const subscriptionStatus = (status?: number | null) =>
  SUBSCRIPTION_STATUS[status ?? 0] ?? SUBSCRIPTION_STATUS[0];
