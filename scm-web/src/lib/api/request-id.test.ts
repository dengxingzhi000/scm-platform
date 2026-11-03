import { afterEach, describe, expect, it, vi } from "vitest";

import { createRandomId, resolveDeviceId } from "./request-id";

const UUID_V4 =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

function stubCrypto(value: unknown) {
  vi.stubGlobal("crypto", value);
}

function getRandomValues(bytes: Uint8Array) {
  bytes.fill(0x12);
  return bytes;
}

afterEach(() => {
  vi.unstubAllGlobals();
  window.localStorage.clear();
});

describe("createRandomId", () => {
  it("prefers crypto.randomUUID when the context exposes it", () => {
    const randomUUID = vi.fn(() => "11111111-1111-4111-8111-111111111111");
    stubCrypto({ randomUUID, getRandomValues });

    expect(createRandomId()).toBe("11111111-1111-4111-8111-111111111111");
    expect(randomUUID).toHaveBeenCalledTimes(1);
  });

  it("falls back to getRandomValues on an insecure HTTP origin", () => {
    // Production is served over http://192.168.0.121:3000 鈥?crypto.randomUUID is
    // undefined there, but crypto.getRandomValues is still available.
    const getRandomValuesSpy = vi.fn(getRandomValues);
    stubCrypto({ getRandomValues: getRandomValuesSpy });

    const id = createRandomId();

    expect(id).toMatch(UUID_V4);
    expect(getRandomValuesSpy).toHaveBeenCalledTimes(1);
  });

  it("still returns a UUID when crypto is unavailable", () => {
    stubCrypto(undefined);

    expect(createRandomId()).toMatch(UUID_V4);
  });
});

describe("resolveDeviceId", () => {
  it("generates and persists a stable device id without crypto.randomUUID", () => {
    stubCrypto({ getRandomValues });

    const first = resolveDeviceId();
    const second = resolveDeviceId();

    expect(first).toMatch(/^[0-9a-f]{32}$/);
    expect(second).toBe(first);
    expect(window.localStorage.getItem("scm-device-id")).toBe(first);
  });
});
