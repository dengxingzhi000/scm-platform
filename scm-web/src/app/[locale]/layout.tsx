import { ReactNode } from "react";
import { NextIntlClientProvider } from "next-intl";
import { getMessages } from "next-intl/server";
import { AntdRegistry } from "@ant-design/nextjs-registry";
import ThemeProvider from "@/providers/theme-provider";
import QueryProvider from "@/components/providers/QueryProvider";

type Props = {
  children: ReactNode;
  params: Promise<{ locale: string }>;
};

export default async function LocaleLayout({ children, params }: Props) {
  const { locale } = await params;
  const messages = await getMessages();

  return (
    <html lang={locale}>
      <body>
        <NextIntlClientProvider messages={messages}>
          <AntdRegistry>
            <ThemeProvider>
              <QueryProvider>{children}</QueryProvider>
            </ThemeProvider>
          </AntdRegistry>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
