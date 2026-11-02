import { toApiError } from "@/lib/api/envelope";

export interface CleanError {
  code: number;
  message: string;
  traceId?: string;
}

/**
 * 后端 GlobalExceptionHandler 把业务异常包成
 * `Service error: <原因> (traceId=xxx)`，axios 因此会 reject，
 * unwrap() 走不到 —— 必须从错误对象里把真正的原因捞出来。
 * traceId 只在消息尾部匹配，业务原因里的括号不会被误伤。
 */
const TRACE_PATTERN = /\s*\(traceId=([^)]+)\)\s*$/;

export function cleanApiError(error: unknown): CleanError {
  const apiError = toApiError(error);
  let message = apiError.message ?? "";
  let traceId: string | undefined;

  const trace = TRACE_PATTERN.exec(message);
  if (trace) {
    traceId = trace[1];
    message = message.replace(TRACE_PATTERN, "");
  }
  if (message.startsWith("Service error: ")) {
    message = message.slice("Service error: ".length);
  }
  return traceId ? { code: apiError.code, message, traceId } : { code: apiError.code, message };
}

export function errorMessage(error: unknown): string {
  return cleanApiError(error).message;
}
