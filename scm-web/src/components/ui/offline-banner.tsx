'use client'

import { useSyncExternalStore } from 'react'
import { Alert } from 'antd'

function subscribe(onChange: () => void) {
  window.addEventListener('online', onChange)
  window.addEventListener('offline', onChange)
  return () => {
    window.removeEventListener('online', onChange)
    window.removeEventListener('offline', onChange)
  }
}

const getSnapshot = () => navigator.onLine
// SSR 阶段没有 navigator，先按在线渲染，水合后切换到真实状态。
const getServerSnapshot = () => true

export default function OfflineBanner() {
  const isOnline = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot)

  if (isOnline) return null

  return (
    <Alert
      title="网络连接已断开，请检查您的网络设置"
      type="warning"
      showIcon
      banner
      closable={false}
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        zIndex: 9999,
      }}
    />
  )
}
