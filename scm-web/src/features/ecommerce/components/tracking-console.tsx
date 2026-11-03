"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { message } from "antd";
import { MapPin, Plus, Search } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
import { TableEmpty, TablePanel } from "@/components/ui/table-shell";
import { Textarea } from "@/components/ui/textarea";
import { toApiError } from "@/lib/api/envelope";
import { fulfillmentApi } from "@/lib/api/services/ecommerce";
import type { Tracking } from "@/lib/api/contracts/ecommerce";
import { cn } from "@/lib/utils";
import { formatDateTime } from "../lib/status";

type SearchMode = "tracking" | "fulfillment";

interface TrackingFormState {
  fulfillmentNo: string;
  trackingNo: string;
  carrier: string;
  status: string;
  location: string;
  description: string;
  eventTime: string;
}

const emptyTrackingForm: TrackingFormState = {
  fulfillmentNo: "",
  trackingNo: "",
  carrier: "",
  status: "",
  location: "",
  description: "",
  eventTime: "",
};

export default function TrackingConsole() {
  const [mode, setMode] = useState<SearchMode>("tracking");
  const [input, setInput] = useState("");
  const [results, setResults] = useState<Tracking[] | null>(null);
  const [searchedNo, setSearchedNo] = useState("");

  const [recordOpen, setRecordOpen] = useState(false);
  const [recordForm, setRecordForm] =
    useState<TrackingFormState>(emptyTrackingForm);

  const search = useMutation({
    mutationFn: (vars: {
      mode: SearchMode;
      no: string;
      latest: boolean;
    }): Promise<Tracking[]> => {
      if (vars.latest) {
        return fulfillmentApi.tracking
          .latest(vars.no)
          .then((latest) => (latest ? [latest] : []));
      }
      return vars.mode === "tracking"
        ? fulfillmentApi.tracking.byTrackingNo(vars.no)
        : fulfillmentApi.tracking.byFulfillmentNo(vars.no);
    },
    onSuccess: (data, vars) => {
      setResults(data ?? []);
      setSearchedNo(vars.no);
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const record = useMutation({
    mutationFn: () =>
      fulfillmentApi.tracking.record({
        fulfillmentNo: recordForm.fulfillmentNo.trim(),
        trackingNo: recordForm.trackingNo.trim() || undefined,
        carrier: recordForm.carrier.trim() || undefined,
        status: recordForm.status.trim(),
        location: recordForm.location.trim() || undefined,
        description: recordForm.description.trim() || undefined,
        eventTime: recordForm.eventTime || undefined,
      }),
    onSuccess: () => {
      message.success("轨迹事件已登记");
      setRecordOpen(false);
      setRecordForm(emptyTrackingForm);
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const runSearch = (latest = false) => {
    const no = input.trim();
    if (!no) {
      message.warning("请输入单号");
      return;
    }
    search.mutate({ mode, no, latest });
  };

  const canRecord = Boolean(
    recordForm.fulfillmentNo.trim() && recordForm.status.trim()
  );

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="电商中心"
        title="物流轨迹"
        description="按物流单号或履约单号查询轨迹，并登记轨迹事件"
        actions={
          <Button
            onClick={() => {
              setRecordForm(emptyTrackingForm);
              setRecordOpen(true);
            }}
          >
            <Plus />
            登记轨迹事件
          </Button>
        }
      />

      <TablePanel className="p-4">
        <form
          className="flex flex-wrap items-end gap-3"
          onSubmit={(event) => {
            event.preventDefault();
            runSearch();
          }}
        >
          <div className="space-y-1">
            <Label className="text-xs">查询方式</Label>
            <Select
              value={mode}
              onValueChange={(value) => setMode(value as SearchMode)}
            >
              <SelectTrigger size="sm" className="w-40 text-xs">
                <SelectValue placeholder="查询方式" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="tracking">按物流单号</SelectItem>
                <SelectItem value="fulfillment">按履约单号</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1">
            <Label className="text-xs">单号</Label>
            <Input
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder={
                mode === "tracking" ? "请输入物流单号" : "请输入履约单号"
              }
              className="h-8 w-64 text-xs"
            />
          </div>
          <Button
            type="submit"
            size="sm"
            className="h-8"
            disabled={search.isPending}
          >
            <Search />
            查询
          </Button>
          {mode === "fulfillment" && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-8"
              disabled={search.isPending}
              onClick={() => runSearch(true)}
            >
              查询最新
            </Button>
          )}
        </form>
      </TablePanel>

      <TablePanel>
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border-secondary)] px-3 py-2">
          <span className="text-[12px] font-semibold">轨迹记录</span>
          <span className="text-muted-foreground text-[11px]">
            {results === null ? "尚未查询" : `共 ${results.length} 条`}
            {searchedNo ? ` · ${searchedNo}` : ""}
          </span>
        </div>

        {search.isPending ? (
          <div className="text-muted-foreground py-16 text-center text-[13px]">
            查询中…
          </div>
        ) : results === null ? (
          <TableEmpty>输入单号后点击查询，查看物流轨迹</TableEmpty>
        ) : results.length === 0 ? (
          <TableEmpty>未找到轨迹记录</TableEmpty>
        ) : (
          <div className="p-4">
            {results.map((item, index) => (
              <div
                key={item.id ?? `${item.eventTime ?? item.createdAt}-${index}`}
                className="flex gap-3"
              >
                <div className="flex flex-col items-center pt-1.5">
                  <span
                    className={cn(
                      "size-2.5 shrink-0 rounded-full",
                      index === 0
                        ? "bg-[var(--color-primary)]"
                        : "bg-[var(--color-border-strong)]"
                    )}
                  />
                  {index < results.length - 1 && (
                    <span className="w-px flex-1 bg-[var(--color-border-secondary)]" />
                  )}
                </div>
                <div className="min-w-0 flex-1 pb-5">
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge variant={index === 0 ? "info" : "secondary"}>
                      {item.status || "状态未知"}
                    </Badge>
                    <span className="text-muted-foreground text-[11px]">
                      {formatDateTime(item.eventTime ?? item.createdAt)}
                    </span>
                  </div>
                  {item.location && (
                    <p className="text-muted-foreground mt-1 flex items-center gap-1 text-[11px]">
                      <MapPin className="size-3" />
                      {item.location}
                    </p>
                  )}
                  {item.description && (
                    <p className="mt-1 text-xs">{item.description}</p>
                  )}
                  {(item.carrier || item.trackingNo) && (
                    <p className="text-muted-foreground mt-1 font-mono text-[10px]">
                      {[item.carrier, item.trackingNo]
                        .filter(Boolean)
                        .join(" · ")}
                    </p>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </TablePanel>

      <Dialog open={recordOpen} onOpenChange={setRecordOpen}>
        <DialogContent className="max-h-[86vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>登记轨迹事件</DialogTitle>
            <DialogDescription>
              为履约单补充一条物流轨迹
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="履约单号">
              <Input
                value={recordForm.fulfillmentNo}
                onChange={(event) =>
                  setRecordForm((prev) => ({
                    ...prev,
                    fulfillmentNo: event.target.value,
                  }))
                }
                placeholder="必填"
              />
            </Field>
            <Field label="物流单号">
              <Input
                value={recordForm.trackingNo}
                onChange={(event) =>
                  setRecordForm((prev) => ({
                    ...prev,
                    trackingNo: event.target.value,
                  }))
                }
                placeholder="可选"
              />
            </Field>
            <Field label="承运商">
              <Input
                value={recordForm.carrier}
                onChange={(event) =>
                  setRecordForm((prev) => ({
                    ...prev,
                    carrier: event.target.value,
                  }))
                }
                placeholder="可选，如 SF / JD"
              />
            </Field>
            <Field label="轨迹状态">
              <Input
                value={recordForm.status}
                onChange={(event) =>
                  setRecordForm((prev) => ({
                    ...prev,
                    status: event.target.value,
                  }))
                }
                placeholder="必填，如 SHIPPED"
              />
            </Field>
            <Field label="所在地">
              <Input
                value={recordForm.location}
                onChange={(event) =>
                  setRecordForm((prev) => ({
                    ...prev,
                    location: event.target.value,
                  }))
                }
                placeholder="可选"
              />
            </Field>
            <Field label="事件时间">
              <Input
                type="datetime-local"
                value={recordForm.eventTime}
                onChange={(event) =>
                  setRecordForm((prev) => ({
                    ...prev,
                    eventTime: event.target.value,
                  }))
                }
              />
            </Field>
            <div className="space-y-1 sm:col-span-2">
              <Label className="text-xs">轨迹描述</Label>
              <Textarea
                value={recordForm.description}
                onChange={(event) =>
                  setRecordForm((prev) => ({
                    ...prev,
                    description: event.target.value,
                  }))
                }
                rows={3}
                placeholder="可选"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRecordOpen(false)}>
              取消
            </Button>
            <Button
              disabled={record.isPending || !canRecord}
              onClick={() => record.mutate()}
            >
              登记
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-1">
      <Label className="text-xs">{label}</Label>
      {children}
    </div>
  );
}
