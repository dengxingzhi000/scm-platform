/**
 * 剩余天数的着色：已过期（<= 0）与一周内到期（1..7）都是红色，
 * 一个月内（8..30）是琥珀色，更远的是中性色。
 *
 * 详情页的证照表与准入审核工作台的到期预警表共用这一份阈值。
 */
export const expiryDaysClass = (days: number | null): string => {
  if (days === null) return "text-muted-foreground";
  if (days <= 7) return "text-[var(--color-error-text)]";
  if (days <= 30) return "text-[var(--color-warning-text)]";
  return "text-muted-foreground";
};
