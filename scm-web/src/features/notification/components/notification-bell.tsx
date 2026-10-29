'use client'
import { Badge, Dropdown, Empty } from 'antd'
import { BellOutlined } from '@ant-design/icons'
import { useRouter } from 'next/navigation'
import { useUnreadCount, useNotificationList, useMarkAsRead } from '../hooks'
import type { Notification } from '../types'

export default function NotificationBell() {
  const router = useRouter()
  const { data: unreadCount } = useUnreadCount()
  const { data } = useNotificationList({ pageSize: 5 })
  const markAsRead = useMarkAsRead()

  const items = (data?.records || []).map((n: Notification) => ({
    key: n.id,
    label: (
      <div style={{ maxWidth: 280, padding: '4px 0' }} onClick={() => { if (!n.isRead) markAsRead.mutate(n.id); if (n.link) router.push(n.link) }}>
        <div style={{ fontWeight: n.isRead ? 'normal' : 'bold', fontSize: 13 }}>{n.title}</div>
        <div style={{ fontSize: 12, color: 'var(--color-text-tertiary)', marginTop: 2 }}>{n.createdAt}</div>
      </div>
    ),
  }))

  return (
    <Dropdown menu={{ items: items.length ? items : [{ key: 'empty', label: <Empty description="暂无通知" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }] }} placement="bottomRight" trigger={['click']}>
      <Badge count={unreadCount || 0} size="small">
        <BellOutlined style={{ fontSize: 18, cursor: 'pointer' }} />
      </Badge>
    </Dropdown>
  )
}
