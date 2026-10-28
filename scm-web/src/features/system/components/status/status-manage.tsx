"use client";

import { useMemo, useState, type ReactNode } from "react";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import {
  ArrowRight,
  GitBranch,
  Pencil,
  Plus,
  RefreshCw,
  SearchCheck,
  Trash2,
  Zap,
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardAction,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/ui/page-header";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import type {
  StatusDictDTO,
  StatusTransitionDTO,
} from "@/lib/api/contracts/system";
import {
  BIZ_TYPES,
  useAvailableActions,
  useCreateStatusDict,
  useCreateStatusTransition,
  useDeleteStatusDict,
  useDeleteStatusTransition,
  useNextStatuses,
  useStatusDictList,
  useStatusTransitionList,
  useStatusTransitionPreview,
  useTransitionCheck,
  useUpdateStatusDict,
  useUpdateStatusTransition,
} from "../../hooks/use-status-dict";

const features = tableFeatures({});
const CUSTOM_OPTION = "__custom__";
const NONE_OPTION = "__none__";
const dictColumnHelper = createColumnHelper<typeof features, StatusDictDTO>();
const transitionColumnHelper = createColumnHelper<
  typeof features,
  StatusTransitionDTO
>();
const emptyDictRows: StatusDictDTO[] = [];
const emptyTransitionRows: StatusTransitionDTO[] = [];

interface DictForm {
  id?: string;
  statusCode: string;
  statusName: string;
  statusNameEn: string;
  color: string;
  icon: string;
  sortOrder: string;
  isInitial: boolean;
  isTerminal: boolean;
  isCancellable: boolean;
  enabled: boolean;
  remark: string;
}

const emptyDictForm: DictForm = {
  statusCode: "",
  statusName: "",
  statusNameEn: "",
  color: "",
  icon: "",
  sortOrder: "",
  isInitial: false,
  isTerminal: false,
  isCancellable: false,
  enabled: true,
  remark: "",
};

interface TransitionForm {
  id?: string;
  fromStatus: string;
  toStatus: string;
  actionCode: string;
  actionName: string;
  actionNameEn: string;
  conditionExpression: string;
  preAction: string;
  postAction: string;
  sortOrder: string;
  needApproval: boolean;
  enabled: boolean;
  remark: string;
}

const emptyTransitionForm: TransitionForm = {
  fromStatus: "",
  toStatus: "",
  actionCode: "",
  actionName: "",
  actionNameEn: "",
  conditionExpression: "",
  preAction: "",
  postAction: "",
  sortOrder: "",
  needApproval: false,
  enabled: true,
  remark: "",
};

const isBlank = (value: string) => value.trim() === "";

function toDictPayload(form: DictForm, bizType: string): StatusDictDTO {
  return {
    id: form.id ?? "",
    bizType,
    statusCode: form.statusCode.trim(),
    statusName: form.statusName.trim(),
    statusNameEn: isBlank(form.statusNameEn) ? undefined : form.statusNameEn.trim(),
    color: isBlank(form.color) ? undefined : form.color.trim(),
    icon: isBlank(form.icon) ? undefined : form.icon.trim(),
    sortOrder: isBlank(form.sortOrder) ? undefined : Number(form.sortOrder),
    isInitial: form.isInitial,
    isTerminal: form.isTerminal,
    isCancellable: form.isCancellable,
    enabled: form.enabled,
    remark: isBlank(form.remark) ? undefined : form.remark.trim(),
  };
}

function toTransitionPayload(
  form: TransitionForm,
  bizType: string,
): StatusTransitionDTO {
  return {
    id: form.id ?? "",
    bizType,
    fromStatus: form.fromStatus.trim(),
    toStatus: form.toStatus.trim(),
    actionCode: form.actionCode.trim(),
    actionName: isBlank(form.actionName) ? undefined : form.actionName.trim(),
    actionNameEn: isBlank(form.actionNameEn)
      ? undefined
      : form.actionNameEn.trim(),
    needApproval: form.needApproval,
    enabled: form.enabled,
    conditionExpression: isBlank(form.conditionExpression)
      ? undefined
      : form.conditionExpression.trim(),
    preAction: isBlank(form.preAction) ? undefined : form.preAction.trim(),
    postAction: isBlank(form.postAction) ? undefined : form.postAction.trim(),
    sortOrder: isBlank(form.sortOrder) ? undefined : Number(form.sortOrder),
    remark: isBlank(form.remark) ? undefined : form.remark.trim(),
  };
}

