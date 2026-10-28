'use client'
import { useMemo, useState } from 'react'
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from '@tanstack/react-table'
import { Plus, RefreshCw, Search } from 'lucide-react'

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
import { PageHeader } from '@/components/ui/page-header'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  TableEmpty,
  TableLoading,
  TablePanel,
  TablePagination,
  TableToolbar,
  TableToolbarEnd,
} from '@/components/ui/table-shell'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import { cn } from '@/lib/utils'
import type {
  PaymentQueryRequest,
  PaymentResponse,
  PaymentStatus,
} from '@/lib/api/contracts/order'
import { formatDateTime } from '../lib/mappers'
import {
  useCreatePayment,
  usePaymentList,
  useUpdatePaymentStatus,
} from '../hooks/use-payments'

const PAGE_SIZE = 20
const ALL = '__all__'

const features = tableFeatures({})
const columnHelper = createColumnHelper<typeof features, PaymentResponse>()
const emptyRows: PaymentResponse[] = []

type BadgeVariant = 'success' | 'destructive' | 'secondary' | 'warning' | 'info'

const statusConfig: Record<PaymentStatus, { label: string; variant: BadgeVariant }> = {
  PENDING: { label: '待支付', variant: 'secondary' },
  PROCESSING: { label: '处理中', variant: 'warning' },
  SUCCESS: { label: '支付成功', variant: 'success' },
  FAILED: { label: '支付失败', variant: 'destructive' },
  REFUNDING: { label: '退款中', variant: 'warning' },
  REFUNDED: { label: '已退款', variant: 'info' },
  CANCELLED: { label: '已取消', variant: 'secondary' },
}

const statusOptions = Object.keys(statusConfig) as PaymentStatus[]

const NUMERIC_COLUMNS = new Set(['paymentAmount'])

/** `ord_order.payment_method`: 1-支付宝, 2-微信, 3-银行卡, 4-余额, 5-货到付款 (per scm-order schema). */
const paymentMethodOptions = [
  { value: 1, label: '支付宝' },
  { value: 2, label: '微信' },
  { value: 3, label: '银行卡' },
  { value: 4, label: '余额' },
  { value: 5, label: '货到付款' },
]

const paymentMethodLabel = (code?: number | null) =>
  paymentMethodOptions.find((option) => option.value === code)?.label ?? '-'

const money = (value?: number | null) =>
  `¥${(value ?? 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`

interface CreateForm {
  paymentNo: string
  orderId: string
  orderNo: string
  userId: string
  paymentMethod?: number
  paymentChannel: string
  paymentAmount: string
  remark: string
}

const emptyCreateForm: CreateForm = {
  paymentNo: '',
  orderId: '',
  orderNo: '',
  userId: '',
  paymentMethod: undefined,
  paymentChannel: '',
  paymentAmount: '',
  remark: '',
}

