import { getRequestConfig } from "next-intl/server";
import { routing } from "./routing";

const messageModules = [
  "common",
  "order",
  "inventory",
  "product",
] as const;

export default getRequestConfig(async ({ requestLocale }) => {
  let locale = await requestLocale;
  if (!locale || !routing.locales.includes(locale)) {
    locale = routing.defaultLocale;
  }

  const allMessages: Record<string, unknown> = {};

  for (const module of messageModules) {
    try {
      const mod = await import(`../../messages/${module}/${locale}.json`);
      Object.assign(allMessages, mod.default);
    } catch {
      // skip missing module
    }
  }

  return { locale, messages: allMessages };
});
