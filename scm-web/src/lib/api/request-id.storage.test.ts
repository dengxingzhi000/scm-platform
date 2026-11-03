import { afterEach, describe, expect, it, vi } from "vitest";

const DEVICE_ID_KEY = "scm-device-id";

function storageDenied() {
  return new DOMException("The operation is insecure.", "SecurityError");
}

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  vi.resetModules();
  window.localStorage.clear();
});

describe("resolveDeviceId with unavailable storage", () => {
  it("falls back to a module-level id when localStorage throws SecurityError", async () => {
    // Private mode, disabled cookies and some WebViews throw on every access.
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw storageDenied();
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw storageDenied();
    });

    const { resolveDeviceId } = await import("./request-id");

    const first = resolveDeviceId();
    const second = resolveDeviceId();

    expect(first).toMatch(/^[0-9a-f]{32}$/);
    expect(second).toBe(first);
    expect(window.localStorage.getItem).toHaveBeenCalled();
    expect(vi.mocked(window.localStorage.setItem)).toHaveBeenCalledWith(
      DEVICE_ID_KEY,
      first
    );
  });
});
