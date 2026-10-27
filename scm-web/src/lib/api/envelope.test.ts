import { describe, expect, it } from "vitest";

import { ApiError, toApiError, unwrap } from "./envelope";

describe("ApiResponse envelope", () => {
  it("returns data when the backend omits the key entirely", async () => {
    // ApiResponse is @JsonInclude(NON_NULL): every Void success has no `data`.
    await expect(
      unwrap<void>(Promise.resolve({ code: 200, message: "Success", timestamp: 1 }))
    ).resolves.toBeUndefined();
  });

  it("unwraps a normal success payload", async () => {
    await expect(
      unwrap<{ id: string }>(Promise.resolve({ code: 200, message: "Success", data: { id: "1" } }))
    ).resolves.toEqual({ id: "1" });
  });

  it("throws on an in-band failure that still carries HTTP 200", async () => {
    await expect(
      unwrap(Promise.resolve({ code: 404, message: "订单不存在", timestamp: 1 }))
    ).rejects.toMatchObject({ name: "ApiError", code: 404, message: "订单不存在" });
  });

  it("passes through bodies that are not envelopes", async () => {
    await expect(unwrap<boolean>(Promise.resolve(true))).resolves.toBe(true);
    await expect(unwrap<number>(Promise.resolve(12))).resolves.toBe(12);
  });

  it("maps gateway error bodies and transport failures to ApiError", () => {
    const gateway = toApiError({
      response: { status: 401, data: { code: 401, error: "INVALID_TOKEN", message: "Token expired" } },
    });
    expect(gateway).toBeInstanceOf(ApiError);
    expect(gateway.code).toBe(401);
    expect(gateway.message).toBe("Token expired");

    expect(toApiError(new Error("Network down"))).toMatchObject({ code: 0, message: "Network down" });
  });
});
