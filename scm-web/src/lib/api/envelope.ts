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

  // ApiResponse is @JsonInclude(NON_NULL): failures and Void results omit `data` entirely.
  if (isEnvelope<T>(payload)) {
    if (payload.code !== 200) {
      throw new ApiError(payload.code, payload.message || "Request failed");
    }
    return payload.data as T;
  }

  return payload as T;
}

function isEnvelope<T>(payload: unknown): payload is ApiEnvelope<T> {
  return (
    payload !== null &&
    typeof payload === "object" &&
    typeof (payload as ApiEnvelope<T>).code === "number" &&
    ("data" in payload || "message" in payload)
  );
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
