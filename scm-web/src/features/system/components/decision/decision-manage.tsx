"use client";

import { useMemo, useState, type ReactNode } from "react";
import {
  createColumnHelper,
  tableFeatures,
  useTable,
} from "@tanstack/react-table";
import {
  CheckCheck,
  GitCompareArrows,
  Layers,
  Pencil,
  Plus,
  Power,
  Scale,
  ShieldAlert,
  Trash2,
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
  ConflictDTO,
  DecisionRuleDTO,
  WeightConditionDTO,
  WeightProfileDTO,
} from "@/lib/api/contracts/system";
import {
  ENGINE_TYPES,
  useActivateWeight,
  useCreateDecisionRule,
  useCreateWeight,
  useDecisionRules,
  useDeleteDecisionRule,
  useDeleteWeight,
  useDetectConflicts,
  useToggleDecisionRule,
  useUpdateDecisionRule,
  useWeightProfiles,
  useWeightVersions,
} from "../../hooks/use-decision";

const features = tableFeatures({});
const ruleColumnHelper = createColumnHelper<typeof features, DecisionRuleDTO>();
const weightColumnHelper = createColumnHelper<
  typeof features,
  WeightProfileDTO
>();
const CUSTOM_OPTION = "__custom__";
const emptyRules: DecisionRuleDTO[] = [];
const emptyWeights: WeightProfileDTO[] = [];

interface RuleForm {
  id?: string;
  scene: string;
  ruleType: string;
  expression: string;
  description: string;
  priority: string;
  enabled: boolean;
}

const emptyRuleForm: RuleForm = {
  scene: "",
  ruleType: "",
  expression: "",
  description: "",
  priority: "",
  enabled: true,
};

interface WeightForm {
  scene: string;
  weights: string;
  conditions: string;
}

const emptyWeightForm: WeightForm = {
  scene: "",
  weights: "{\n  \n}",
  conditions: "[\n  \n]",
};

const isBlank = (value: string) => value.trim() === "";

/** `version` is server-managed, so it is deliberately not part of the payload. */
function toRulePayload(form: RuleForm, engineType: string): DecisionRuleDTO {
  return {
    id: form.id ?? "",
    engineType,
    scene: isBlank(form.scene) ? undefined : form.scene.trim(),
    ruleType: isBlank(form.ruleType) ? undefined : form.ruleType.trim(),
    expression: isBlank(form.expression) ? undefined : form.expression,
    description: isBlank(form.description)
      ? undefined
      : form.description.trim(),
    priority: isBlank(form.priority) ? undefined : Number(form.priority),
    enabled: form.enabled,
  };
}

type ParseResult<T> = { ok: true; value: T } | { ok: false; error: string };

function parseWeights(raw: string): ParseResult<Record<string, number>> {
  if (isBlank(raw)) return { ok: true, value: {} };
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch (error) {
    return {
      ok: false,
      error: `权重 JSON 解析失败：${
        error instanceof Error ? error.message : "未知错误"
      }`,
    };
  }
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    return { ok: false, error: "权重必须是非空 JSON 对象，例如 {\"price\": 0.6}" };
  }
  const entries = Object.entries(parsed as Record<string, unknown>);
  for (const [key, value] of entries) {
    if (typeof value !== "number" || Number.isNaN(value)) {
      return { ok: false, error: `权重 "${key}" 必须是数字` };
    }
  }
  return {
    ok: true,
    value: entries.reduce<Record<string, number>>((acc, [key, value]) => {
      acc[key] = value as number;
      return acc;
    }, {}),
  };
}

