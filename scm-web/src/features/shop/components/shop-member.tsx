'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { Coins, MapPin, UserRound } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
import { toApiError } from '@/lib/api/envelope'
import { memberApi } from '@/lib/api/services/ecommerce'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  formatDate,
  formatDateTime,
  memberStatusMap,
  statusMeta,
} from '@/features/ecommerce/lib/status'

const GENDER_LABELS: Record<number, string> = { 0: '未知', 1: '男', 2: '女' }

export default function ShopMember() {
  const userId = useAuthStore((state) => state.user?.id ?? null)
  const queryClient = useQueryClient()

  const [registerNickname, setRegisterNickname] = useState('')
  const [profileOpen, setProfileOpen] = useState(false)
  const [profileForm, setProfileForm] = useState({
    nickname: '',
    avatar: '',
    gender: '0',
    birthday: '',
  })
  const [addressOpen, setAddressOpen] = useState(false)
  const [addressForm, setAddressForm] = useState({
    receiverName: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
  })

  const memberQuery = useQuery({
    queryKey: ['shop', 'member', userId],
    queryFn: () => memberApi.get(userId!),
    enabled: !!userId,
    retry: false,
  })

  const pointsQuery = useQuery({
    queryKey: ['shop', 'member', userId, 'points'],
    queryFn: () => memberApi.points.balance(userId!),
    enabled: !!userId && !!memberQuery.data,
  })

  const pointsLogQuery = useQuery({
    queryKey: ['shop', 'member', userId, 'points', 'log'],
    queryFn: () => memberApi.points.log(userId!),
    enabled: !!userId && !!memberQuery.data,
  })

  const addressesQuery = useQuery({
    queryKey: ['shop', 'member', userId, 'addresses'],
    queryFn: () => memberApi.addresses.list(userId!),
    enabled: !!userId && !!memberQuery.data,
  })

  const invalidateMember = () => {
    void queryClient.invalidateQueries({ queryKey: ['shop', 'member'] })
  }

  const register = useMutation({
    mutationFn: () =>
      memberApi.register({ userId: userId!, nickname: registerNickname.trim() }),
    onSuccess: () => {
      message.success('会员注册成功')
      invalidateMember()
    },
    onError: (error) => message.error(toApiError(error).message),
  })

  const updateProfile = useMutation({
    mutationFn: () =>
      memberApi.update(userId!, {
        nickname: profileForm.nickname || undefined,
        avatar: profileForm.avatar || undefined,
        gender: Number(profileForm.gender),
        birthday: profileForm.birthday || undefined,
      }),
    onSuccess: () => {
      message.success('资料已更新')
      setProfileOpen(false)
      invalidateMember()
    },
    onError: (error) => message.error(toApiError(error).message),
  })

  const createAddress = useMutation({
    mutationFn: () => memberApi.addresses.create(userId!, addressForm),
    onSuccess: () => {
      message.success('地址已新增')
      setAddressOpen(false)
      setAddressForm({
        receiverName: '',
        phone: '',
        province: '',
        city: '',
        district: '',
        detailAddress: '',
      })
      invalidateMember()
    },
    onError: (error) => message.error(toApiError(error).message),
  })

  const removeAddress = useMutation({
    mutationFn: (addressId: number) => memberApi.addresses.remove(userId!, addressId),
    onSuccess: () => {
      message.success('地址已删除')
      invalidateMember()
    },
    onError: (error) => message.error(toApiError(error).message),
  })

  const setDefault = useMutation({
    mutationFn: (addressId: number) => memberApi.addresses.setDefault(userId!, addressId),
    onSuccess: () => {
      message.success('默认地址已更新')
      invalidateMember()
    },
    onError: (error) => message.error(toApiError(error).message),
  })

  if (!userId) {
    return (
      <div className="rounded-2xl border border-dashed border-[var(--color-border-secondary)] bg-white px-6 py-20 text-center">
        <h2 className="text-[16px] font-semibold">请先登录</h2>
        <Button asChild className="mt-4">
          <Link href="/login">去登录</Link>
        </Button>
      </div>
    )
  }

  if (memberQuery.isLoading) {
    return <div className="h-64 animate-pulse rounded-2xl bg-[var(--color-bg-sunken)]" />
  }

  const member = memberQuery.data

  if (!member) {
    return (
      <div className="mx-auto max-w-md rounded-2xl border border-[var(--color-border-secondary)] bg-white p-6 text-center">
        <span className="mx-auto flex size-14 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-[var(--color-primary-text)]">
          <UserRound className="size-6" />
        </span>
        <h1 className="mt-4 text-[18px] font-semibold">注册会员</h1>
        <p className="mt-1 text-[12px] text-[var(--color-text-secondary)]">
          当前账号尚未注册会员，注册后可查看积分、地址与等级。
        </p>
        <div className="mt-5 text-left">
          <Label className="text-xs">昵称</Label>
          <Input
            value={registerNickname}
            onChange={(event) => setRegisterNickname(event.target.value)}
            placeholder="给自己起个昵称"
            className="mt-1"
          />
        </div>
        <Button
          className="mt-4 w-full"
          disabled={register.isPending}
          onClick={() => register.mutate()}
        >
          立即注册
        </Button>
      </div>
    )
  }

  const meta = statusMeta(memberStatusMap, member.status)
  const balance = pointsQuery.data ?? member.points ?? 0

  return (
    <div className="space-y-5">
      <section className="relative overflow-hidden rounded-2xl border border-[var(--color-border-secondary)] bg-white p-6">
        <div
          aria-hidden
          className="pointer-events-none absolute -top-16 -right-10 size-52 rounded-full bg-[radial-gradient(circle,var(--color-primary-soft),transparent_70%)]"
        />
        <div className="relative flex flex-wrap items-center gap-4">
          <div className="flex size-16 items-center justify-center overflow-hidden rounded-2xl bg-[var(--color-primary-soft)] text-[var(--color-primary-text)]">
            {member.avatar ? (
              <img src={member.avatar} alt={member.nickname ?? ''} className="size-full object-cover" />
            ) : (
              <span className="font-serif text-2xl">
                {(member.nickname || member.memberNo).slice(0, 1)}
              </span>
            )}
          </div>
          <div className="flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-[20px] font-semibold">{member.nickname || member.memberNo}</h1>
              <Badge variant="info">V{member.memberLevel ?? 1}</Badge>
              <Badge variant={meta.tone}>{meta.label}</Badge>
            </div>
            <p className="mt-1 font-mono text-[11px] text-[var(--color-text-tertiary)]">
              {member.memberNo} · {member.userId}
            </p>
            <p className="mt-1 text-[11px] text-[var(--color-text-secondary)]">
              注册于 {formatDate(member.createdAt)} · 累计消费 ¥{Number(member.totalSpent ?? 0).toFixed(2)}
            </p>
          </div>
          <Button variant="outline" onClick={() => {
            setProfileForm({
              nickname: member.nickname ?? '',
              avatar: member.avatar ?? '',
              gender: String(member.gender ?? 0),
              birthday: member.birthday ?? '',
            })
            setProfileOpen(true)
          }}>
            编辑资料
          </Button>
        </div>
      </section>

      <div className="grid gap-5 lg:grid-cols-[1fr_1.2fr]">
        <section className="rounded-2xl border border-[var(--color-border-secondary)] bg-white p-5">
          <div className="flex items-center justify-between">
            <h2 className="flex items-center gap-1.5 text-[14px] font-semibold">
              <Coins className="size-4" />
              我的积分
            </h2>
            <span className="text-[24px] font-semibold text-[var(--color-primary-text)]">
              {balance}
            </span>
          </div>
          <Separator className="my-3" />
          <div className="max-h-64 space-y-1 overflow-y-auto">
            {(pointsLogQuery.data?.length ?? 0) === 0 ? (
              <p className="py-6 text-center text-[11px] text-[var(--color-text-secondary)]">
                暂无积分流水
              </p>
            ) : (
              pointsLogQuery.data!.map((log) => (
                <div
                  key={log.id}
                  className="flex items-center justify-between gap-3 rounded-md px-2 py-1.5 text-[11px] hover:bg-[var(--color-bg-sunken)]"
                >
                  <span
                    className={
                      log.points >= 0
                        ? 'font-mono font-semibold text-[var(--color-success-text)]'
                        : 'font-mono font-semibold text-[var(--color-error-text)]'
                    }
                  >
                    {log.points >= 0 ? '+' : ''}
                    {log.points}
                  </span>
                  <span className="min-w-0 flex-1 truncate text-[var(--color-text-secondary)]">
                    {log.description || log.source || '—'}
                  </span>
                  <span className="text-[10px] text-[var(--color-text-tertiary)]">
                    {formatDate(log.createdAt)}
                  </span>
                </div>
              ))
            )}
          </div>
        </section>

        <section className="rounded-2xl border border-[var(--color-border-secondary)] bg-white p-5">
          <div className="flex items-center justify-between">
            <h2 className="flex items-center gap-1.5 text-[14px] font-semibold">
              <MapPin className="size-4" />
              收货地址
            </h2>
            <Button size="sm" variant="outline" onClick={() => setAddressOpen(true)}>
              新增地址
            </Button>
          </div>
          <Separator className="my-3" />
          {(addressesQuery.data?.length ?? 0) === 0 ? (
            <p className="py-6 text-center text-[11px] text-[var(--color-text-secondary)]">
              暂无收货地址
            </p>
          ) : (
            <div className="space-y-2">
              {addressesQuery.data!.map((address) => (
                <div
                  key={address.id}
                  className="flex items-start justify-between gap-3 rounded-lg border border-[var(--color-border-secondary)] px-3 py-2"
                >
                  <div className="text-[11px]">
                    <div className="flex items-center gap-2">
                      <span className="text-[12px] font-semibold">{address.receiverName}</span>
                      <span className="text-[var(--color-text-secondary)]">{address.phone}</span>
                      {address.isDefault && <Badge variant="success">默认</Badge>}
                    </div>
                    <p className="mt-1 text-[var(--color-text-secondary)]">
                      {[address.province, address.city, address.district, address.detailAddress]
                        .filter(Boolean)
                        .join(' ')}
                    </p>
                    <div className="mt-1 flex gap-2">
                      {!address.isDefault && (
                        <button
                          type="button"
                          className="text-[10px] text-[var(--color-primary-text)] hover:underline"
                          onClick={() => setDefault.mutate(address.id)}
                        >
                          设为默认
                        </button>
                      )}
                      <button
                        type="button"
                        className="text-[10px] text-[var(--color-error-text)] hover:underline"
                        onClick={() => removeAddress.mutate(address.id)}
                      >
                        删除
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>

      <Dialog open={profileOpen} onOpenChange={setProfileOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>编辑资料</DialogTitle>
            <DialogDescription>{member.memberNo}</DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1">
              <Label className="text-xs">昵称</Label>
              <Input
                value={profileForm.nickname}
                onChange={(event) =>
                  setProfileForm((prev) => ({ ...prev, nickname: event.target.value }))
                }
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">头像地址</Label>
              <Input
                value={profileForm.avatar}
                onChange={(event) =>
                  setProfileForm((prev) => ({ ...prev, avatar: event.target.value }))
                }
                placeholder="https://..."
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">性别</Label>
              <select
                value={profileForm.gender}
                onChange={(event) =>
                  setProfileForm((prev) => ({ ...prev, gender: event.target.value }))
                }
                className="h-9 w-full rounded-lg border border-[var(--color-border-secondary)] bg-white px-3 text-sm"
              >
                {Object.entries(GENDER_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1">
              <Label className="text-xs">生日</Label>
              <Input
                type="date"
                value={profileForm.birthday}
                onChange={(event) =>
                  setProfileForm((prev) => ({ ...prev, birthday: event.target.value }))
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setProfileOpen(false)}>
              取消
            </Button>
            <Button disabled={updateProfile.isPending} onClick={() => updateProfile.mutate()}>
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={addressOpen} onOpenChange={setAddressOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>新增收货地址</DialogTitle>
            <DialogDescription>{member.userId}</DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            {[
              ['receiverName', '收货人'],
              ['phone', '手机号'],
              ['province', '省份'],
              ['city', '城市'],
              ['district', '区县'],
              ['detailAddress', '详细地址'],
            ].map(([key, label]) => (
              <div key={key} className="space-y-1">
                <Label className="text-xs">{label}</Label>
                <Input
                  value={addressForm[key as keyof typeof addressForm]}
                  onChange={(event) =>
                    setAddressForm((prev) => ({ ...prev, [key]: event.target.value }))
                  }
                />
              </div>
            ))}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAddressOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                createAddress.isPending ||
                !addressForm.receiverName.trim() ||
                !addressForm.phone.trim()
              }
              onClick={() => createAddress.mutate()}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
