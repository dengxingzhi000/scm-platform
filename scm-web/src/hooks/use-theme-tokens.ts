"use client";

import { useEffect, useState } from "react";

export interface ThemeTokens {
  text: string;
  textSecondary: string;
  textTertiary: string;
  border: string;
  borderSecondary: string;
  bgContainer: string;
  bgElevated: string;
  primary: string;
  success: string;
  warning: string;
  error: string;
}

const FALLBACK_TOKENS: ThemeTokens = {
  text: "#1f2329",
  textSecondary: "#6b7280",
  textTertiary: "#9ca3af",
  border: "#e2e5ea",
  borderSecondary: "#eef0f4",
  bgContainer: "#ffffff",
  bgElevated: "#ffffff",
  primary: "#4f46e5",
  success: "#16a34a",
  warning: "#d97706",
  error: "#dc2626",
};

function readTokens(): ThemeTokens {
  if (typeof window === "undefined") return FALLBACK_TOKENS;

  const styles = window.getComputedStyle(document.documentElement);
  const read = (name: string, fallback: string) =>
    styles.getPropertyValue(name).trim() || fallback;

  return {
    text: read("--color-text", FALLBACK_TOKENS.text),
    textSecondary: read("--color-text-secondary", FALLBACK_TOKENS.textSecondary),
    textTertiary: read("--color-text-tertiary", FALLBACK_TOKENS.textTertiary),
    border: read("--color-border", FALLBACK_TOKENS.border),
    borderSecondary: read(
      "--color-border-secondary",
      FALLBACK_TOKENS.borderSecondary,
    ),
    bgContainer: read("--color-bg-container", FALLBACK_TOKENS.bgContainer),
    bgElevated: read("--color-bg-elevated", FALLBACK_TOKENS.bgElevated),
    primary: read("--color-primary", FALLBACK_TOKENS.primary),
    success: read("--color-success", FALLBACK_TOKENS.success),
    warning: read("--color-warning", FALLBACK_TOKENS.warning),
    error: read("--color-error", FALLBACK_TOKENS.error),
  };
}

/**
 * Resolves the CSS design tokens for canvas-based charts (ECharts, G6) which
 * cannot consume `var(--token)` values directly. Re-reads when ThemeProvider
 * flips `data-theme` on `<html>`.
 */
export function useThemeTokens(): ThemeTokens {
  const [tokens, setTokens] = useState<ThemeTokens>(FALLBACK_TOKENS);

  useEffect(() => {
    const update = () => setTokens(readTokens());
    update();

    const observer = new MutationObserver(update);
    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ["data-theme"],
    });

    return () => observer.disconnect();
  }, []);

  return tokens;
}