function parseConditions(raw: string): ParseResult<WeightConditionDTO[]> {
  if (isBlank(raw)) return { ok: true, value: [] };
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch (error) {
    return {
      ok: false,
      error: `条件 JSON 解析失败：${
        error instanceof Error ? error.message : "未知错误"
      }`,
    };
  }
  if (!Array.isArray(parsed)) {
    return {
      ok: false,
      error: '条件必须是数组，例如 [{"field":"amount","operator":">","value":100}]',
    };
  }
  for (const item of parsed) {
    if (
      !item ||
      typeof item !== "object" ||
      typeof (item as WeightConditionDTO).field !== "string" ||
      typeof (item as WeightConditionDTO).operator !== "string"
    ) {
      return {
        ok: false,
        error: "每个条件需包含字符串字段 field 与 operator",
      };
    }
  }
  return { ok: true, value: parsed as WeightConditionDTO[] };
}

function severityVariant(severity?: string) {
  switch ((severity ?? "").toUpperCase()) {
    case "HIGH":
    case "ERROR":
    case "CRITICAL":
    case "严重":
    case "高":
      return "destructive" as const;
    case "MEDIUM":
    case "WARNING":
    case "中":
      return "warning" as const;
    case "LOW":
    case "INFO":
    case "低":
      return "info" as const;
    default:
      return "secondary" as const;
  }
}

