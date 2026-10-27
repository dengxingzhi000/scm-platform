'use client'
import { Steps } from 'antd'
import type { OrderStatus } from '../types'

const steps = [
  { title: '待付款', key: 'PENDING_PAYMENT' },
  { title: '已付款', key: 'PAID' },
  { title: '待发货', key: 'PENDING_SHIP' },
  { title: '已发货', key: 'SHIPPED' },
  { title: '运输中', key: 'IN_TRANSIT' },
  { title: '已送达', key: 'DELIVERED' },
  { title: '已完成', key: 'COMPLETED' },
]

interface OrderStatusFlowProps { status: OrderStatus }

export default function OrderStatusFlow({ status }: OrderStatusFlowProps) {
  if (status === 'CANCELLED') {
    return <Steps current={-1} items={[...steps.slice(0, 3), { title: '已取消', status: 'error' }]} />
  }
  if (status === 'REFUNDING') {
    return <Steps current={-1} items={[...steps.slice(0, 2), { title: '退款中', status: 'process' }]} />
  }
  if (status === 'REFUNDED') {
    return <Steps current={-1} items={[...steps.slice(0, 2), { title: '已退款', status: 'error' }]} />
  }
  const currentStep = steps.findIndex(s => s.key === status)
  return <Steps current={currentStep >= 0 ? currentStep : 0} items={steps.map((s, i) => ({ ...s, status: i <= currentStep ? 'finish' : 'wait' }))} />
}
