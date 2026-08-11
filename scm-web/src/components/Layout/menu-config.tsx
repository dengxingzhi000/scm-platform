import type { MenuProps } from 'antd'
import {
  DashboardOutlined,
  ShoppingCartOutlined,
  InboxOutlined,
  ShopOutlined,
  CarOutlined,
  TeamOutlined,
  SettingOutlined,
  UserOutlined,
  SafetyOutlined,
  BookOutlined,
  FileTextOutlined,
  NotificationOutlined,
  ApartmentOutlined,
  GlobalOutlined,
} from '@ant-design/icons'

type MenuItem = Required<MenuProps>['items'][number]

function getItem(
  label: React.ReactNode,
  key: string,
  icon?: React.ReactNode,
  children?: MenuItem[]
): MenuItem {
  return { key, icon, children, label } as MenuItem
}

export const appMenuItems: MenuItem[] = [
  getItem('仪表盘', '/dashboard', <DashboardOutlined />),
  getItem('商品管理', '/product', <ShopOutlined />, [
    getItem('商品列表', '/product'),
    getItem('商品分类', '/product/category'),
    getItem('品牌管理', '/product/brand'),
  ]),
  getItem('订单管理', '/order', <ShoppingCartOutlined />, [
    getItem('订单列表', '/order'),
    getItem('退款管理', '/order/refund'),
  ]),
  getItem('库存管理', '/inventory', <InboxOutlined />, [
    getItem('库存列表', '/inventory'),
    getItem('库存预警', '/inventory/alerts'),
  ]),
  getItem('仓库管理', '/warehouse', <ApartmentOutlined />, [
    getItem('仓库列表', '/warehouse'),
    getItem('入库管理', '/warehouse/inbound'),
    getItem('出库管理', '/warehouse/outbound'),
    getItem('拣货波次', '/warehouse/wave-picking'),
  ]),
  getItem('采购管理', '/purchase', <FileTextOutlined />, [
    getItem('采购订单', '/purchase'),
    getItem('询价管理', '/purchase/rfq'),
    getItem('报价管理', '/purchase/quotation'),
  ]),
  getItem('供应商管理', '/supplier', <TeamOutlined />),
  getItem('物流管理', '/logistics', <CarOutlined />, [
    getItem('运单管理', '/logistics'),
    getItem('物流跟踪', '/logistics/tracking'),
    getItem('承运商管理', '/logistics/carrier'),
  ]),
  getItem('财务管理', '/finance', <BookOutlined />, [
    getItem('结算管理', '/finance/settlement'),
    getItem('发票管理', '/finance/invoice'),
    getItem('对账管理', '/finance/reconciliation'),
  ]),
  getItem('租户管理', '/tenant', <GlobalOutlined />),
  getItem('系统管理', '/system', <SettingOutlined />, [
    getItem('用户管理', '/system/user', <UserOutlined />),
    getItem('角色管理', '/system/role', <SafetyOutlined />),
    getItem('权限管理', '/system/permission'),
    getItem('部门管理', '/system/dept'),
    getItem('字典管理', '/system/dictionary'),
  ]),
  getItem('通知管理', '/notification', <NotificationOutlined />),
]

const labelMap: Record<string, string> = {}
function collectLabel(items: MenuItem[]) {
  for (const item of items) {
    if (!item || typeof item === 'undefined') continue
    const { key, label, children } = item as {
      key: string
      label: React.ReactNode
      children?: MenuItem[]
    }
    labelMap[key] = String(label)
    if (children) collectLabel(children)
  }
}
collectLabel(appMenuItems)

export function stripLocale(pathname: string): string {
  return pathname.replace(/^\/[a-z]{2}-[A-Z]{2}/, '') || pathname
}

/** Match the longest menu key that is a path-prefix of the current route. */
export function matchMenuKey(pathname: string): string | null {
  const path = stripLocale(pathname)
  const keys = Object.keys(labelMap)
    .filter((key) => path === key || path.startsWith(`${key}/`))
    .sort((a, b) => b.length - a.length)
  return keys[0] ?? null
}

export function getMenuLabel(key: string): string {
  return labelMap[key] ?? key
}

/** Breadcrumb trail (root → matched item) for the current route. */
export function getBreadcrumb(pathname: string): { key: string; title: string }[] {
  const matched = matchMenuKey(pathname)
  const path = stripLocale(pathname)
  const trail: { key: string; title: string }[] = [{ key: '/', title: '首页' }]

  if (!matched) return trail
  const segments = matched.split('/').filter(Boolean)
  let current = ''
  for (const seg of segments) {
    current += `/${seg}`
    trail.push({ key: current, title: getMenuLabel(current) })
  }
  // For dynamic detail routes (e.g. /order/C2001) keep the group but drop the id
  if (path !== matched && segments.length > 0) return trail
  return trail
}