/** Canonical engineType values in a Select, but the field stays free text for tenant-specific engines. */
function EnginePicker({
  value,
  onChange,
}: {
  value: string;
  onChange: (next: string) => void;
}) {
  const isCustom = !(ENGINE_TYPES as readonly string[]).includes(value);
  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-muted-foreground text-sm">引擎类型</span>
      <Select
        value={isCustom ? CUSTOM_OPTION : value}
        onValueChange={(next) => {
          if (next !== CUSTOM_OPTION) onChange(next);
        }}
      >
        <SelectTrigger className="w-48">
          <SelectValue placeholder="引擎类型" />
        </SelectTrigger>
        <SelectContent>
          {ENGINE_TYPES.map((option) => (
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
        placeholder="engineType"
        className="w-48 font-mono text-xs"
        aria-label="引擎类型（自定义）"
      />
      {isCustom ? <Badge variant="info">自定义引擎</Badge> : null}
    </div>
  );
}

function Field({
  label,
  hint,
  children,
  className,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={`space-y-1.5 ${className ?? ""}`}>
      <Label className="text-muted-foreground text-xs">{label}</Label>
      {children}
      {hint ? (
        <p className="text-muted-foreground text-xs">{hint}</p>
      ) : null}
    </div>
  );
}

function JsonPreview({ text }: { text: string }) {
  return (
    <span
      className="text-muted-foreground block max-w-64 truncate font-mono text-xs"
      title={text}
    >
      {text}
    </span>
  );
}

export default function DecisionManage() {
  const [engineType, setEngineType] = useState<string>("SCORING");
  const [sceneFilter, setSceneFilter] = useState("");

  const [ruleForm, setRuleForm] = useState<RuleForm | null>(null);
  const [ruleError, setRuleError] = useState("");
  const [ruleDeleteTarget, setRuleDeleteTarget] =
    useState<DecisionRuleDTO | null>(null);
  const [conflicts, setConflicts] = useState<ConflictDTO[] | null>(null);

  const [weightForm, setWeightForm] = useState<WeightForm | null>(null);
  const [weightError, setWeightError] = useState("");
  const [weightDeleteTarget, setWeightDeleteTarget] =
    useState<WeightProfileDTO | null>(null);
  const [versionsOpen, setVersionsOpen] = useState(false);

  const { data: rules, isLoading: rulesLoading } =
    useDecisionRules(engineType);
  const { data: weights, isLoading: weightsLoading } = useWeightProfiles(
    engineType,
    isBlank(sceneFilter) ? undefined : sceneFilter.trim(),
  );
  const { data: versions, isLoading: versionsLoading } = useWeightVersions(
    engineType,
    versionsOpen,
  );

  const createRule = useCreateDecisionRule(engineType);
  const updateRule = useUpdateDecisionRule(engineType);
  const deleteRule = useDeleteDecisionRule(engineType);
  const toggleRule = useToggleDecisionRule(engineType);
  const detectConflicts = useDetectConflicts();

  const createWeight = useCreateWeight(engineType);
  const activateWeight = useActivateWeight(engineType);
  const deleteWeight = useDeleteWeight(engineType);

  const orderedRules = useMemo(
    () =>
      [...(rules ?? emptyRules)].sort(
        (a, b) => (a.priority ?? 0) - (b.priority ?? 0),
      ),
    [rules],
  );

  const ruleTable = useTable({
    features,
    columns: useMemo(
      () =>
        ruleColumnHelper.columns([
          ruleColumnHelper.accessor("priority", {
            header: "优先级",
            cell: (info) => (
              <span className="font-mono text-xs">{info.getValue() ?? "-"}</span>
            ),
          }),
          ruleColumnHelper.accessor("ruleType", {
            header: "规则类型",
            cell: (info) => info.getValue() ?? "-",
          }),
          ruleColumnHelper.accessor("scene", {
            header: "场景",
            cell: (info) => info.getValue() ?? "-",
          }),
          ruleColumnHelper.accessor("expression", {
            header: "表达式",
            cell: (info) => {
              const value = info.getValue();
              return value ? <JsonPreview text={value} /> : "-";
            },
          }),
          ruleColumnHelper.accessor("description", {
            header: "说明",
            cell: (info) => {
              const value = info.getValue();
              return value ? <JsonPreview text={value} /> : "-";
            },
          }),
          ruleColumnHelper.accessor("version", {
            header: "版本",
            cell: (info) =>
              info.getValue() == null ? (
                "-"
              ) : (
                <Badge variant="secondary">v{info.getValue()}</Badge>
              ),
          }),
          ruleColumnHelper.display({
            id: "enabled",
            header: "状态",
            cell: ({ row }) => (
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={row.original.enabled ? "停用规则" : "启用规则"}
                    className={
                      row.original.enabled
                        ? "text-[var(--color-success-text)]"
                        : "text-muted-foreground"
                    }
                    disabled={
                      toggleRule.isPending &&
                      toggleRule.variables?.id === row.original.id
                    }
                    onClick={() =>
                      toggleRule.mutate({
                        id: row.original.id,
                        enabled: row.original.enabled !== true,
                      })
                    }
                  >
                    <Power />
                    {row.original.enabled ? "启用中" : "已停用"}
                  </Button>
                </TooltipTrigger>
                <TooltipContent>
                  点击{row.original.enabled ? "停用" : "启用"}该规则
                </TooltipContent>
              </Tooltip>
            ),
          }),
          ruleColumnHelper.display({
            id: "actions",
            header: "操作",
            cell: ({ row }) => (
              <div className="flex items-center gap-1">
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label="编辑规则"
                      onClick={() => {
                        setRuleError("");
                        setConflicts(null);
                        setRuleForm({
                          id: row.original.id,
                          scene: row.original.scene ?? "",
                          ruleType: row.original.ruleType ?? "",
                          expression: row.original.expression ?? "",
                          description: row.original.description ?? "",
                          priority:
                            row.original.priority == null
                              ? ""
                              : String(row.original.priority),
                          enabled: row.original.enabled !== false,
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
                      aria-label="删除规则"
                      className="text-destructive hover:text-destructive"
                      onClick={() => setRuleDeleteTarget(row.original)}
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
      [toggleRule],
    ),
    data: orderedRules,
  });

  const weightTable = useTable({
    features,
    columns: useMemo(
      () =>
        weightColumnHelper.columns([
          weightColumnHelper.accessor("version", {
            header: "版本",
            cell: (info) =>
              info.getValue() == null ? (
                "-"
              ) : (
                <span className="font-mono text-xs">v{info.getValue()}</span>
              ),
          }),
          weightColumnHelper.accessor("scene", {
            header: "场景",
            cell: (info) => info.getValue() ?? "-",
          }),
          weightColumnHelper.accessor("active", {
            header: "生效",
            cell: (info) =>
              info.getValue() ? (
                <Badge variant="success">生效中</Badge>
              ) : (
                <Badge variant="secondary">未启用</Badge>
              ),
          }),
          weightColumnHelper.accessor("weights", {
            header: "权重",
            cell: (info) => {
              const weights = info.getValue();
              if (!weights || Object.keys(weights).length === 0) return "-";
              return (
                <div className="flex flex-wrap gap-1">
                  {Object.entries(weights).map(([key, value]) => (
                    <Badge key={key} variant="outline">
                      {key}: {value}
                    </Badge>
                  ))}
                </div>
              );
            },
          }),
          weightColumnHelper.accessor("conditions", {
            header: "条件",
            cell: (info) => {
              const list = info.getValue();
              if (!list || list.length === 0) return "-";
              return (
                <JsonPreview
                  text={list
                    .map(
                      (item) =>
                        `${item.field} ${item.operator} ${
                          item.value == null ? "" : JSON.stringify(item.value)
                        }`,
                    )
                    .join(" && ")}
                />
              );
            },
          }),
          weightColumnHelper.display({
            id: "actions",
            header: "操作",
            cell: ({ row }) => (
              <div className="flex items-center gap-1">
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="sm"
                      aria-label="启用权重方案"
                      disabled={
                        row.original.active === true ||
                        (activateWeight.isPending &&
                          activateWeight.variables === row.original.id)
                      }
                      onClick={() => activateWeight.mutate(row.original.id)}
                    >
                      <CheckCheck />
                      启用
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>
                    {row.original.active ? "当前已生效" : "设为生效方案"}
                  </TooltipContent>
                </Tooltip>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label="删除权重方案"
                      className="text-destructive hover:text-destructive"
                      onClick={() => setWeightDeleteTarget(row.original)}
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
      [activateWeight],
    ),
    data: weights ?? emptyWeights,
  });

  const submitRule = () => {
    if (!ruleForm) return;
    if (isBlank(ruleForm.expression)) {
      setRuleError("表达式为必填");
      return;
    }
    if (!isBlank(ruleForm.priority) && Number.isNaN(Number(ruleForm.priority))) {
      setRuleError("优先级必须是数字");
      return;
    }
    const payload = toRulePayload(ruleForm, engineType);
    const isEdit = !!ruleForm.id;
    if (isEdit) {
      updateRule.mutate(
        { id: ruleForm.id as string, data: payload },
        { onSuccess: () => setRuleForm(null) },
      );
    } else {
      createRule.mutate(payload, { onSuccess: () => setRuleForm(null) });
    }
  };

  const submitWeight = () => {
    if (!weightForm) return;
    const parsedWeights = parseWeights(weightForm.weights);
    if (!parsedWeights.ok) {
      setWeightError(parsedWeights.error);
      return;
    }
    const parsedConditions = parseConditions(weightForm.conditions);
    if (!parsedConditions.ok) {
      setWeightError(parsedConditions.error);
      return;
    }
    const payload: WeightProfileDTO = {
      id: "",
      engineType,
      scene: isBlank(weightForm.scene) ? undefined : weightForm.scene.trim(),
      weights: parsedWeights.value,
      conditions: parsedConditions.value,
    };
    createWeight.mutate(payload, { onSuccess: () => setWeightForm(null) });
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="系统管理"
        title="决策矩阵配置"
        description="维护决策规则、评分权重与冲突检测"
        actions={
          <>
            <Badge variant="secondary">{engineType}</Badge>
            <EnginePicker
              value={engineType}
              onChange={(next) => {
                if (next === engineType) return;
                setEngineType(next);
                setConflicts(null);
                setRuleForm(null);
                setWeightForm(null);
                setVersionsOpen(false);
              }}
            />
          </>
        }
      />

      <Card className="gap-0 overflow-hidden py-0">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Scale />
            决策规则
          </CardTitle>
          <CardAction className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={detectConflicts.isPending}
              onClick={() =>
                detectConflicts.mutate(engineType, {
                  onSuccess: (data) => setConflicts(data ?? []),
                })
              }
            >
              <ShieldAlert />
              检测冲突
            </Button>
            <Button
              size="sm"
              onClick={() => {
                setRuleError("");
                setRuleForm({ ...emptyRuleForm });
              }}
            >
              <Plus />
              新增规则
            </Button>
          </CardAction>
        </CardHeader>
        {conflicts ? (
          <div className="border-b px-4 py-3">
            <div className="mb-2 flex items-center justify-between">
              <span className="text-sm font-medium">
                冲突检测结果（{conflicts.length}）
              </span>
              <Button
                variant="ghost"
                size="sm"
                aria-label="关闭冲突结果"
                onClick={() => setConflicts(null)}
              >
                关闭
              </Button>
            </div>
            {conflicts.length === 0 ? (
              <p className="text-muted-foreground text-sm">
                未检测到规则冲突
              </p>
            ) : (
              <ul className="space-y-1.5">
                {conflicts.map((conflict, index) => (
                  <li
                    key={`${conflict.ruleId1 ?? "a"}-${conflict.ruleId2 ?? "b"}-${index}`}
                    className="flex flex-wrap items-center gap-2 text-sm"
                  >
                    <Badge variant={severityVariant(conflict.severity)}>
                      {conflict.severity ?? "未知"}
                    </Badge>
                    <span>{conflict.type ?? "冲突"}</span>
                    <span className="text-muted-foreground font-mono text-xs">
                      {conflict.ruleId1 ?? "-"} ↔ {conflict.ruleId2 ?? "-"}
                    </span>
                    {conflict.description ? (
                      <span className="text-muted-foreground text-xs">
                        {conflict.description}
                      </span>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </div>
        ) : null}
        {rulesLoading ? (
          <div className="p-6">
            <Skeleton className="h-48 w-full" />
          </div>
        ) : ruleTable.getRowModel().rows.length === 0 ? (
          <div className="text-muted-foreground py-12 text-center text-sm">
            该引擎下暂无决策规则
          </div>
        ) : (
          <Table>
            <TableHeader>
              {ruleTable.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead key={header.id}>
                      {header.isPlaceholder ? null : (
                        <ruleTable.FlexRender header={header} />
                      )}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {ruleTable.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell key={cell.id}>
                      <ruleTable.FlexRender cell={cell} />
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        <div className="text-muted-foreground border-t px-4 py-3 text-sm">
          共 {(rules ?? emptyRules).length} 条，按优先级升序（接口不分页）
        </div>
      </Card>

      <Card className="gap-0 overflow-hidden py-0">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Layers />
            权重方案
          </CardTitle>
          <CardAction className="flex flex-wrap items-center gap-2">
            <Input
              value={sceneFilter}
              onChange={(event) => setSceneFilter(event.target.value)}
              placeholder="按场景过滤"
              className="w-40 text-xs"
              aria-label="场景过滤"
            />
            <Button
              variant="outline"
              size="sm"
              onClick={() => setVersionsOpen(true)}
            >
              <GitCompareArrows />
              版本历史
            </Button>
            <Button
              size="sm"
              onClick={() => {
                setWeightError("");
                setWeightForm({ ...emptyWeightForm });
              }}
            >
              <Plus />
              新增方案
            </Button>
          </CardAction>
        </CardHeader>
        {weightsLoading ? (
          <div className="p-6">
            <Skeleton className="h-48 w-full" />
          </div>
        ) : weightTable.getRowModel().rows.length === 0 ? (
          <div className="text-muted-foreground py-12 text-center text-sm">
            该引擎下暂无权重方案
          </div>
        ) : (
          <Table>
            <TableHeader>
              {weightTable.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <TableHead key={header.id}>
                      {header.isPlaceholder ? null : (
                        <weightTable.FlexRender header={header} />
                      )}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {weightTable.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getAllCells().map((cell) => (
                    <TableCell key={cell.id}>
                      <weightTable.FlexRender cell={cell} />
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        <div className="text-muted-foreground border-t px-4 py-3 text-sm">
          共 {(weights ?? emptyWeights).length} 条
          {isBlank(sceneFilter) ? "" : `（场景 ${sceneFilter.trim()}）`}
        </div>
      </Card>

      <Dialog open={!!ruleForm} onOpenChange={(open) => !open && setRuleForm(null)}>
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{ruleForm?.id ? "编辑规则" : "新增规则"}</DialogTitle>
            <DialogDescription>
              引擎 {engineType} 的决策规则。表达式由后端引擎解析，前端只做透传。
            </DialogDescription>
          </DialogHeader>
          {ruleForm ? (
            <div className="grid grid-cols-2 gap-4">
              <Field label="规则类型">
                <Input
                  value={ruleForm.ruleType}
                  onChange={(event) =>
                    setRuleForm({ ...ruleForm, ruleType: event.target.value })
                  }
                  placeholder="THRESHOLD"
                  className="font-mono text-xs"
                />
              </Field>
              <Field label="场景">
                <Input
                  value={ruleForm.scene}
                  onChange={(event) =>
                    setRuleForm({ ...ruleForm, scene: event.target.value })
                  }
                  placeholder="supplier_select"
                  className="font-mono text-xs"
                />
              </Field>
              <Field
                label="优先级"
                hint="数值越小越先命中（列表按优先级升序展示）"
              >
                <Input
                  value={ruleForm.priority}
                  onChange={(event) =>
                    setRuleForm({ ...ruleForm, priority: event.target.value })
                  }
                  inputMode="numeric"
                  placeholder="1"
                />
              </Field>
              <div className="flex items-end pb-2">
                <label className="flex items-center gap-2 text-sm">
                  <Checkbox
                    checked={ruleForm.enabled}
                    onCheckedChange={(value) =>
                      setRuleForm({ ...ruleForm, enabled: value === true })
                    }
                  />
                  启用规则
                </label>
              </div>
              <Field
                label="表达式 *"
                className="col-span-2"
                hint="引擎表达式，保存时原样提交"
              >
                <Textarea
                  value={ruleForm.expression}
                  onChange={(event) =>
                    setRuleForm({ ...ruleForm, expression: event.target.value })
                  }
                  rows={4}
                  className="font-mono text-xs"
                  placeholder="score >= 80 && riskLevel == 'LOW'"
                />
              </Field>
              <Field label="说明" className="col-span-2">
                <Textarea
                  value={ruleForm.description}
                  onChange={(event) =>
                    setRuleForm({ ...ruleForm, description: event.target.value })
                  }
                  rows={2}
                />
              </Field>
              {ruleError ? (
                <p className="text-destructive col-span-2 text-sm">{ruleError}</p>
              ) : null}
            </div>
          ) : null}
          <DialogFooter>
            <Button variant="outline" onClick={() => setRuleForm(null)}>
              取消
            </Button>
            <Button
              disabled={createRule.isPending || updateRule.isPending}
              onClick={submitRule}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!weightForm}
        onOpenChange={(open) => {
          if (!open) {
            setWeightForm(null);
            setWeightError("");
          }
        }}
      >
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>新增权重方案</DialogTitle>
            <DialogDescription>
              引擎 {engineType} 的权重方案。权重与条件的键随引擎变化，请以 JSON 编辑。
            </DialogDescription>
          </DialogHeader>
          {weightForm ? (
            <div className="space-y-4">
              <Field label="场景">
                <Input
                  value={weightForm.scene}
                  onChange={(event) =>
                    setWeightForm({ ...weightForm, scene: event.target.value })
                  }
                  placeholder="supplier_select"
                  className="font-mono text-xs"
                />
              </Field>
              <Field
                label="权重 JSON *"
                hint='形如 {"price": 0.4, "quality": 0.35, "delivery": 0.25}'
              >
                <Textarea
                  value={weightForm.weights}
                  onChange={(event) =>
                    setWeightForm({ ...weightForm, weights: event.target.value })
                  }
                  rows={6}
                  className="font-mono text-xs"
                />
              </Field>
              <Field
                label="条件 JSON"
                hint='形如 [{"field":"region","operator":"==","value":"CN"}]'
              >
                <Textarea
                  value={weightForm.conditions}
                  onChange={(event) =>
                    setWeightForm({
                      ...weightForm,
                      conditions: event.target.value,
                    })
                  }
                  rows={5}
                  className="font-mono text-xs"
                />
              </Field>
              {weightError ? (
                <p className="text-destructive text-sm">{weightError}</p>
              ) : null}
            </div>
          ) : null}
          <DialogFooter>
            <Button variant="outline" onClick={() => setWeightForm(null)}>
              取消
            </Button>
            <Button
              disabled={createWeight.isPending}
              onClick={submitWeight}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={versionsOpen} onOpenChange={setVersionsOpen}>
        <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>权重版本历史</DialogTitle>
            <DialogDescription>
              引擎 {engineType} 的全部权重版本（含未生效的历史版本）。
            </DialogDescription>
          </DialogHeader>
          {versionsLoading ? (
            <Skeleton className="h-40 w-full" />
          ) : (versions ?? emptyWeights).length === 0 ? (
            <p className="text-muted-foreground py-6 text-center text-sm">
              暂无版本记录
            </p>
          ) : (
            <ul className="space-y-2">
              {(versions ?? emptyWeights).map((item, index) => (
                <li
                  key={item.id || `${item.version ?? "v"}-${index}`}
                  className="border-border space-y-1.5 rounded-md border p-3"
                >
                  <div className="flex flex-wrap items-center gap-2 text-sm">
                    <span className="font-mono text-xs">
                      v{item.version ?? "-"}
                    </span>
                    <Badge variant="secondary">{item.scene ?? "默认场景"}</Badge>
                    {item.active ? (
                      <Badge variant="success">生效中</Badge>
                    ) : null}
                    <span className="text-muted-foreground ml-auto font-mono text-xs">
                      {item.id}
                    </span>
                  </div>
                  <div className="flex flex-wrap gap-1">
                    {Object.entries(item.weights ?? {}).map(([key, value]) => (
                      <Badge key={key} variant="outline">
                        {key}: {value}
                      </Badge>
                    ))}
                  </div>
                  {item.conditions && item.conditions.length > 0 ? (
                    <>
                      <Separator />
                      <JsonPreview
                        text={item.conditions
                          .map(
                            (condition) =>
                              `${condition.field} ${condition.operator} ${
                                condition.value == null
                                  ? ""
                                  : JSON.stringify(condition.value)
                              }`,
                          )
                          .join(" && ")}
                      />
                    </>
                  ) : null}
                </li>
              ))}
            </ul>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setVersionsOpen(false)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!ruleDeleteTarget}
        onOpenChange={(open) => !open && setRuleDeleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>删除规则</DialogTitle>
            <DialogDescription>
              确定删除规则 {ruleDeleteTarget?.id}？此操作不可撤销。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRuleDeleteTarget(null)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={deleteRule.isPending}
              onClick={() => {
                if (!ruleDeleteTarget) return;
                deleteRule.mutate(ruleDeleteTarget.id, {
                  onSettled: () => setRuleDeleteTarget(null),
                });
              }}
            >
              删除
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!weightDeleteTarget}
        onOpenChange={(open) => !open && setWeightDeleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>删除权重方案</DialogTitle>
            <DialogDescription>
              确定删除权重方案 {weightDeleteTarget?.id}（v
              {weightDeleteTarget?.version ?? "-"}）？生效中的方案请先切换。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setWeightDeleteTarget(null)}
            >
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={deleteWeight.isPending}
              onClick={() => {
                if (!weightDeleteTarget) return;
                deleteWeight.mutate(weightDeleteTarget.id, {
                  onSettled: () => setWeightDeleteTarget(null),
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
