"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { message } from "antd";

import type {
  ContractInfo,
  FileQuery,
  FileUpdatePayload,
  InvoiceInfo,
} from "@/lib/api/contracts/file";
import { toApiError } from "@/lib/api/envelope";
import { fileApiService } from "@/lib/api/services/file";

const fail = (error: unknown) => message.error(toApiError(error).message);

export function useFilePage(query: FileQuery) {
  return useQuery({
    queryKey: ["file", "page", query],
    queryFn: () => fileApiService.page(query),
  });
}

export function useFileStats() {
  return useQuery({
    queryKey: ["file", "stats"],
    queryFn: () => fileApiService.stats(),
  });
}

export function useUpdateFile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      payload,
    }: {
      id: string;
      payload: FileUpdatePayload;
    }) => fileApiService.update(id, payload),
    onSuccess: () => {
      message.success("文件信息已更新");
      qc.invalidateQueries({ queryKey: ["file"] });
    },
    onError: fail,
  });
}

export function useDeleteFile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => fileApiService.remove(id),
    onSuccess: () => {
      message.success("文件已删除");
      qc.invalidateQueries({ queryKey: ["file"] });
    },
    onError: fail,
  });
}

export function useDeleteFiles() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (ids: string[]) => fileApiService.removeBatch(ids),
    onSuccess: (deleted) => {
      message.success(`已删除 ${deleted} 个文件`);
      qc.invalidateQueries({ queryKey: ["file"] });
    },
    onError: fail,
  });
}

export function useUploadFile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      file,
      bizType,
      bizId,
    }: {
      file: File;
      bizType?: string;
      bizId?: string;
    }) => fileApiService.upload(file, bizType, bizId),
    onSuccess: () => {
      message.success("文件上传成功");
      qc.invalidateQueries({ queryKey: ["file", "page"] });
    },
    onError: fail,
  });
}

export type OcrMode = "recognize" | "invoice" | "contract";

export function useOcr() {
  return useMutation({
    mutationFn: async ({
      file,
      mode,
    }: {
      file: File;
      mode: OcrMode;
    }): Promise<{ mode: OcrMode; text?: string; invoice?: InvoiceInfo; contract?: ContractInfo }> => {
      if (mode === "invoice") {
        return { mode, invoice: await fileApiService.ocrInvoice(file) };
      }
      if (mode === "contract") {
        return { mode, contract: await fileApiService.ocrContract(file) };
      }
      return { mode, text: await fileApiService.ocrRecognize(file) };
    },
    onError: fail,
  });
}
