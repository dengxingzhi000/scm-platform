import { theme } from 'antd'
import type { ThemeConfig } from 'antd'
import { tokens } from './design-tokens'

const sharedTokens = {
  colorInfo: tokens.colors.info,
  colorPrimary: tokens.colors.primary,
  colorSuccess: tokens.colors.success,
  colorWarning: tokens.colors.warning,
  colorError: tokens.colors.error,
  colorLink: tokens.colors.primary,
  borderRadius: tokens.radius.base,
  fontSize: tokens.fontSize.base,
  controlHeight: 36,
}

const sharedComponents = {
  Layout: {
    headerBg: tokens.colors.bgContainer,
    siderBg: tokens.colors.bgContainer,
    headerHeight: tokens.headerHeight,
    headerPadding: `${tokens.spacing.md}px ${tokens.contentPadding}px`,
  },
  Menu: {
    itemHeight: 40,
    itemMarginBlock: 4,
    itemBorderRadius: tokens.radius.sm,
    itemBg: 'transparent',
    itemSelectedBg: tokens.colors.primaryBg,
    itemSelectedColor: tokens.colors.primary,
    itemHoverBg: 'rgba(79, 70, 229, 0.06)',
    subMenuItemBg: 'transparent',
    iconSize: 16,
  },
  Card: {
    headerFontSize: tokens.fontSize.lg,
  },
  Table: {
    headerBg: tokens.colors.bgLayout,
    headerColor: tokens.colors.textSecondary,
    headerSplitColor: tokens.colors.borderSecondary,
  } as Record<string, string>,
  Statistic: {
    contentFontSize: 28,
  },
} as const

export const lightTheme: ThemeConfig = {
  token: {
    ...sharedTokens,
    colorBgLayout: tokens.colors.bgLayout,
    colorBgContainer: tokens.colors.bgContainer,
    colorBgElevated: tokens.colors.bgElevated,
    colorText: tokens.colors.text,
    colorTextSecondary: tokens.colors.textSecondary,
    colorBorder: tokens.colors.border,
    colorBorderSecondary: tokens.colors.borderSecondary,
  },
  algorithm: theme.defaultAlgorithm,
  components: sharedComponents,
}

export const darkTheme: ThemeConfig = {
  token: {
    ...sharedTokens,
    colorPrimary: tokens.colors.primaryDark,
    colorInfo: tokens.colors.primaryDark,
    colorLink: tokens.colors.primaryDark,
    colorBgLayout: tokens.colors.darkBgLayout,
    colorBgContainer: tokens.colors.darkBgContainer,
    colorBgElevated: tokens.colors.darkBgElevated,
    colorText: tokens.colors.darkText,
    colorTextSecondary: tokens.colors.darkTextSecondary,
  },
  algorithm: theme.darkAlgorithm,
  components: {
    ...sharedComponents,
    Layout: {
      ...sharedComponents.Layout,
      headerBg: tokens.colors.darkBgContainer,
      siderBg: tokens.colors.darkBgContainer,
    },
    Menu: {
      ...sharedComponents.Menu,
      itemSelectedBg: 'rgba(99, 102, 241, 0.2)',
      itemSelectedColor: '#a5b4fc',
      itemHoverBg: 'rgba(99, 102, 241, 0.12)',
    },
    Table: {
      headerBg: 'rgba(255, 255, 255, 0.04)',
      headerColor: 'rgba(255, 255, 255, 0.65)',
      headerSplitColor: 'rgba(255, 255, 255, 0.08)',
    },
  },
}