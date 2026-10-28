import { theme } from 'antd'
import type { ThemeConfig } from 'antd'
import { tokens } from './design-tokens'

/*
 * Ant Design binding for the neutral + violet system in design-tokens.css.
 * Elevation is deliberately switched off (shadows are hairline-flat); surface
 * steps and 1px borders carry the hierarchy instead.
 */
const sharedTokens = {
  fontFamily: tokens.fontFamily.sans,
  colorInfo: tokens.colors.info,
  colorPrimary: tokens.colors.primary,
  colorSuccess: tokens.colors.success,
  colorWarning: tokens.colors.warning,
  colorError: tokens.colors.error,
  colorLink: tokens.colors.primaryText,
  borderRadius: tokens.radius.base,
  fontSize: tokens.fontSize.base,
  controlHeight: 36,
  wireframe: false,
}

const sharedComponents = {
  Layout: {
    headerBg: tokens.colors.bgHeader,
    siderBg: tokens.colors.bgSider,
    bodyBg: tokens.colors.bgLayout,
    headerHeight: tokens.headerHeight,
    headerPadding: `0 ${tokens.contentPadding}px`,
  },
  Menu: {
    itemHeight: 34,
    itemMarginBlock: 2,
    itemMarginInline: 6,
    itemBorderRadius: tokens.radius.sm,
    itemBg: 'transparent',
    itemColor: tokens.colors.textSecondary,
    itemSelectedBg: tokens.colors.brandBlueLight,
    itemSelectedColor: tokens.colors.primaryText,
    itemHoverBg: tokens.colors.bgSunken,
    itemHoverColor: tokens.colors.text,
    subMenuItemBg: 'transparent',
    activeBarWidth: 2,
    iconSize: 15,
  },
  Card: {
    headerBg: 'transparent',
    headerFontSize: tokens.fontSize.lg,
    borderRadiusLG: tokens.radius.base,
    boxShadowTertiary: 'none',
  },
  Button: {
    fontWeight: 500,
    borderRadius: tokens.radius.base,
    primaryShadow: 'none',
    defaultShadow: 'none',
    dangerShadow: 'none',
  },
  Input: {
    borderRadius: tokens.radius.base,
    colorBgContainer: tokens.colors.bgContainer,
    activeBg: tokens.colors.bgContainer,
    hoverBg: tokens.colors.bgContainer,
  },
  Select: {
    borderRadius: tokens.radius.base,
  },
  Table: {
    // Leaderboard treatment: light blue-gray band, black 11px labels,
    // neutral hairlines and neutral-50 row hover.
    headerBg: tokens.colors.brandBlueLight,
    headerColor: tokens.colors.text,
    headerSplitColor: tokens.colors.border,
    headerSortActiveBg: tokens.colors.brandBlueLight,
    headerSortHoverBg: tokens.colors.bgSunken,
    bodySortBg: 'transparent',
    borderColor: tokens.colors.border,
    rowHoverBg: tokens.colors.bgHover,
    rowSelectedBg: tokens.colors.brandBlueLight,
    rowSelectedHoverBg: tokens.colors.bgSunken,
    footerBg: tokens.colors.brandBlueLight,
    headerBorderRadius: 0,
    cellPaddingBlock: 7,
    cellPaddingInline: 10,
    cellFontSize: 12,
    cellFontSizeMD: 12,
    cellFontSizeSM: 12,
  },
  Pagination: {
    itemSize: 28,
    itemSizeSM: 24,
    itemBg: 'transparent',
    itemLinkBg: 'transparent',
    itemActiveBg: tokens.colors.brandBlueLight,
    itemActiveColor: tokens.colors.text,
  },
  Tag: {
    borderRadiusSM: tokens.radius.sm,
    defaultBg: tokens.colors.bgSunken,
    defaultColor: tokens.colors.textSecondary,
  },
  Modal: {
    borderRadiusLG: tokens.radius.xl,
    titleFontSize: tokens.fontSize.xl,
  },
  Descriptions: {
    labelBg: tokens.colors.bgSunken,
  },
  Breadcrumb: {
    itemColor: tokens.colors.textTertiary,
    lastItemColor: tokens.colors.text,
    separatorColor: tokens.colors.borderStrong,
  },
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
    colorPrimaryHover: tokens.colors.primaryHover,
    colorPrimaryActive: tokens.colors.primaryActive,
    colorText: tokens.colors.text,
    colorTextSecondary: tokens.colors.textSecondary,
    colorTextTertiary: tokens.colors.textTertiary,
    colorBorder: tokens.colors.borderStrong,
    colorBorderSecondary: tokens.colors.borderSecondary,
    colorSplit: tokens.colors.borderSecondary,
    boxShadow: '0 4px 16px -6px rgba(20, 20, 19, 0.14)',
    boxShadowSecondary: '0 8px 24px -10px rgba(20, 20, 19, 0.16)',
  },
  algorithm: theme.defaultAlgorithm,
  components: sharedComponents,
}

