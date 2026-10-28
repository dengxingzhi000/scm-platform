'use client'
import { useCallback, useMemo, useState, type ReactNode } from 'react'
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from '@tanstack/react-table'
import { Check, Eye, Search, X } from 'lucide-react'

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
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
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
  RefundQueryRequest,
  RefundResponse,
  RefundStatus,
} from '@/lib/api/contracts/order'
import { formatDateTime } from '../lib/mappers'
import {
  useApproveRefund,
  useCompleteRefund,
  useRefundDetail,
  useRefundItems,
  useRefundList,
  useRejectRefund,
} from '../hooks/use-refunds'

const PAGE_SIZE = 20
const ALL = '__all__'

const features = tableFeatures({})
const columnHelper = createColumnHelper<typeof features, RefundResponse>()
const emptyRows: RefundResponse[] = []

type BadgeVariant = 'success' | 'destructive' | 'secondary' | 'warning' | 'info'

const statusConfig: Record<RefundStatus, { label: string; variant: BadgeVariant }> = {
  PENDING: { label: '待审核', variant: 'warning' },
  APPROVED: { label: '已批准', variant: 'info' },
  REJECTED: { label: '已驳回', variant: 'destructive' },
  COMPLETED: { label: '已完成', variant: 'success' },
}

/** `ord_refund.refund_type`: 1-仅退款, 2-退货退款 (per scm-order schema comments). */
const refundTypeLabel = (code?: number | null) =>
  code === 1 ? '仅退款' : code === 2 ? '退货退款' : '-'

const money = (value?: number | null) =>
  `¥${(value ?? 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`

const NUMERIC_COLUMNS = new Set(['refundAmount'])

type HandlerAction = 'approve' | 'reject'

function DetailField({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="min-w-0">
      <div className="text-muted-foreground text-xs">{label}</div>
      <div className="mt-0.5 truncate">{children}</div>
    </div>
  )
}

