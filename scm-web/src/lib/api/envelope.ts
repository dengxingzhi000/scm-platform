export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
  timestamp?: number;
}

export class ApiError extends Error {
  constructor(
    public readonly code: number,
    message: string
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function unwrap<T>(request: Promise<unknown>): Promise<T> {
  const payload = (await request) as ApiEnvelope<T> | T;

  if (
    payload !== null &&
    typeof payload === "object" &&
    "code" in payload &&
    "data" in payload
  ) {
    const envelope = payload as ApiEnvelope<T>;
    if (envelope.code !== 200) {
      throw new ApiError(envelope.code, envelope.message || "Request failed");
    }
    return envelope.data;
  }

  return payload as T;
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;

  const response = (
    error as {
      response?: { status?: number; data?: Partial<ApiEnvelope<unknown>> };
    }
  )?.response;

  if (response?.data && typeof response.data.code === "number") {
    return new ApiError(
      response.data.code,
      response.data.message || "Request failed"
    );
  }

  return new ApiError(
    response?.status ?? 0,
    error instanceof Error ? error.message : "Network error"
  );
}
