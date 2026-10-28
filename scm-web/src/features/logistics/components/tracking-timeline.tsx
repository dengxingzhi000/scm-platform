'use client'
import { Card, Timeline, Tag } from 'antd'
import { useTrackingEvents } from '../hooks'

interface Props { waybillId: string }

export default function TrackingTimeline({ waybillId }: Props) {
  const { data: events, isLoading } = useTrackingEvents(waybillId)

  return (
    <Card title="物流跟踪" loading={isLoading}>
      <Timeline
        items={(events || []).map((e, i) => ({
          children: (
            <div>
              <div style={{ fontWeight: i === 0 ? 'bold' : 'normal' }}>{e.description}</div>
              <div style={{ fontSize: 12, color: 'var(--color-text-tertiary)' }}>{e.time} · {e.location}</div>
            </div>
          ),
          color: i === 0 ? 'green' : 'gray',
        }))}
      />
    </Card>
  )
}