export default function RefundManage() {
  const [pageNum, setPageNum] = useState(1)
  const [refundNoDraft, setRefundNoDraft] = useState('')
  const [orderIdDraft, setOrderIdDraft] = useState('')
  const [refundNo, setRefundNo] = useState('')
  const [orderId, setOrderId] = useState('')
  const [status, setStatus] = useState<RefundStatus>()
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')

  const [detailId, setDetailId] = useState<string | null>(null)
  const [handlerTarget, setHandlerTarget] = useState<{
    action: HandlerAction
    refund: RefundResponse
  } | null>(null)
  const [handlerId, setHandlerId] = useState('')
  const [handlerName, setHandlerName] = useState('')
  const [handlerRemark, setHandlerRemark] = useState('')
  const [completeTarget, setCompleteTarget] = useState<RefundResponse | null>(null)

  const request = useMemo<RefundQueryRequest>(
    () => ({
      pageNum,
      pageSize: PAGE_SIZE,
      refundNo: refundNo || undefined,
      orderId: orderId || undefined,
      status,
      startTime: startDate || undefined,
      endTime: endDate || undefined,
    }),
    [pageNum, refundNo, orderId, status, startDate, endDate],
  )

  const { data, isLoading } = useRefundList(request)
  const { data: detail } = useRefundDetail(detailId)
  const { data: detailItems } = useRefundItems(detailId)

  const approve = useApproveRefund()
  const reject = useRejectRefund()
  const complete = useCompleteRefund()

  const openHandler = useCallback((action: HandlerAction, refund: RefundResponse) => {
    setHandlerId('')
    setHandlerName('')
    setHandlerRemark('')
    setHandlerTarget({ action, refund })
  }, [])

  const columns = useMemo(
    () =>
      columnHelper.columns([
        columnHelper.accessor('refundNo', {
          header: '退款单号',
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
        columnHelper.accessor('refundType', {
          header: '类型',
          cell: (info) => refundTypeLabel(info.getValue()),
        }),
        columnHelper.accessor('reason', {
          header: '退款原因',
          cell: (info) => info.getValue() ?? '-',
        }),
        columnHelper.accessor('refundAmount', {
          header: '退款金额',
          cell: (info) => (
            <span className="font-medium">{money(info.getValue())}</span>
          ),
        }),
        columnHelper.accessor('refundStatus', {
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
          header: '申请时间',
          cell: (info) => formatDateTime(info.getValue()),
        }),
        columnHelper.display({
          id: 'actions',
          header: '操作',
          cell: ({ row }) => {
            const refund = row.original
            const refundStatus = refund.refundStatus
            return (
              <div className="flex items-center gap-1">
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label="详情"
                      onClick={() => setDetailId(refund.id)}
                    >
                      <Eye />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>详情</TooltipContent>
                </Tooltip>
                {refundStatus === 'PENDING' && (
                  <>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="审核通过"
                          className="text-[var(--color-success-text)]"
                          onClick={() => openHandler('approve', refund)}
                        >
                          <Check />
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent>审核通过</TooltipContent>
                    </Tooltip>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="驳回"
                          className="text-destructive hover:text-destructive"
                          onClick={() => openHandler('reject', refund)}
                        >
                          <X />
                        </Button>
                      </TooltipTrigger>
                      <TooltipContent>驳回</TooltipContent>
                    </Tooltip>
                  </>
                )}
                {refundStatus === 'APPROVED' && (
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setCompleteTarget(refund)}
                      >
                        已完成
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>标记退款已完成</TooltipContent>
                  </Tooltip>
                )}
              </div>
            )
          },
        }),
      ]),
    [openHandler],
  )

  const table = useTable({
    features,
    columns,
    data: data?.records ?? emptyRows,
  })

  const total = data?.total ?? 0
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE))

  const applySearch = () => {
    setRefundNo(refundNoDraft.trim())
    setOrderId(orderIdDraft.trim())
    setPageNum(1)
  }

  const isReject = handlerTarget?.action === 'reject'
  const handlerPending = isReject ? reject.isPending : approve.isPending

  const submitHandler = () => {
    if (!handlerTarget) return
    const trimmedId = handlerId.trim()
    const trimmedRemark = handlerRemark.trim()
    if (!trimmedId) return
    if (isReject && !trimmedRemark) return

    const common = {
      handlerId: trimmedId,
      handlerName: handlerName.trim() || undefined,
    }
    if (isReject) {
      reject.mutate(
        {
          id: handlerTarget.refund.id,
          request: { ...common, handlerRemark: trimmedRemark },
        },
        { onSettled: () => setHandlerTarget(null) },
      )
    } else {
      approve.mutate(
        {
          id: handlerTarget.refund.id,
          request: { ...common, handlerRemark: trimmedRemark || undefined },
        },
        { onSettled: () => setHandlerTarget(null) },
      )
    }
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="订单履约"
        title="退款管理"
        description="跟踪退款单审核与退款进度"
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
              value={refundNoDraft}
              onChange={(event) => setRefundNoDraft(event.target.value)}
              placeholder="退款单号"
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
                setStatus(value === ALL ? undefined : (value as RefundStatus))
                setPageNum(1)
              }}
            >
              <SelectTrigger size="sm" className="w-32 text-xs">
                <span className="text-muted-foreground">状态</span>
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>全部状态</SelectItem>
                {(Object.keys(statusConfig) as RefundStatus[]).map((key) => (
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
          <TableEmpty>暂无退款记录</TableEmpty>
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
                        header.column.id === 'refundStatus' && 'text-center',
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
                        cell.column.id === 'refundStatus' && 'text-center',
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

      {/* 详情 Dialog: loads getRefund + listRefundItems keyed by detailId. */}
      <Dialog open={!!detailId} onOpenChange={(open) => !open && setDetailId(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>退款详情</DialogTitle>
            <DialogDescription>
              {detail?.refundNo ?? '正在加载退款单…'}
            </DialogDescription>
          </DialogHeader>

          {detail ? (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
                <DetailField label="退款单号">{detail.refundNo ?? '-'}</DetailField>
                <DetailField label="订单号">{detail.orderNo ?? '-'}</DetailField>
                <DetailField label="退款类型">{refundTypeLabel(detail.refundType)}</DetailField>
                <DetailField label="退款金额">{money(detail.refundAmount)}</DetailField>
                <DetailField label="状态">
                  {detail.refundStatus ? (
                    <Badge variant={statusConfig[detail.refundStatus].variant}>
                      {statusConfig[detail.refundStatus].label}
                    </Badge>
                  ) : (
                    '-'
                  )}
                </DetailField>
                <DetailField label="申请时间">{formatDateTime(detail.createTime)}</DetailField>
                <DetailField label="退款原因">{detail.reason ?? '-'}</DetailField>
                <DetailField label="详细描述">{detail.description ?? '-'}</DetailField>
                <DetailField label="处理人">
                  {detail.handlerName || detail.handlerId || '-'}
                </DetailField>
                <DetailField label="处理备注">{detail.handlerRemark ?? '-'}</DetailField>
                <DetailField label="退回单号">{detail.returnWaybillNo ?? '-'}</DetailField>
                <DetailField label="退回承运">
                  {detail.returnCarrier ?? '-'}
                </DetailField>
                <DetailField label="批准时间">{formatDateTime(detail.approvedAt)}</DetailField>
                <DetailField label="驳回时间">{formatDateTime(detail.rejectedAt)}</DetailField>
                <DetailField label="完成时间">{formatDateTime(detail.completedAt)}</DetailField>
                <DetailField label="更新时间">{formatDateTime(detail.updateTime)}</DetailField>
              </div>

              <Separator />

              <div>
                <p className="mb-2 text-sm font-medium">退款明细</p>
                <div className="rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>商品名称</TableHead>
                        <TableHead>SKU 编码</TableHead>
                        <TableHead className="text-center">数量</TableHead>
                        <TableHead className="text-right">退款金额</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {(detailItems ?? []).length === 0 ? (
                        <TableRow>
                          <TableCell
                            colSpan={4}
                            className="text-muted-foreground h-16 text-center"
                          >
                            {detailItems ? '无退款明细' : '加载中…'}
                          </TableCell>
                        </TableRow>
                      ) : (
                        detailItems?.map((item) => (
                          <TableRow key={item.id}>
                            <TableCell>{item.skuName ?? '-'}</TableCell>
                            <TableCell className="font-mono text-xs">
                              {item.skuCode ?? '-'}
                            </TableCell>
                            <TableCell className="text-center">
                              {item.quantity ?? '-'}
                            </TableCell>
                            <TableCell className="text-right">
                              {money(item.refundAmount)}
                            </TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </div>
              </div>
            </div>
          ) : (
            <Skeleton className="h-48 w-full" />
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setDetailId(null)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 审核通过 / 驳回 handler Dialog. */}
      <Dialog
        open={!!handlerTarget}
        onOpenChange={(open) => !open && setHandlerTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {isReject ? '驳回退款' : '审核通过退款'}
            </DialogTitle>
            <DialogDescription>
              退款单 {handlerTarget?.refund.refundNo ?? ''} ·{' '}
              {money(handlerTarget?.refund.refundAmount)}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="refund-handler-id">
                处理人 ID<span className="text-destructive ml-1">*</span>
              </Label>
              <Input
                id="refund-handler-id"
                value={handlerId}
                onChange={(event) => setHandlerId(event.target.value)}
                placeholder="操作人账号 ID（必填）"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="refund-handler-name">处理人姓名</Label>
              <Input
                id="refund-handler-name"
                value={handlerName}
                onChange={(event) => setHandlerName(event.target.value)}
                placeholder="选填"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="refund-handler-remark">
                处理备注
                {isReject && <span className="text-destructive ml-1">*</span>}
              </Label>
              <Textarea
                id="refund-handler-remark"
                value={handlerRemark}
                onChange={(event) => setHandlerRemark(event.target.value)}
                placeholder={isReject ? '请填写驳回理由（必填）' : '选填'}
                rows={3}
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setHandlerTarget(null)}>
              取消
            </Button>
            <Button
              variant={isReject ? 'destructive' : 'default'}
              disabled={
                handlerPending ||
                !handlerId.trim() ||
                (isReject && !handlerRemark.trim())
              }
              onClick={submitHandler}
            >
              {isReject ? '确认驳回' : '确认通过'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 已完成 confirm Dialog. */}
      <Dialog
        open={!!completeTarget}
        onOpenChange={(open) => !open && setCompleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>标记退款完成</DialogTitle>
            <DialogDescription>
              确认退款单 {completeTarget?.refundNo ?? ''} 已完成？此操作将结束该退款流程。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCompleteTarget(null)}>
              取消
            </Button>
            <Button
              disabled={complete.isPending}
              onClick={() => {
                if (!completeTarget) return
                complete.mutate(completeTarget.id, {
                  onSettled: () => setCompleteTarget(null),
                })
              }}
            >
              确认完成
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
