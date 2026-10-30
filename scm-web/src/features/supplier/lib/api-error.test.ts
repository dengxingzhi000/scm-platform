import { describe, expect, it } from "vitest";
import { cleanApiError, errorMessage } from "./api-error";

describe("cleanApiError", () => {
  it("keeps the backend business reason from a 4xx envelope", () => {
    const error = { response: { status: 400, data: { code: 400, message: "Service error: 合作中的供应商不能删除，请先终止合作 (traceId=abc123)" } } };
    expect(cleanApiError(error)).toEqual({
      code: 400,
      message: "合作中的供应商不能删除，请先终止合作",
      traceId: "abc123",
    });
  });

  it("strips the prefix even when there is no trace id", () => {
    const error = { response: { status: 400, data: { code: 400, message: "Service error: 税号已存在" } } };
    expect(errorMessage(error)).toBe("税号已存在");
  });

  it("leaves a plain message untouched", () => {
    expect(errorMessage(new Error("Network down"))).toBe("Network down");
  });
});
