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
  bgSunken: string;
  bgInk: string;
  primary: string;
  clay: string;
  success: string;
  warning: string;
  error: string;
  /** Violet/blue-led categorical series for multi-series charts. */
  chart: string[];
}

const FALLBACK_TOKENS: ThemeTokens = {
  text: "#000000",
  textSecondary: "#787878",
  textTertiary: "#949494",
  border: "#d9d9d9",
  borderSecondary: "#e7e7e7",
  bgContainer: "#ffffff",
  bgElevated: "#ffffff",
  bgSunken: "#f5f5f5",
  bgInk: "#000000",
  primary: "#8842fd",
  clay: "#8842fd",
  success: "#16a34a",
  warning: "#d97706",
  error: "#dc2626",
  chart: [
    "#8842fd",
    "#2563eb",
    "#ff7734",
    "#16a34a",
    "#e676fd",
    "#0d9488",
    "#fde002",
    "#949494",
  ],
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
    bgSunken: read("--color-bg-sunken", FALLBACK_TOKENS.bgSunken),
    bgInk: read("--color-bg-ink", FALLBACK_TOKENS.bgInk),
    primary: read("--color-primary", FALLBACK_TOKENS.primary),
    clay: read("--color-clay", FALLBACK_TOKENS.clay),
    success: read("--color-success", FALLBACK_TOKENS.success),
    warning: read("--color-warning", FALLBACK_TOKENS.warning),
    error: read("--color-error", FALLBACK_TOKENS.error),
    chart: Array.from(
      { length: FALLBACK_TOKENS.chart.length },
      (_, index) =>
        read(`--color-chart-${index + 1}`, FALLBACK_TOKENS.chart[index]),
    ),
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
