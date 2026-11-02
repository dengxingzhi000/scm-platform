import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  ContractInfo,
  FileMetadataDTO,
  FileMetadataPage,
  FileQuery,
  FileStats,
  FileUpdatePayload,
  InvoiceInfo,
} from "../contracts/file";

const FILE_BASE = "/scm-file/api/v1/files";
const OCR_BASE = "/scm-file/file/ocr";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, body?: unknown, config?: object) =>
  unwrap<T>(apiClient.post(url, body, config));

const put = <T>(url: string, body?: unknown) =>
  unwrap<T>(apiClient.put(url, body));

const del = <T>(url: string) => unwrap<T>(apiClient.delete(url));

const withFile = (file: File) => {
  const form = new FormData();
  form.append("file", file);
  return form;
};

export const fileApiService = {
  page: (query: FileQuery) => get<FileMetadataPage>(`${FILE_BASE}/page`, query),

  stats: () => get<FileStats>(`${FILE_BASE}/stats`),

  update: (id: string, payload: FileUpdatePayload) =>
    put<FileMetadataDTO>(`${FILE_BASE}/${id}`, payload),

  remove: (id: string) => del<void>(`${FILE_BASE}/${id}`),

  removeBatch: (ids: string[]) =>
    post<number>(`${FILE_BASE}/batch-delete`, { ids }),

  /** Multipart upload; the backend also accepts `bizType`/`bizId` as query params. */
  upload: (file: File, bizType?: string, bizId?: string) =>
    post<FileMetadataDTO>(
      `${FILE_BASE}/upload`,
      withFile(file),
      {
        params: {
          ...(bizType ? { bizType } : {}),
          ...(bizId ? { bizId } : {}),
        },
        headers: { "Content-Type": "multipart/form-data" },
      },
    ),

  checkMd5: (md5: string) =>
    get<FileMetadataDTO | null>(`${FILE_BASE}/check/${md5}`),

  getUrl: (id: string) =>
    get<Record<string, string>>(`${FILE_BASE}/${id}/url`),

  /** Returns the raw blob so the caller can trigger a browser download. */
  download: (id: string) =>
    apiClient
      .get(`${FILE_BASE}/${id}/content`, { responseType: "blob" })
      .then((payload) => payload as unknown as Blob),

  /** Inline variant of the content endpoint, used by the preview dialog. */
  preview: (id: string) =>
    apiClient
      .get(`${FILE_BASE}/${id}/preview`, { responseType: "blob" })
      .then((payload) => payload as unknown as Blob),

  ocrRecognize: (file: File) =>
    post<string>(`${OCR_BASE}/recognize`, withFile(file), {
      headers: { "Content-Type": "multipart/form-data" },
    }),

  ocrInvoice: (file: File) =>
    post<InvoiceInfo>(`${OCR_BASE}/invoice`, withFile(file), {
      headers: { "Content-Type": "multipart/form-data" },
    }),

  ocrContract: (file: File) =>
    post<ContractInfo>(`${OCR_BASE}/contract`, withFile(file), {
      headers: { "Content-Type": "multipart/form-data" },
    }),
};
