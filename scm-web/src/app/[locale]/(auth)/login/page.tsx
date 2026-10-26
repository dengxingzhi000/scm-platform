'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useLocale } from 'next-intl'
import { Form, Input, Button, App, Typography, Checkbox, Space } from 'antd'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { authService } from '@/lib/api/services/auth'
import { toApiError } from '@/lib/api/envelope'
import { toSessionUser, mergeUserInfo } from '@/lib/auth/session'
import { useAuthStore } from '@/stores/useAuthStore'
import type { LoginRequest, LoginResponse } from '@/lib/api/contracts/auth'
import AuthBrandCard from '@/components/ui/auth-brand-card'

const { Link } = Typography

interface LoginForm {
  username: string
  password: string
  remember?: boolean
}

interface TOTPForm {
  code: string
}

const isMfaRequired = (message: string, code: number): boolean =>
  code === 40104 || /two-factor|verification code|MFA/i.test(message)

export default function LoginPage() {
  const [showTOTP, setShowTOTP] = useState(false)
  const [pendingCredentials, setPendingCredentials] = useState<LoginForm | null>(null)
  const router = useRouter()
  const locale = useLocale()
  const queryClient = useQueryClient()
  const { message } = App.useApp()
  const { login, setUser, setPermissions } = useAuthStore()

  const loginMutation = useMutation({
    mutationFn: (data: LoginRequest) => authService.login(data),
  })

  const completeLogin = async (response: LoginResponse) => {
    if (!response.accessToken) {
      message.error(response.message || '登录失败')
      return
    }

    login(
      toSessionUser(response),
      response.accessToken,
      response.refreshToken ?? ''
    )
    setPermissions(response.permissions ?? [])

    try {
      const userInfo = await authService.getUserInfo()
      setUser(mergeUserInfo(useAuthStore.getState().user, userInfo))
    } catch {
      // The token is valid; profile enrichment is best-effort.
    }

    queryClient.clear()
    message.success('登录成功')
    router.push(`/${locale}/dashboard`)
  }

  const onLoginFinish = async (values: LoginForm) => {
    try {
      const response = await loginMutation.mutateAsync({
        username: values.username,
        password: values.password,
      })
      await completeLogin(response)
    } catch (error: unknown) {
      const apiError = toApiError(error)
      if (isMfaRequired(apiError.message, apiError.code)) {
        setPendingCredentials(values)
        setShowTOTP(true)
        return
      }
      message.error(apiError.message || '登录失败')
    }
  }

  const onTOTPFinish = async (values: TOTPForm) => {
    if (!pendingCredentials) {
      setShowTOTP(false)
      return
    }

    try {
      const response = await loginMutation.mutateAsync({
        username: pendingCredentials.username,
        password: pendingCredentials.password,
        twoFactorCode: values.code,
      })
      await completeLogin(response)
    } catch (error: unknown) {
      const apiError = toApiError(error)
      message.error(apiError.message || '验证码错误')
    }
  }

  if (showTOTP) {
    return (
      <AuthBrandCard title="两步验证" subtitle="请输入您的 TOTP 验证码">
        <Form name="totp" onFinish={onTOTPFinish} layout="vertical">
          <Form.Item
            name="code"
            rules={[
              { required: true, message: '请输入验证码' },
              { len: 6, message: '验证码为6位数字' },
              { pattern: /^\d+$/, message: '验证码只能包含数字' },
            ]}
          >
            <Input
              prefix={<SafetyOutlined />}
              placeholder="6位验证码"
              maxLength={6}
              size="large"
              style={{ textAlign: 'center', fontSize: 24, letterSpacing: 8 }}
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loginMutation.isPending} block size="large">
              验证
            </Button>
          </Form.Item>
          <Button
            type="link"
            block
            onClick={() => {
              setShowTOTP(false)
              setPendingCredentials(null)
            }}
          >
            返回登录
          </Button>
        </Form>
      </AuthBrandCard>
    )
  }

  return (
    <AuthBrandCard title="欢迎回来" subtitle="登录您的供应链管理中台">
      <Form name="login" onFinish={onLoginFinish} layout="vertical" initialValues={{ remember: true }}>
        <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
          <Input prefix={<UserOutlined />} placeholder="用户名" size="large" />
        </Form.Item>
        <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
          <Input.Password prefix={<LockOutlined />} placeholder="密码" size="large" />
        </Form.Item>
        <Form.Item>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Form.Item name="remember" valuePropName="checked" noStyle>
              <Checkbox>记住我</Checkbox>
            </Form.Item>
            <Link href={`/${locale}/forgot-password`}>忘记密码?</Link>
          </Space>
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loginMutation.isPending} block size="large">
            登录
          </Button>
        </Form.Item>
      </Form>
    </AuthBrandCard>
  )
}