function ColorSwatch({ color }: { color?: string }) {
  if (!color) {
    return (
      <span className="bg-muted inline-block size-4 rounded-sm border" />
    );
  }
  return (
    <span
      className="inline-block size-4 rounded-sm border"
      style={{ backgroundColor: color }}
      title={color}
    />
  );
}

/** Data-driven chips: the colour comes from the dict row, so it is inline by nature. */
function StatusChip({ row }: { row: StatusDictDTO }) {
  const text = `${row.statusCode} ${row.statusName}`;
  if (!row.color) {
    return (
      <Badge variant={row.enabled === false ? "secondary" : "default"}>
        {text}
      </Badge>
    );
  }
  return (
    <Badge
      variant="outline"
      style={{ borderColor: row.color, color: row.color }}
    >
      {text}
    </Badge>
  );
}

/** Canonical values live in the Select, but a tenant may add more, so the field stays free text. */
function ScopePicker({
  label,
  options,
  value,
  onChange,
  placeholder,
}: {
  label: string;
  options: readonly string[];
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
}) {
  const isCustom = !options.includes(value);
  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-muted-foreground text-sm">{label}</span>
      <Select
        value={isCustom ? CUSTOM_OPTION : value}
        onValueChange={(next) => {
          if (next !== CUSTOM_OPTION) onChange(next);
        }}
      >
        <SelectTrigger className="w-44">
          <SelectValue placeholder={placeholder} />
        </SelectTrigger>
        <SelectContent>
          {options.map((option) => (
            <SelectItem key={option} value={option}>
              {option}
            </SelectItem>
          ))}
          <SelectItem value={CUSTOM_OPTION}>自定义…</SelectItem>
        </SelectContent>
      </Select>
      <Input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className="w-44 font-mono text-xs"
        aria-label={`${label}（自定义）`}
      />
      {isCustom ? (
        <Badge variant="info">自定义 {label}</Badge>
      ) : null}
    </div>
  );
}

function Field({
  label,
  children,
  className,
}: {
  label: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={`space-y-1.5 ${className ?? ""}`}>
      <Label className="text-muted-foreground text-xs">{label}</Label>
      {children}
    </div>
  );
}