export default function PaymentManage() {
  const [pageNum, setPageNum] = useState(1)
  const [paymentNoDraft, setPaymentNoDraft] = useState('')
  const [orderIdDraft, setOrderIdDraft] = useState('')
  const [paymentNo, setPaymentNo] = useState('')
  const [orderId, setOrderId] = useState('')
  const [status, setStatus] = useState<PaymentStatus>()
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  const [createOpen, setCreateOpen] = useState(false)
  const [createForm, setCreateForm] = useState<CreateForm>(emptyCreateForm)

  const [statusTarget, setStatusTarget] = useState<PaymentResponse | null>(null)
  const [targetStatus, setTargetStatus] = useState<PaymentStatus>()
  const [reason, setReason] = useState('')

  const request = useMemo<PaymentQueryRequest>(
    () => ({
      pageNum,
      pageSize: PAGE_SIZE,
      paymentNo: paymentNo || undefined,
      orderId: orderId || undefined,
      status,
      startTime: startDate || undefined,
      endTime: endDate || undefined,
    }),
    [pageNum, paymentNo, orderId, status, startDate, endDate],
  )

  const { data, isLoading } = usePaymentList(request)
  const createPayment = useCreatePayment()
  const updateStatus = useUpdatePaymentStatus()

  const amountNumber = Number(createForm.paymentAmount)
  const canCreate = Boolean(
    createForm.paymentNo.trim() &&
      createForm.orderId.trim() &&
      createForm.orderNo.trim() &&
      createForm.userId.trim() &&
      createForm.paymentMethod !== undefined &&
      createForm.paymentAmount.trim() !== '' &&
      !Number.isNaN(amountNumber) &&
      amountNumber >= 0,
  )

  const openCreate = () => {
    setCreateForm(emptyCreateForm)
    setCreateOpen(true)
  }

  const submitCreate = () => {
    if (!canCreate) return
    createPayment.mutate(
      {
        paymentNo: createForm.paymentNo.trim(),
        orderId: createForm.orderId.trim(),
        orderNo: createForm.orderNo.trim(),
        userId: createForm.userId.trim(),
        paymentMethod: createForm.paymentMethod as number,
        paymentChannel: createForm.paymentChannel.trim() || undefined,
        paymentAmount: amountNumber,
        remark: createForm.remark.trim() || undefined,
      },
      { onSuccess: () => setCreateOpen(false) },
    )
  }

  const openStatusDialog = (payment: PaymentResponse) => {
    setStatusTarget(payment)
    setTargetStatus(undefined)
    setReason('')
  }

  const submitStatus = () => {
    if (!statusTarget || !targetStatus) return
    updateStatus.mutate(
      {
        id: statusTarget.id,
        targetStatus,
        reason: reason.trim() || undefined,
      },
      { onSettled: () => setStatusTarget(null) },
    )
  }

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor('paymentNo', {
          header: '支付单号',
          cell: (info) => (
            <span className="font-mono text-xs">{info.getValue() ?? '-'}</span>
          ),
        }),
        columnHelper.accessor('orderNo', {
          header: '订单号',
          cell: (info) => (
            <span className="font-mono text-xs">{info.getValue() ?? '-'}</span>
          ),
        }),
        columnHelper.accessor('paymentMethod', {
          header: '支付方式',
          cell: (info) => paymentMethodLabel(info.getValue()),
        }),
        columnHelper.accessor('paymentAmount', {
          header: '支付金额',
          cell: (info) => (
            <span className="font-medium">{money(info.getValue())}</span>
          ),
        }),
        columnHelper.accessor('paymentStatus', {
          header: '状态',
          cell: (info) => {
            const value = info.getValue()
            const config = value ? statusConfig[value] : undefined
            return config ? (
              <Badge variant={config.variant}>{config.label}</Badge>
            ) : (
              value ?? '-'
            )
          },
        }),
        columnHelper.accessor('createTime', {
          header: '创建时间',
          cell: (info) => formatDateTime(info.getValue()),
        }),
        columnHelper.display({
          id: 'actions',
          header: '操作',
          cell: ({ row }) => (
            <div className="flex items-center gap-1">
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => openStatusDialog(row.original)}
                  >
                    <RefreshCw />
                    更新状态
                  </Button>
                </TooltipTrigger>
                <TooltipContent>更新支付状态</TooltipContent>
              </Tooltip>
            </div>
          ),
        }),
      ]),
    // openStatusDialog only touches stable state setters → safe to leave out of deps.
    [],
  )

  const table = useTable({
    features,
    columns,
    data: data?.records ?? emptyRows,
  })

  const total = data?.total ?? 0
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE))

  const applySearch = () => {
    setPaymentNo(paymentNoDraft.trim())
    setOrderId(orderIdDraft.trim())
    setPageNum(1)
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="订单履约"
        title="支付管理"
        description="查询支付单状态、渠道流水与金额"
        actions={
          <Button onClick={openCreate}>
            <Plus />
            新建支付
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <form
            onSubmit={(event) => {
              event.preventDefault()
              applySearch()
            }}
            className="flex items-center gap-2"
          >
            <Input
              value={paymentNoDraft}
              onChange={(event) => setPaymentNoDraft(event.target.value)}
              placeholder="支付单号"
              className="h-8 w-40 text-xs"
            />
            <Input
              value={orderIdDraft}
              onChange={(event) => setOrderIdDraft(event.target.value)}
              placeholder="订单 ID"
              className="h-8 w-40 text-xs"
            />
            <Button type="submit" variant="outline" size="icon" className="size-8" aria-label="查询">
              <Search />
            </Button>
          </form>

          <TableToolbarEnd>
            <Select
              value={status ?? ALL}
              onValueChange={(value) => {
                setStatus(value === ALL ? undefined : (value as PaymentStatus))
                setPageNum(1)
              }}
            >
              <SelectTrigger size="sm" className="w-32 text-xs">
                <span className="text-muted-foreground">状态</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部状态</SelectItem>
                {statusOptions.map((key) => (
                  <SelectItem key={key} value={key}>
                    {statusConfig[key].label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <div className="flex items-center gap-1.5">
              <Input
                type="date"
                value={startDate}
                onChange={(event) => {
                  setStartDate(event.target.value)
                  setPageNum(1)
                }}
                className="h-8 w-36 text-xs"
                aria-label="开始日期"
              />
              <span className="text-muted-foreground text-[11px]">至</span>
              <Input
                type="date"
                value={endDate}
                onChange={(event) => {
                  setEndDate(event.target.value)
                  setPageNum(1)
                }}
                className="h-8 w-36 text-xs"
                aria-label="结束日期"
              />
            </div>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : table.getRowModel().rows.length === 0 ? (
          <TableEmpty>暂无支付记录</TableEmpty>
        ) : (
          <Table containerClassName="max-h-[calc(100vh-320px)] overflow-y-auto">
            <TableHeader>
              {table.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      numeric={NUMERIC_COLUMNS.has(header.column.id)}
                      className={cn(
                        header.column.id === 'paymentStatus' && 'text-center',
                        header.column.id === 'actions' && 'text-right',
                      )}
                    >
                      {header.isPlaceholder ? null : (
                        <table.FlexRender header={header} />
                      )}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {table.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      numeric={NUMERIC_COLUMNS.has(cell.column.id)}
                      className={cn(
                        cell.column.id === 'paymentStatus' && 'text-center',
                        cell.column.id === 'actions' && 'text-right',
                      )}
                    >
                      <table.FlexRender cell={cell} />
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        <TablePagination
          page={pageNum}
          pageSize={PAGE_SIZE}
          total={total}
          pageCount={pageCount}
          onPageChange={setPageNum}
        />
      </TablePanel>

      {/* 新建支付 Dialog — every PaymentCreateRequest field is user-provided. */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>新建支付</DialogTitle>
            <DialogDescription>
              创建一笔支付单。订单/用户 ID 为 UUID，支付金额为数值。
            </DialogDescription>
          </DialogHeader>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="pay-no">
                支付单号<span className="text-destructive ml-1">*</span>
              </Label>
              <Input
                id="pay-no"
                value={createForm.paymentNo}
                onChange={(event) =>
                  setCreateForm((form) => ({ ...form, paymentNo: event.target.value }))
                }
                placeholder="唯一支付单号"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="pay-amount">
                支付金额<span className="text-destructive ml-1">*</span>
              </Label>
              <Input
                id="pay-amount"
                type="number"
                min={0}
                step="0.01"
                value={createForm.paymentAmount}
                onChange={(event) =>
                  setCreateForm((form) => ({
                    ...form,
                    paymentAmount: event.target.value,
                  }))
                }
                placeholder="0.00"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="pay-order-id">
                订单 ID<span className="text-destructive ml-1">*</span>
              </Label>
              <Input
                id="pay-order-id"
                value={createForm.orderId}
                onChange={(event) =>
                  setCreateForm((form) => ({ ...form, orderId: event.target.value }))
                }
                placeholder="订单 UUID"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="pay-order-no">
                订单号<span className="text-destructive ml-1">*</span>
              </Label>
              <Input
                id="pay-order-no"
                value={createForm.orderNo}
                onChange={(event) =>
                  setCreateForm((form) => ({ ...form, orderNo: event.target.value }))
                }
                placeholder="业务订单号"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="pay-user-id">
                用户 ID<span className="text-destructive ml-1">*</span>
              </Label>
              <Input
                id="pay-user-id"
                value={createForm.userId}
                onChange={(event) =>
                  setCreateForm((form) => ({ ...form, userId: event.target.value }))
                }
                placeholder="用户 UUID"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="pay-channel">支付渠道</Label>
              <Input
                id="pay-channel"
                value={createForm.paymentChannel}
                onChange={(event) =>
                  setCreateForm((form) => ({
                    ...form,
                    paymentChannel: event.target.value,
                  }))
                }
                placeholder="选填，如 ALIPAY"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="pay-method">
                支付方式<span className="text-destructive ml-1">*</span>
              </Label>
              <Select
                value={
                  createForm.paymentMethod === undefined
                    ? ''
                    : String(createForm.paymentMethod)
                }
                onValueChange={(value) =>
                  setCreateForm((form) => ({
                    ...form,
                    paymentMethod: Number(value),
                  }))
                }
              >
                <SelectTrigger id="pay-method" className="w-full">
                  <SelectValue placeholder="选择支付方式" />
                </SelectTrigger>
                <SelectContent>
                  {paymentMethodOptions.map((option) => (
                    <SelectItem key={option.value} value={String(option.value)}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="col-span-2 space-y-2">
              <Label htmlFor="pay-remark">备注</Label>
              <Textarea
                id="pay-remark"
                value={createForm.remark}
                onChange={(event) =>
                  setCreateForm((form) => ({ ...form, remark: event.target.value }))
                }
                rows={2}
                placeholder="选填"
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              取消
            </Button>
            <Button disabled={!canCreate || createPayment.isPending} onClick={submitCreate}>
              创建
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 更新状态 Dialog — target state machine enforced server-side. */}
      <Dialog
        open={!!statusTarget}
        onOpenChange={(open) => !open && setStatusTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>更新支付状态</DialogTitle>
            <DialogDescription>
              支付单 {statusTarget?.paymentNo ?? ''} · 当前状态{' '}
              {statusTarget?.paymentStatus
                ? statusConfig[statusTarget.paymentStatus].label
                : '-'}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="pay-target-status">
                目标状态<span className="text-destructive ml-1">*</span>
              </Label>
              <Select
                value={targetStatus ?? ''}
                onValueChange={(value) => setTargetStatus(value as PaymentStatus)}
              >
                <SelectTrigger id="pay-target-status" className="w-full">
                  <SelectValue placeholder="选择目标状态" />
                </SelectTrigger>
                <SelectContent>
                  {statusOptions
                    .filter((key) => key !== statusTarget?.paymentStatus)
                    .map((key) => (
                      <SelectItem key={key} value={key}>
                        {statusConfig[key].label}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
              <p className="text-muted-foreground text-xs">
                非法状态流转将由服务端拦截并返回错误提示。
              </p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="pay-reason">变更原因</Label>
              <Textarea
                id="pay-reason"
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                rows={3}
                placeholder="选填"
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setStatusTarget(null)}>
              取消
            </Button>
            <Button
              disabled={!targetStatus || updateStatus.isPending}
              onClick={submitStatus}
            >
              确认更新
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
