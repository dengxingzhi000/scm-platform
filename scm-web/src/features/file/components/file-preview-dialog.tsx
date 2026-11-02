"use client";

import { useEffect, useMemo, useState } from "react";
import { message } from "antd";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import type { FileMetadataDTO } from "@/lib/api/contracts/file";
import { toApiError } from "@/lib/api/envelope";
import { fileApiService } from "@/lib/api/services/file";

interface FilePreviewDialogProps {
  file: FileMetadataDTO | null;
  onOpenChange: (open: boolean) => void;
}

const isPdf = (type: string) => type === "application/pdf";
const isText = (type: string) =>
  type.startsWith("text/") ||
  type.includes("json") ||
  type.includes("xml") ||
  type.includes("javascript");

function FilePreviewDialog({ file, onOpenChange }: FilePreviewDialogProps) {
  const [blob, setBlob] = useState<Blob | null>(null);
  const [text, setText] = useState<string | null>(null);
  const [loadedFor, setLoadedFor] = useState<string | null>(null);
  const contentType = file?.contentType ?? "";

  useEffect(() => {
    if (!file) return;
    let cancelled = false;
    fileApiService
      .preview(file.id)
      .then(async (data) => {
        if (cancelled) return;
        setBlob(data);
        if (isText(contentType)) {
          setText(await data.text());
        }
        setLoadedFor(file.id);
      })
      .catch((error) => {
        if (cancelled) return;
        setBlob(null);
        setText(null);
        setLoadedFor(file.id);
        message.error(toApiError(error).message);
      });
    return () => {
      cancelled = true;
    };
  }, [file, contentType]);

  const loading = Boolean(file) && loadedFor !== file?.id;

  const url = useMemo(() => (blob ? URL.createObjectURL(blob) : null), [blob]);
  useEffect(
    () => () => {
      if (url) URL.revokeObjectURL(url);
    },
    [url],
  );

  const renderBody = () => {
    if (loading) {
      return <Skeleton className="h-56 w-full rounded-md" />;
    }
    if (!blob) {
      return (
        <div className="text-muted-foreground py-12 text-center text-[12px]">
          暂无预览内容
        </div>
      );
    }
    if (contentType.startsWith("image/")) {
      return (
        // eslint-disable-next-line @next/next/no-img-element -- blob preview, dimensions unknown
        <img
          src={url ?? ""}
          alt={file?.originalName ?? "文件预览"}
          className="mx-auto max-h-[60vh] w-auto rounded-md"
        />
      );
    }
    if (isPdf(contentType)) {
      return (
        <iframe
          src={url ?? ""}
          title={file?.originalName ?? "PDF 预览"}
          className="h-[60vh] w-full rounded-md border"
        />
      );
    }
    if (isText(contentType)) {
      return (
        <pre className="max-h-[60vh] overflow-auto rounded-md bg-card p-3 text-[12px] whitespace-pre-wrap break-all">
          {text ?? "暂无文本内容"}
        </pre>
      );
    }
    return (
      <div className="text-muted-foreground py-12 text-center text-[12px]">
        该文件类型暂不支持在线预览，请下载后查看。
      </div>
    );
  };

  return (
    <Dialog open={Boolean(file)} onOpenChange={(open) => !open && onOpenChange(false)}>
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle className="truncate">
            {file?.originalName ?? "文件预览"}
          </DialogTitle>
          <DialogDescription>
            {contentType || "未知类型"}
            {file?.bizType ? ` · ${file.bizType}` : ""}
          </DialogDescription>
        </DialogHeader>
        {renderBody()}
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
          {url && (
            <Button asChild>
              <a href={url} download={file?.originalName ?? undefined}>
                下载
              </a>
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export { FilePreviewDialog };
