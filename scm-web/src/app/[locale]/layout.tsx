import { ReactNode } from "react";
import { NextIntlClientProvider } from "next-intl";
import { getMessages } from "next-intl/server";
import { AntdRegistry } from "@ant-design/nextjs-registry";
import ThemeProvider from "@/providers/theme-provider";
import QueryProvider from "@/components/providers/QueryProvider";
import LocaleAttributes from "@/components/providers/locale-attributes";

type Props = {
  children: ReactNode;
  params: Promise<{ locale: string }>;
};

export default async function LocaleLayout({ children, params }: Props) {
  const { locale } = await params;
  const messages = await getMessages();

  return (
    <NextIntlClientProvider messages={messages}>
      <LocaleAttributes locale={locale} />
      <AntdRegistry>
        <ThemeProvider>
          <QueryProvider>{children}</QueryProvider>
        </ThemeProvider>
      </AntdRegistry>
    </NextIntlClientProvider>
  );
}
