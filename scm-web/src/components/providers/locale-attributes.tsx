"use client";

import { useEffect } from "react";

/**
 * The root layout owns `<html>` (required by Next.js), so locale-specific
 * attributes are applied here instead of rendering a second nested `<html>`.
 */
export default function LocaleAttributes({ locale }: { locale: string }) {
  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  return null;
}