export default function StatusManage() {
  const [bizType, setBizType] = useState<string>("ORDER");

  const [dictForm, setDictForm] = useState<DictForm | null>(null);
  const [dictError, setDictError] = useState("");
  const [dictDeleteTarget, setDictDeleteTarget] =
    useState<StatusDictDTO | null>(null);

  const [transitionForm, setTransitionForm] = useState<TransitionForm | null>(
    null,
  );
  const [transitionError, setTransitionError] = useState("");
  const [transitionDeleteTarget, setTransitionDeleteTarget] =
    useState<StatusTransitionDTO | null>(null);

  const [previewStatus, setPreviewStatus] = useState("");
  const [checkFrom, setCheckFrom] = useState("");
  const [checkTo, setCheckTo] = useState("");

  const { data: dictRows, isLoading: dictLoading } =
    useStatusDictList(bizType);
  const { data: transitionRows, isLoading: transitionLoading } =
    useStatusTransitionList(bizType);

  const createDict = useCreateStatusDict(bizType);
  const updateDict = useUpdateStatusDict(bizType);
  const deleteDict = useDeleteStatusDict(bizType);
  const createTransition = useCreateStatusTransition(bizType);
  const updateTransition = useUpdateStatusTransition(bizType);
  const deleteTransition = useDeleteStatusTransition(bizType);

  const check = useTransitionCheck();
  const previewTransition = useStatusTransitionPreview();

  const { data: nextStatuses, isLoading: nextLoading } = useNextStatuses(
    bizType,
    previewStatus,
  );
  const { data: actions, isLoading: actionsLoading } = useAvailableActions(
    bizType,
    previewStatus,
    previewStatus !== "",
  );

  const statusOptions = useMemo(
    () => (dictRows ?? []).map((row) => row.statusCode),
    [dictRows],
  );

  const dictTable = useTable({
    features,
    columns: useMemo(
      () =>
        dictColumnHelper.columns([
          dictColumnHelper.display({
            id: "color",
            header: "颜色",
            cell: ({ row }) => <ColorSwatch color={row.original.color} />,
          }),
          dictColumnHelper.accessor("statusCode", {
            header: "状态码",
            cell: (info) => (
              <span className="font-mono text-xs">{info.getValue()}</span>
            ),
          }),
          dictColumnHelper.accessor("statusName", {
            header: "状态名称",
            cell: (info) => (
              <div className="flex items-center gap-1.5">
                {info.row.original.icon ? (
                  <span aria-hidden>{info.row.original.icon}</span>
                ) : null}
                <StatusChip row={info.row.original} />
                {info.getValue()}
              </div>
            ),
          }),
          dictColumnHelper.accessor("sortOrder", {
            header: "排序",
            cell: (info) => info.getValue() ?? "-",
          }),
          dictColumnHelper.display({
            id: "markers",
            header: "标记",
            cell: ({ row }) => (
              <div className="flex flex-wrap gap-1">
                {row.original.isInitial ? (
                  <Badge variant="info">初始</Badge>
                ) : null}
                {row.original.isTerminal ? (
                  <Badge variant="success">终态</Badge>
                ) : null}
                {row.original.isCancellable ? (
                  <Badge variant="warning">可取消</Badge>
                ) : null}
                {row.original.enabled === false ? (
                  <Badge variant="destructive">停用</Badge>
                ) : null}
              </div>
            ),
          }),
          dictColumnHelper.display({
            id: "actions",
            header: "操作",
            cell: ({ row }) => (
              <div className="flex items-center gap-1">
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label="编辑状态"
                      onClick={() => {
                        setDictError("");
                        setDictForm({
                          id: row.original.id,
                          statusCode: row.original.statusCode,
                          statusName: row.original.statusName,
                          statusNameEn: row.original.statusNameEn ?? "",
                          color: row.original.color ?? "",
                          icon: row.original.icon ?? "",
                          sortOrder:
                            row.original.sortOrder == null
                              ? ""
                              : String(row.original.sortOrder),
                          isInitial: !!row.original.isInitial,
                          isTerminal: !!row.original.isTerminal,
                          isCancellable: !!row.original.isCancellable,
                          enabled: row.original.enabled !== false,
                          remark: row.original.remark ?? "",
                        });
                      }}
                    >
                      <Pencil />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>编辑</TooltipContent>
                </Tooltip>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label="删除状态"
                      className="text-destructive hover:text-destructive"
                      onClick={() => setDictDeleteTarget(row.original)}
                    >
                      <Trash2 />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>删除</TooltipContent>
                </Tooltip>
              </div>
            ),
          }),
        ]),
      [],
    ),
    data: dictRows ?? emptyDictRows,
  });

  const transitionTable = useTable({
    features,
    columns: useMemo(
      () =>
        transitionColumnHelper.columns([
          transitionColumnHelper.display({
            id: "edge",
            header: "流转",
            cell: ({ row }) => (
              <div className="flex items-center gap-1.5 text-xs">
                <span className="font-mono">{row.original.fromStatus}</span>
                <ArrowRight className="text-muted-foreground size-3.5" />
                <span className="font-mono">{row.original.toStatus}</span>
              </div>
            ),
          }),
          transitionColumnHelper.accessor("actionCode", {
            header: "动作",
            cell: (info) => (
              <div className="leading-tight">
                <div className="font-mono text-xs">{info.getValue()}</div>
                <div className="text-muted-foreground text-xs">
                  {info.row.original.actionName ?? "-"}
                </div>
              </div>
            ),
          }),
          transitionColumnHelper.accessor("needApproval", {
            header: "需审批",
            cell: (info) =>
              info.getValue() ? (
                <Badge variant="warning">是</Badge>
              ) : (
                <Badge variant="secondary">否</Badge>
              ),
          }),
          transitionColumnHelper.accessor("enabled", {
            header: "启用",
            cell: (info) =>
              info.getValue() === false ? (
                <Badge variant="destructive">停用</Badge>
              ) : (
                <Badge variant="success">启用</Badge>
              ),
          }),
          transitionColumnHelper.accessor("conditionExpression", {
            header: "条件/动作",
            cell: (info) => (
              <div className="text-muted-foreground max-w-56 space-y-0.5 text-xs">
                {info.getValue() ? (
                  <div className="truncate font-mono" title={info.getValue()}>
                    条件：{info.getValue()}
                  </div>
                ) : null}
                {info.row.original.preAction || info.row.original.postAction ? (
                  <div className="truncate font-mono">
                    {info.row.original.preAction ?? ""}
                    {info.row.original.preAction && info.row.original.postAction
                      ? " → "
                      : ""}
                    {info.row.original.postAction ?? ""}
                  </div>
                ) : null}
                {!info.getValue() &&
                !info.row.original.preAction &&
                !info.row.original.postAction
                  ? "-"
                  : null}
              </div>
            ),
          }),
          transitionColumnHelper.accessor("sortOrder", {
            header: "排序",
            cell: (info) => info.getValue() ?? "-",
          }),
          transitionColumnHelper.display({
            id: "actions",
            header: "操作",
            cell: ({ row }) => (
              <div className="flex items-center gap-1">
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label="编辑流转"
                      onClick={() => {
                        setTransitionError("");
                        setTransitionForm({
                          id: row.original.id,
                          fromStatus: row.original.fromStatus,
                          toStatus: row.original.toStatus,
                          actionCode: row.original.actionCode,
                          actionName: row.original.actionName ?? "",
                          actionNameEn: row.original.actionNameEn ?? "",
                          conditionExpression:
                            row.original.conditionExpression ?? "",
                          preAction: row.original.preAction ?? "",
                          postAction: row.original.postAction ?? "",
                          sortOrder:
                            row.original.sortOrder == null
                              ? ""
                              : String(row.original.sortOrder),
                          needApproval: !!row.original.needApproval,
                          enabled: row.original.enabled !== false,
                          remark: row.original.remark ?? "",
                        });
                      }}
                    >
                      <Pencil />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>编辑</TooltipContent>
                </Tooltip>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label="删除流转"
                      className="text-destructive hover:text-destructive"
                      onClick={() => setTransitionDeleteTarget(row.original)}
                    >
                      <Trash2 />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>删除</TooltipContent>
                </Tooltip>
              </div>
            ),
          }),
        ]),
      [],
    ),
    data: transitionRows ?? emptyTransitionRows,
  });

  const submitDict = () => {
    if (!dictForm) return;
    if (isBlank(dictForm.statusCode) || isBlank(dictForm.statusName)) {
      setDictError("状态码与状态名称均为必填");
      return;
    }
    if (!isBlank(dictForm.sortOrder) && Number.isNaN(Number(dictForm.sortOrder))) {
      setDictError("排序必须是数字");
      return;
    }
    const payload = toDictPayload(dictForm, bizType);
    const isEdit = !!dictForm.id;
    (isEdit ? updateDict : createDict).mutate(payload, {
      onSuccess: () => setDictForm(null),
      onError: () => undefined,
    });
  };

  const submitTransition = () => {
    if (!transitionForm) return;
    if (
      isBlank(transitionForm.fromStatus) ||
      isBlank(transitionForm.toStatus) ||
      isBlank(transitionForm.actionCode)
    ) {
      setTransitionError("起始状态、目标状态与动作码均为必填");
      return;
    }
    if (
      !isBlank(transitionForm.sortOrder) &&
      Number.isNaN(Number(transitionForm.sortOrder))
    ) {
      setTransitionError("排序必须是数字");
      return;
    }
    const payload = toTransitionPayload(transitionForm, bizType);
    const isEdit = !!transitionForm.id;
    (isEdit ? updateTransition : createTransition).mutate(payload, {
      onSuccess: () => setTransitionForm(null),
      onError: () => undefined,
    });
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="系统管理"
        title="状态机管理"
        description="维护业务状态字典与合法流转规则"
        actions={
          <>
            <Badge variant="secondary">{bizType}</Badge>
            <ScopePicker
              label="业务类型"
              options={BIZ_TYPES}
              value={bizType}
              onChange={(next) => {
                if (next === bizType) return;
                setBizType(next);
                setPreviewStatus("");
                setCheckFrom("");
                setCheckTo("");
                setDictForm(null);
                setTransitionForm(null);
                check.reset();
                previewTransition.reset();
              }}
              placeholder="业务类型"
            />
          </>
        }
      />

      <Card className="gap-0 overflow-hidden py-0">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <GitBranch />
            状态字典
          </CardTitle>
          <CardAction>
            <Button
              size="sm"
              onClick={() => {
                setDictError("");
                setDictForm({ ...emptyDictForm });
              }}
            >
              <Plus />
              新增状态
            </Button>
          </CardAction>
        </CardHeader>
        {dictLoading ? (
          <div className="p-6">
            <Skeleton className="h-48 w-full" />
          </div>
        ) : dictTable.getRowModel().rows.length === 0 ? (
          <div className="text-muted-foreground py-12 text-center text-sm">
            该业务类型下暂无状态字典
          </div>
        ) : (
          <Table>
            <TableHeader>
              {dictTable.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead key={header.id}>
                      {header.isPlaceholder ? null : (
                        <dictTable.FlexRender header={header} />
                      )}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {dictTable.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell key={cell.id}>
                      <dictTable.FlexRender cell={cell} />
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        <div className="text-muted-foreground border-t px-4 py-3 text-sm">
          共 {(dictRows ?? emptyDictRows).length} 条（接口不分页，返回全量数组）
        </div>
      </Card>

      <Card className="gap-0 overflow-hidden py-0">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ArrowRight />
            流转规则
          </CardTitle>
          <CardAction>
            <Button
              size="sm"
              onClick={() => {
                setTransitionError("");
                setTransitionForm({ ...emptyTransitionForm });
              }}
            >
              <Plus />
              新增流转
            </Button>
          </CardAction>
        </CardHeader>
        {transitionLoading ? (
          <div className="p-6">
            <Skeleton className="h-48 w-full" />
          </div>
        ) : transitionTable.getRowModel().rows.length === 0 ? (
          <div className="text-muted-foreground py-12 text-center text-sm">
            该业务类型下暂无流转规则
          </div>
        ) : (
          <Table>
            <TableHeader>
              {transitionTable.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead key={header.id}>
                      {header.isPlaceholder ? null : (
                        <transitionTable.FlexRender header={header} />
                      )}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {transitionTable.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell key={cell.id}>
                      <transitionTable.FlexRender cell={cell} />
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        <div className="text-muted-foreground border-t px-4 py-3 text-sm">
          共 {(transitionRows ?? emptyTransitionRows).length} 条
        </div>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Zap />
            状态流转预览
          </CardTitle>
          <CardAction className="flex items-center gap-2">
            <Select
              value={previewStatus || NONE_OPTION}
              onValueChange={(next) => {
                if (next === NONE_OPTION) return;
                setPreviewStatus(next);
                previewTransition.reset();
              }}
            >
              <SelectTrigger className="w-48">
                <SelectValue placeholder="选择当前状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NONE_OPTION}>选择当前状态</SelectItem>
                {statusOptions.map((code) => (
                  <SelectItem key={code} value={code}>
                    {code}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button
              variant="outline"
              size="icon"
              aria-label="刷新预览"
              disabled={!previewStatus}
              onClick={() => {
                previewTransition.reset();
                check.reset();
              }}
            >
              <RefreshCw />
            </Button>
          </CardAction>
        </CardHeader>
        <CardContent className="space-y-6">
          {!previewStatus ? (
            <p className="text-muted-foreground text-sm">
              选择当前状态后，可查看该状态下的后继状态与可执行动作。
            </p>
          ) : (
            <div className="space-y-4">
              <div className="space-y-2">
                <span className="text-muted-foreground text-sm">后继状态</span>
                {nextLoading ? (
                  <Skeleton className="h-6 w-40" />
                ) : (nextStatuses ?? []).length === 0 ? (
                  <Badge variant="secondary">无后继状态</Badge>
                ) : (
                  <div className="flex flex-wrap gap-1.5">
                    {(nextStatuses ?? []).map((code) => (
                      <Badge key={code} variant="info">
                        {code}
                      </Badge>
                    ))}
                  </div>
                )}
              </div>

              <Separator />

              <div className="space-y-2">
                <span className="text-muted-foreground text-sm">
                  可用动作（{previewStatus}）
                </span>
                {actionsLoading ? (
                  <Skeleton className="h-24 w-full" />
                ) : (actions ?? []).length === 0 ? (
                  <p className="text-muted-foreground text-sm">
                    该状态下没有可用动作
                  </p>
                ) : (
                  <ul className="space-y-2">
                    {(actions ?? []).map((action) => (
                      <li
                        key={action.actionCode}
                        className="border-border bg-card flex flex-wrap items-center gap-2 rounded-md border px-3 py-2"
                      >
                        <span className="font-mono text-xs">
                          {action.actionCode}
                        </span>
                        <span className="text-sm">
                          {action.actionName ?? "-"}
                        </span>
                        {action.targetStatus ? (
                          <span className="text-muted-foreground flex items-center gap-1 text-xs">
                            <ArrowRight className="size-3" />
                            {action.targetStatusName ?? action.targetStatus}
                          </span>
                        ) : null}
                        {action.needApproval ? (
                          <Badge variant="warning">需审批</Badge>
                        ) : null}
                        {action.conditionExpression ? (
                          <Badge variant="secondary">有条件</Badge>
                        ) : null}
                        <Button
                          className="ml-auto"
                          variant="outline"
                          size="sm"
                          disabled={
                            previewTransition.isPending &&
                            previewTransition.variables?.actionCode ===
                              action.actionCode
                          }
                          onClick={() =>
                            previewTransition.mutate({
                              bizType,
                              fromStatus: previewStatus,
                              actionCode: action.actionCode,
                            })
                          }
                        >
                          执行流转
                        </Button>
                      </li>
                    ))}
                  </ul>
                )}
                {previewTransition.data ? (
                  <div className="border-border bg-muted/40 rounded-md border p-3 text-sm">
                    <div className="flex items-center gap-2">
                      <Badge
                        variant={
                          previewTransition.data.success ? "success" : "destructive"
                        }
                      >
                        {previewTransition.data.success ? "流转成功" : "流转失败"}
                      </Badge>
                      <span className="font-mono text-xs">
                        {previewTransition.data.fromStatus} →{" "}
                        {previewTransition.data.toStatus}
                      </span>
                      {previewTransition.data.needApproval ? (
                        <Badge variant="warning">需审批</Badge>
                      ) : null}
                    </div>
                    {previewTransition.data.postAction ? (
                      <p className="text-muted-foreground mt-1 font-mono text-xs">
                        后置动作：{previewTransition.data.postAction}
                      </p>
                    ) : null}
                    {previewTransition.data.errorMessage ? (
                      <p className="text-destructive mt-1 text-xs">
                        {previewTransition.data.errorMessage}
                      </p>
                    ) : null}
                    <p className="text-muted-foreground mt-1 text-xs">
                      注意：执行流转会调用后端 transition 接口，请按真实业务效果使用。
                    </p>
                  </div>
                ) : null}
              </div>

              <Separator />

              <div className="space-y-2">
                <span className="text-muted-foreground flex items-center gap-1.5 text-sm">
                  <SearchCheck />
                  流转校验器
                </span>
                <div className="flex flex-wrap items-end gap-2">
                  <Field label="起始状态">
                    <Input
                      value={checkFrom}
                      onChange={(event) => setCheckFrom(event.target.value)}
                      placeholder="fromStatus"
                      className="w-40 font-mono text-xs"
                    />
                  </Field>
                  <Field label="目标状态">
                    <Input
                      value={checkTo}
                      onChange={(event) => setCheckTo(event.target.value)}
                      placeholder="toStatus"
                      className="w-40 font-mono text-xs"
                    />
                  </Field>
                  <Button
                    variant="outline"
                    disabled={
                      checkFrom.trim() === "" ||
                      checkTo.trim() === "" ||
                      check.isPending
                    }
                    onClick={() =>
                      check.mutate({
                        bizType,
                        fromStatus: checkFrom.trim(),
                        toStatus: checkTo.trim(),
                      })
                    }
                  >
                    校验
                  </Button>
                </div>
                {check.data ? (
                  <div className="border-border rounded-md border p-3 text-sm">
                    <Badge variant={check.data.allowed ? "success" : "destructive"}>
                      {check.data.allowed ? "允许流转" : "禁止流转"}
                    </Badge>
                    {check.data.reason ? (
                      <p className="text-muted-foreground mt-1.5 text-xs">
                        {check.data.reason}
                      </p>
                    ) : null}
                  </div>
                ) : null}
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog
        open={!!dictForm}
        onOpenChange={(open) => {
          if (!open) {
            setDictForm(null);
            setDictError("");
          }
        }}
      >
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{dictForm?.id ? "编辑状态" : "新增状态"}</DialogTitle>
            <DialogDescription>
              业务类型 {bizType} 的状态字典项，控制状态在界面上的展示与流转语义。
            </DialogDescription>
          </DialogHeader>
          {dictForm ? (
            <div className="grid grid-cols-2 gap-4">
              <Field label="状态码 *">
                <Input
                  value={dictForm.statusCode}
                  onChange={(event) =>
                    setDictForm({ ...dictForm, statusCode: event.target.value })
                  }
                  placeholder="PENDING_PAYMENT"
                  className="font-mono text-xs"
                />
              </Field>
              <Field label="状态名称 *">
                <Input
                  value={dictForm.statusName}
                  onChange={(event) =>
                    setDictForm({ ...dictForm, statusName: event.target.value })
                  }
                  placeholder="待付款"
                />
              </Field>
              <Field label="英文名称">
                <Input
                  value={dictForm.statusNameEn}
                  onChange={(event) =>
                    setDictForm({
                      ...dictForm,
                      statusNameEn: event.target.value,
                    })
                  }
                  placeholder="Pending Payment"
                />
              </Field>
              <Field label="排序">
                <Input
                  value={dictForm.sortOrder}
                  onChange={(event) =>
                    setDictForm({ ...dictForm, sortOrder: event.target.value })
                  }
                  inputMode="numeric"
                  placeholder="0"
                />
              </Field>
              <Field label="颜色">
                <div className="flex items-center gap-2">
                  <ColorSwatch
                    color={/^#[0-9a-f]{3,8}$/i.test(dictForm.color) ? dictForm.color : undefined}
                  />
                  <Input
                    value={dictForm.color}
                    onChange={(event) =>
                      setDictForm({ ...dictForm, color: event.target.value })
                    }
                    placeholder="#16a34a"
                    className="font-mono text-xs"
                  />
                </div>
              </Field>
              <Field label="图标">
                <Input
                  value={dictForm.icon}
                  onChange={(event) =>
                    setDictForm({ ...dictForm, icon: event.target.value })
                  }
                  placeholder="📦"
                />
              </Field>
              <div className="col-span-2 flex flex-wrap gap-5">
                {(
                  [
                    ["isInitial", "初始状态"],
                    ["isTerminal", "终态"],
                    ["isCancellable", "可取消"],
                    ["enabled", "启用"],
                  ] as const
                ).map(([key, text]) => (
                  <label
                    key={key}
                    className="flex items-center gap-2 text-sm font-normal"
                  >
                    <Checkbox
                      checked={dictForm[key]}
                      onCheckedChange={(value) =>
                        setDictForm({ ...dictForm, [key]: value === true })
                      }
                    />
                    {text}
                  </label>
                ))}
              </div>
              <Field label="备注" className="col-span-2">
                <Textarea
                  value={dictForm.remark}
                  onChange={(event) =>
                    setDictForm({ ...dictForm, remark: event.target.value })
                  }
                  rows={2}
                />
              </Field>
              {dictError ? (
                <p className="text-destructive col-span-2 text-sm">{dictError}</p>
              ) : null}
            </div>
          ) : null}
          <DialogFooter>
            <Button variant="outline" onClick={() => setDictForm(null)}>
              取消
            </Button>
            <Button
              disabled={createDict.isPending || updateDict.isPending}
              onClick={submitDict}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!transitionForm}
        onOpenChange={(open) => {
          if (!open) {
            setTransitionForm(null);
            setTransitionError("");
          }
        }}
      >
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              {transitionForm?.id ? "编辑流转规则" : "新增流转规则"}
            </DialogTitle>
            <DialogDescription>
              业务类型 {bizType} 的一条状态机边：从起始状态经动作到目标状态。
            </DialogDescription>
          </DialogHeader>
          {transitionForm ? (
            <div className="grid grid-cols-2 gap-4">
              <Field label="起始状态 *">
                <Input
                  value={transitionForm.fromStatus}
                  onChange={(event) =>
                    setTransitionForm({
                      ...transitionForm,
                      fromStatus: event.target.value,
                    })
                  }
                  placeholder="PENDING_PAYMENT"
                  className="font-mono text-xs"
                />
              </Field>
              <Field label="目标状态 *">
                <Input
                  value={transitionForm.toStatus}
                  onChange={(event) =>
                    setTransitionForm({
                      ...transitionForm,
                      toStatus: event.target.value,
                    })
                  }
                  placeholder="PAID"
                  className="font-mono text-xs"
                />
              </Field>
              <Field label="动作码 *">
                <Input
                  value={transitionForm.actionCode}
                  onChange={(event) =>
                    setTransitionForm({
                      ...transitionForm,
                      actionCode: event.target.value,
                    })
                  }
                  placeholder="PAY"
                  className="font-mono text-xs"
                />
              </Field>
              <Field label="动作名称">
                <Input
                  value={transitionForm.actionName}
                  onChange={(event) =>
                    setTransitionForm({
                      ...transitionForm,
                      actionName: event.target.value,
                    })
                  }
                  placeholder="支付"
                />
              </Field>
              <Field label="动作英文名">
                <Input
                  value={transitionForm.actionNameEn}
                  onChange={(event) =>
                    setTransitionForm({
                      ...transitionForm,
                      actionNameEn: event.target.value,
                    })
                  }
                  placeholder="Pay"
                />
              </Field>
              <Field label="排序">
                <Input
                  value={transitionForm.sortOrder}
                  onChange={(event) =>
                    setTransitionForm({
                      ...transitionForm,
                      sortOrder: event.target.value,
                    })
                  }
                  inputMode="numeric"
                  placeholder="0"
                />
              </Field>
              <Field label="条件表达式" className="col-span-2">
                <Input
                  value={transitionForm.conditionExpression}
                  onChange={(event) =>
                    setTransitionForm({
                      ...transitionForm,
                      conditionExpression: event.target.value,
                    })
                  }
                  placeholder="amount > 0"
                  className="font-mono text-xs"
                />
              </Field>
              <Field label="前置动作">
                <Input
                  value={transitionForm.preAction}
                  onChange={(event) =>
                    setTransitionForm({
                      ...transitionForm,
                      preAction: event.target.value,
                    })
                  }
                  placeholder="checkStock"
                  className="font-mono text-xs"
                />
              </Field>
              <Field label="后置动作">
                <Input
                  value={transitionForm.postAction}
                  onChange={(event) =>
                    setTransitionForm({
                      ...transitionForm,
                      postAction: event.target.value,
                    })
                  }
                  placeholder="sendPaidMessage"
                  className="font-mono text-xs"
                />
              </Field>
              <div className="col-span-2 flex flex-wrap gap-5">
                {(
                  [
                    ["needApproval", "需要审批"],
                    ["enabled", "启用"],
                  ] as const
                ).map(([key, text]) => (
                  <label
                    key={key}
                    className="flex items-center gap-2 text-sm font-normal"
                  >
                    <Checkbox
                      checked={transitionForm[key]}
                      onCheckedChange={(value) =>
                        setTransitionForm({
                          ...transitionForm,
                          [key]: value === true,
                        })
                      }
                    />
                    {text}
                  </label>
                ))}
              </div>
              <Field label="备注" className="col-span-2">
                <Textarea
                  value={transitionForm.remark}
                  onChange={(event) =>
                    setTransitionForm({
                      ...transitionForm,
                      remark: event.target.value,
                    })
                  }
                  rows={2}
                />
              </Field>
              {transitionError ? (
                <p className="text-destructive col-span-2 text-sm">
                  {transitionError}
                </p>
              ) : null}
            </div>
          ) : null}
          <DialogFooter>
            <Button variant="outline" onClick={() => setTransitionForm(null)}>
              取消
            </Button>
            <Button
              disabled={createTransition.isPending || updateTransition.isPending}
              onClick={submitTransition}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!dictDeleteTarget}
        onOpenChange={(open) => !open && setDictDeleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>删除状态</DialogTitle>
            <DialogDescription>
              确定删除「{dictDeleteTarget?.statusName}
              （{dictDeleteTarget?.statusCode}）」？引用该状态的流转规则需要一并调整。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDictDeleteTarget(null)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={deleteDict.isPending}
              onClick={() => {
                if (!dictDeleteTarget) return;
                deleteDict.mutate(dictDeleteTarget.id, {
                  onSettled: () => setDictDeleteTarget(null),
                });
              }}
            >
              删除
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!transitionDeleteTarget}
        onOpenChange={(open) => !open && setTransitionDeleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>删除流转规则</DialogTitle>
            <DialogDescription>
              确定删除
              {transitionDeleteTarget
                ? ` ${transitionDeleteTarget.fromStatus} → ${transitionDeleteTarget.toStatus}`
                : ""}
              ？此操作不可撤销。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setTransitionDeleteTarget(null)}
            >
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={deleteTransition.isPending}
              onClick={() => {
                if (!transitionDeleteTarget) return;
                deleteTransition.mutate(transitionDeleteTarget.id, {
                  onSettled: () => setTransitionDeleteTarget(null),
                });
              }}
            >
              删除
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