export const darkTheme: ThemeConfig = {
  token: {
    ...sharedTokens,
    colorPrimary: tokens.colors.primaryDark,
    colorPrimaryHover: tokens.colors.primaryDarkHover,
    colorPrimaryActive: tokens.colors.primaryDarkActive,
    colorInfo: tokens.colors.info,
    colorLink: tokens.colors.primaryDark,
    // Solid violet fills carry white text in both themes.
    colorTextLightSolid: '#ffffff',
    colorBgLayout: tokens.colors.darkBgLayout,
    colorBgContainer: tokens.colors.darkBgContainer,
    colorBgElevated: tokens.colors.darkBgElevated,
    colorText: tokens.colors.darkText,
    colorTextSecondary: tokens.colors.darkTextSecondary,
    colorTextTertiary: tokens.colors.darkTextTertiary,
    colorBorder: tokens.colors.darkBorder,
    colorBorderSecondary: tokens.colors.darkBorderSecondary,
    colorSplit: tokens.colors.darkBorderSecondary,
    boxShadow: '0 4px 16px -6px rgba(0, 0, 0, 0.5)',
    boxShadowSecondary: '0 8px 24px -10px rgba(0, 0, 0, 0.55)',
  },
  algorithm: theme.darkAlgorithm,
  components: {
    ...sharedComponents,
    Layout: {
      ...sharedComponents.Layout,
      headerBg: tokens.colors.darkBgSider,
      siderBg: tokens.colors.darkBgSider,
      bodyBg: tokens.colors.darkBgLayout,
    },
    Menu: {
      ...sharedComponents.Menu,
      itemColor: tokens.colors.darkTextSecondary,
      itemSelectedBg: 'rgba(136, 66, 253, 0.16)',
      itemSelectedColor: tokens.colors.primaryDark,
      itemHoverBg: tokens.colors.darkBgSunken,
      itemHoverColor: tokens.colors.darkText,
    },
    Input: {
      ...sharedComponents.Input,
      colorBgContainer: tokens.colors.darkBgLayout,
      activeBg: tokens.colors.darkBgContainer,
      hoverBg: tokens.colors.darkBgLayout,
    },
    Table: {
      ...sharedComponents.Table,
      headerBg: tokens.colors.darkBgSunken,
      headerColor: tokens.colors.darkText,
      headerSplitColor: tokens.colors.darkBorder,
      headerSortActiveBg: tokens.colors.darkBgSunken,
      headerSortHoverBg: tokens.colors.darkBgElevated,
      rowHoverBg: tokens.colors.darkBgElevated,
      rowSelectedBg: 'rgba(136, 66, 253, 0.16)',
      rowSelectedHoverBg: 'rgba(136, 66, 253, 0.22)',
      borderColor: tokens.colors.darkBorder,
      footerBg: tokens.colors.darkBgSunken,
    },
    Pagination: {
      ...sharedComponents.Pagination,
      itemActiveBg: 'rgba(136, 66, 253, 0.16)',
      itemActiveColor: tokens.colors.darkText,
    },
    Tag: {
      defaultBg: tokens.colors.darkBgSunken,
      defaultColor: tokens.colors.darkTextSecondary,
    },
    Descriptions: {
      labelBg: tokens.colors.darkBgSunken,
    },
    Breadcrumb: {
      itemColor: tokens.colors.darkTextTertiary,
      lastItemColor: tokens.colors.darkText,
      separatorColor: tokens.colors.darkBorder,
    },
  },
}
