'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useLocale } from 'next-intl'
import { Form, Input, Button, App, Typography, Checkbox, Space } from 'antd'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/lib/api/endpoints'
import { useAuthStore } from '@/stores/useAuthStore'
import type { LoginRequest, LoginResponse } from '@/lib/api/types'
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

export default function LoginPage() {
  const [showTOTP, setShowTOTP] = useState(false)
  const [tempToken, setTempToken] = useState('')
  const [totpLoading, setTotpLoading] = useState(false)
  const router = useRouter()
  const locale = useLocale()
  const { message } = App.useApp()
  const { login, setPermissions } = useAuthStore()

  const loginMutation = useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
  })

  const handleLoginSuccess = (response: LoginResponse) => {
    const { user, accessToken, refreshToken, permissions } = response
    login(
      {
        id: user.id,
        username: user.username,
        displayName: user.displayName,
        email: user.email,
        avatar: user.avatar,
        roles: user.roleNames,
      },
      accessToken,
      refreshToken
    )
    if (permissions) {
      setPermissions(permissions)
    }
    message.success('登录成功')
    router.push(`/${locale}/dashboard`)
  }

  const onLoginFinish = async (values: LoginForm) => {
    try {
      const response = (await loginMutation.mutateAsync({
        username: values.username,
        password: values.password,
      })) as unknown as { data: LoginResponse }

      if (response.data.requireMfa) {
        setTempToken(response.data.tempToken!)
        setShowTOTP(true)
      } else {
        handleLoginSuccess(response.data)
      }
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } }
      message.error(err.response?.data?.message || '登录失败')
    }
  }

  const onTOTPFinish = async (values: TOTPForm) => {
    setTotpLoading(true)
    try {
      const response = (await authApi.verifyMfa({
        tempToken,
        code: values.code,
      })) as unknown as { data: LoginResponse }
      handleLoginSuccess(response.data)
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } }
      message.error(err.response?.data?.message || '验证码错误')
    } finally {
      setTotpLoading(false)
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
            <Button type="primary" htmlType="submit" loading={totpLoading} block size="large">
              验证
            </Button>
          </Form.Item>
          <Button type="link" block onClick={() => setShowTOTP(false)}>
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