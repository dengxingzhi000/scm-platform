import { ReactNode } from "react";
import { NextIntlClientProvider } from "next-intl";
import { getMessages } from "next-intl/server";
import { AntdRegistry } from "@ant-design/cssinjs";
import { App, ConfigProvider, theme as antTheme } from "antd";
import QueryProvider from "@/components/providers/QueryProvider";
import { ErrorBoundary } from "@/components/error/ErrorBoundary";

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
            <ConfigProvider
              theme={{
                token: { colorPrimary: "#1677ff" },
              }}
            >
              <App>
                <QueryProvider>
                  <ErrorBoundary>{children}</ErrorBoundary>
                </QueryProvider>
              </App>
            </ConfigProvider>
          </AntdRegistry>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
