/*
 * TypeScript mirror of src/lib/design-tokens.css — consumed by the Ant Design
 * theme. Keep the two files in sync; the CSS custom properties remain the
 * runtime source for Tailwind and canvas charts.
 */
export const tokens = {
  // Layout
  headerHeight: 56,
  sidebarWidth: 236,
  sidebarCollapsedWidth: 80,
  contentPadding: 24,

  // Spacing (8px grid)
  spacing: {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
    xxl: 48,
    xxxl: 64,
  },

  // Typography
  fontSize: {
    xs: 12,
    sm: 13,
    base: 14,
    lg: 16,
    xl: 20,
    xxl: 24,
    xxxl: 32,
  },

  fontFamily: {
    sans: 'Inter, ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans SC", Arial, sans-serif',
    serif: 'Georgia, "Iowan Old Style", "Palatino Linotype", "Songti SC", "SimSun", "Noto Serif SC", "Source Han Serif SC", "Times New Roman", serif',
    mono: 'ui-monospace, "SF Mono", "Cascadia Mono", Menlo, Consolas, monospace',
  },

  // Border radius
  radius: {
    xs: 4,
    sm: 6,
    base: 8,
    lg: 12,
    xl: 16,
    xxl: 24,
    pill: 999,
  },

  // Colors (light theme) — artificialanalysis.ai's white/black neutrals with
  // the violet primary (brand-purple #8842fd, brand-blue hover #6c1bfd).
  colors: {
    brandPurple: '#8842fd',
    brandBlue: '#6c1bfd',
    brandBlueLight: '#f1f2f4',
    primary: '#8842fd',
    primaryHover: '#6c1bfd',
    primaryActive: '#5b13d9',
    primaryBg: '#f1f2f4',
    primaryText: '#6c1bfd',
    primaryOn: '#ffffff',
    clay: '#8842fd',
    primaryDark: '#8842fd',
    primaryDarkHover: '#9b5cff',
    primaryDarkActive: '#6c1bfd',
    primaryOnDark: '#ffffff',
    success: '#16a34a',
    successText: '#166534',
    warning: '#d97706',
    warningText: '#92400e',
    error: '#dc2626',
    errorText: '#991b1b',
    info: '#6c1bfd',
    bgLayout: '#ffffff',
    bgContainer: '#ffffff',
    bgElevated: '#ffffff',
    bgSider: '#fafafa',
    bgHeader: '#ffffff',
    bgSunken: '#f5f5f5',
    bgHover: '#fafafa',
    bgInk: '#000000',
    text: '#000000',
    textSecondary: '#787878',
    textTertiary: '#949494',
    border: '#d9d9d9',
    borderSecondary: '#e7e7e7',
    borderStrong: '#4d4d4d',
    darkBgLayout: '#0a0a0a',
    darkBgContainer: '#141414',
    darkBgElevated: '#1a1a1a',
    darkBgSider: '#0f0f0f',
    darkBgSunken: '#1f1f1f',
    darkText: '#fafafa',
    darkTextSecondary: '#a3a3a3',
    darkTextTertiary: '#737373',
    darkBorder: '#262626',
    darkBorderSecondary: '#1f1f1f',
    chart: [
      '#8842fd',
      '#2563eb',
      '#ff7734',
      '#16a34a',
      '#e676fd',
      '#0d9488',
      '#fde002',
      '#949494',
    ],
  },

  // Shadows — flat; borders carry the separation
  shadow: {
    xs: '0 1px 1px 0 rgba(20,20,19,0.03)',
    sm: '0 1px 2px 0 rgba(20,20,19,0.04)',
    base: '0 2px 8px -2px rgba(20,20,19,0.07)',
    lg: '0 8px 24px -8px rgba(20,20,19,0.12)',
  },

  // Breakpoints
  breakpoint: {
    xs: 480,
    sm: 576,
    md: 768,
    lg: 992,
    xl: 1200,
    xxl: 1600,
  },

  // Animation
  duration: {
    fast: 100,
    normal: 200,
    slow: 300,
  },

  // Z-index layers
  zIndex: {
    dropdown: 1050,
    sticky: 1100,
    fixed: 1200,
    modalBackdrop: 1300,
    modal: 1400,
    popover: 1500,
    tooltip: 1600,
  },
} as const

export type Tokens = typeof tokens
