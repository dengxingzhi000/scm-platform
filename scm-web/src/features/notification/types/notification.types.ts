export interface Notification {
  id: string
  title: string
  content: string
  type: 'system' | 'order' | 'inventory' | 'approval' | 'security'
  typeLabel: string
  isRead: boolean
  link?: string
  createdAt: string
  /** Raw backend timestamp, kept so a panel can render relative time without re-parsing. */
  createTime: string | null
}

export interface NotificationSearchParams {
  current?: number
  pageSize?: number
  type?: string
  isRead?: boolean
  /** Matches the title or the content. */
  keyword?: string
  /** ISO-8601 local date-time, inclusive. */
  startTime?: string
  endTime?: string
}
