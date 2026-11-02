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
  BarChartOutlined,
  AuditOutlined,
  SafetyCertificateOutlined,
  FolderOpenOutlined,
  CloudUploadOutlined,
  ShoppingOutlined,
  GiftOutlined,
  WalletOutlined,
  ContainerOutlined,
  SearchOutlined,
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
  getItem('数据分析', '/analytics', <BarChartOutlined />),
  getItem('商品管理', 'group-product', <ShopOutlined />, [
    getItem('商品列表', '/product'),
    getItem('商品分类', '/product/category'),
    getItem('品牌管理', '/product/brand'),
    getItem('SKU 管理', '/product/sku'),
    getItem('属性模板', '/product/attribute-template'),
  ]),
  getItem('订单管理', 'group-order', <ShoppingCartOutlined />, [
    getItem('订单列表', '/order'),
    getItem('退款管理', '/order/refund'),
    getItem('支付管理', '/order/payment'),
  ]),
  getItem('库存管理', 'group-inventory', <InboxOutlined />, [
    getItem('库存列表', '/inventory'),
    getItem('库存预警', '/inventory/alerts'),
  ]),
  getItem('仓库管理', 'group-warehouse', <ApartmentOutlined />, [
    getItem('仓库列表', '/warehouse'),
    getItem('入库管理', '/warehouse/inbound'),
    getItem('出库管理', '/warehouse/outbound'),
    getItem('拣货波次', '/warehouse/wave-picking'),
  ]),
  getItem('采购管理', 'group-purchase', <FileTextOutlined />, [
    getItem('采购申请', '/purchase/request'),
    getItem('采购订单', '/purchase'),
    getItem('询价管理', '/purchase/rfq'),
    getItem('报价管理', '/purchase/quotation'),
  ]),
  getItem('供应商管理', '/supplier', <TeamOutlined />),
  getItem('物流管理', 'group-logistics', <CarOutlined />, [
    getItem('运单管理', '/logistics'),
    getItem('物流跟踪', '/logistics/tracking'),
    getItem('承运商管理', '/logistics/carrier'),
  ]),
  getItem('财务管理', '/finance', <BookOutlined />, [
    getItem('结算管理', '/finance/settlement'),
    getItem('发票管理', '/finance/invoice'),
    getItem('对账管理', '/finance/reconciliation'),
  ]),
  getItem('文档中心', '/document', <FolderOpenOutlined />),
  getItem('文件中心', '/file', <CloudUploadOutlined />),
  getItem('租户管理', 'group-tenant', <GlobalOutlined />, [
    getItem('租户列表', '/tenant'),
    getItem('套餐管理', '/tenant/package'),
  ]),
  getItem('系统管理', '/system', <SettingOutlined />, [
    getItem('用户管理', '/system/user', <UserOutlined />),
    getItem('角色管理', '/system/role', <SafetyOutlined />),
    getItem('权限管理', '/system/permission'),
    getItem('部门管理', '/system/dept'),
    getItem('字典管理', '/system/dictionary'),
    getItem('状态字典', '/system/status'),
    getItem('决策矩阵', '/system/decision'),
  ]),
  getItem('通知管理', '/notification', <NotificationOutlined />),
  getItem('审批中心', '/approval', <AuditOutlined />),
  getItem('审计日志', '/audit', <SafetyCertificateOutlined />),
  getItem('电商中心', 'group-ecommerce', <ShoppingOutlined />, [
    getItem('会员管理', '/ecommerce/members'),
    getItem('优惠券', '/ecommerce/coupons', <GiftOutlined />),
    getItem('秒杀活动', '/ecommerce/flash-sales'),
    getItem('拼团活动', '/ecommerce/group-buys'),
    getItem('支付单', '/ecommerce/payments', <WalletOutlined />),
    getItem('退款单', '/ecommerce/refunds'),
    getItem('订单中心', '/ecommerce/orders', <ContainerOutlined />),
    getItem('履约单', '/ecommerce/fulfillments'),
    getItem('物流跟踪', '/ecommerce/tracking'),
    getItem('搜索运营', '/ecommerce/search', <SearchOutlined />),
    getItem('商品评价', '/ecommerce/reviews'),
    getItem('卖家管理', '/ecommerce/sellers'),
  ]),
  getItem('商城前台', '/shop', <ShopOutlined />),
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

/** Leaf route key -> key of its parent submenu group. */
const parentGroupMap: Record<string, string> = {}
function collectParentGroups(items: MenuItem[], parentKey?: string) {
  for (const item of items) {
    if (!item) continue
    const { key, children } = item as {
      key?: string
      children?: MenuItem[]
    }
    if (children && children.length > 0) {
      collectParentGroups(children, String(key))
    } else if (parentKey && key) {
      parentGroupMap[key] = parentKey
    }
  }
}
collectParentGroups(appMenuItems)

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

/** Submenu group key that should be open for the current route. */
export function getParentGroupKey(pathname: string): string | null {
  const matched = matchMenuKey(pathname)
  return matched ? parentGroupMap[matched] ?? null : null
}

/** Breadcrumb trail (root → matched item) for the current route. */
export function getBreadcrumb(pathname: string): { key: string; title: string }[] {
  const matched = matchMenuKey(pathname)
  const path = stripLocale(pathname)
  const trail: { key: string; title: string }[] = [{ key: '/', title: '首页' }]

  if (!matched) return trail
  const segments = matched.split('/').filter(Boolean)
  const groupKey = parentGroupMap[matched]
  if (groupKey && segments.length > 1) {
    trail.push({ key: groupKey, title: getMenuLabel(groupKey) })
    trail.push({ key: matched, title: getMenuLabel(matched) })
    return trail
  }
  let current = ''
  for (const seg of segments) {
    current += `/${seg}`
    trail.push({ key: current, title: getMenuLabel(current) })
  }
  // For dynamic detail routes (e.g. /order/C2001) keep the group but drop the id
  if (path !== matched && segments.length > 0) return trail
  return trail
}