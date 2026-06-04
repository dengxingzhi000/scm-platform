import { clsx, type ClassValue } from "clsx";

export function cn(...inputs: ClassValue[]) {
  return clsx(inputs);
}

export function formatCurrency(amount: number, locale = "zh-CN") {
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency: "CNY",
  }).format(amount);
}

export function formatDate(date: string | Date, locale = "zh-CN") {
  return new Intl.DateTimeFormat(locale, {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(date));
}
