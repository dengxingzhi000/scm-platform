import { ReactNode } from "react";
import { NextIntlClientProvider } from "next-intl";
import { getMessages } from "next-intl/server";
import { AntdRegistry } from "@ant-design/cssinjs";
import { App } from "antd";
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
            <App>
              <QueryProvider>{children}</QueryProvider>
            </App>
          </AntdRegistry>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
