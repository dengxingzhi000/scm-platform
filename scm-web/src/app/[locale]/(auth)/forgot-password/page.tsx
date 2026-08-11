'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useLocale } from 'next-intl'
import { Form, Input, Button, App, Typography, Steps } from 'antd'
import { MailOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'
import AuthBrandCard from '@/components/ui/auth-brand-card'

const { Link, Text } = Typography

export default function ForgotPasswordPage() {
  const [currentStep, setCurrentStep] = useState(0)
  const [loading, setLoading] = useState(false)
  const [email, setEmail] = useState('')
  const router = useRouter()
  const locale = useLocale()
  const { message } = App.useApp()

  const onEmailSubmit = async (values: { email: string }) => {
    setLoading(true)
    try {
      setEmail(values.email)
      message.success('验证码已发送到您的邮箱')
      setCurrentStep(1)
    } catch {
      message.error('发送验证码失败')
    } finally {
      setLoading(false)
    }
  }

  const onCodeSubmit = async () => {
    setCurrentStep(2)
  }

  const onPasswordSubmit = async () => {
    message.success('密码重置成功')
    router.push(`/${locale}/login`)
  }

  const steps = [
    {
      title: '验证邮箱',
      content: (
        <Form onFinish={onEmailSubmit} layout="vertical">
          <Form.Item name="email" rules={[{ required: true, message: '请输入邮箱地址' }, { type: 'email', message: '请输入有效的邮箱地址' }]}>
            <Input prefix={<MailOutlined />} placeholder="请输入注册邮箱" size="large" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large">发送验证码</Button>
          </Form.Item>
        </Form>
      ),
    },
    {
      title: '验证身份',
      content: (
        <Form onFinish={onCodeSubmit} layout="vertical">
          <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>验证码已发送至 {email}</Text>
          <Form.Item name="code" rules={[{ required: true, message: '请输入验证码' }, { len: 6, message: '验证码为6位数字' }]}>
            <Input prefix={<SafetyOutlined />} placeholder="6位验证码" maxLength={6} size="large" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large">验证</Button>
          </Form.Item>
        </Form>
      ),
    },
    {
      title: '重置密码',
      content: (
        <Form onFinish={onPasswordSubmit} layout="vertical">
          <Form.Item name="password" rules={[{ required: true, message: '请输入新密码' }, { min: 8, message: '密码至少8位' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="新密码" size="large" />
          </Form.Item>
          <Form.Item name="confirmPassword" dependencies={['password']} rules={[{ required: true, message: '请确认密码' }, ({ getFieldValue }) => ({ validator(_, value) { if (!value || getFieldValue('password') === value) return Promise.resolve(); return Promise.reject(new Error('两次密码不一致')) } })]}>
            <Input.Password prefix={<LockOutlined />} placeholder="确认密码" size="large" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large">重置密码</Button>
          </Form.Item>
        </Form>
      ),
    },
  ]

  return (
    <AuthBrandCard title="找回密码" subtitle="通过已验证邮箱重置您的密码" maxWidth={440}>
      <Steps current={currentStep} items={steps} style={{ marginBottom: 'var(--spacing-lg)' }} />
      {steps[currentStep].content}
      <div style={{ textAlign: 'center', marginTop: 16 }}>
        <Link href={`/${locale}/login`}>返回登录</Link>
      </div>
    </AuthBrandCard>
  )
}