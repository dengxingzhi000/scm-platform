import { describe, expect, it } from "vitest";

import { buildLoginSchema, buildTotpSchema } from "./login-schema";

// 翻译函数原样返回 key,这样断言里能直接看到是哪个 key
const t = (key: string) => key;

describe("buildLoginSchema", () => {
  it("rejects an empty username", () => {
    const result = buildLoginSchema(t).safeParse({ username: "", password: "x" });
    expect(result.success).toBe(false);
  });

  it("rejects an empty password", () => {
    const result = buildLoginSchema(t).safeParse({ username: "admin", password: "" });
    expect(result.success).toBe(false);
  });

  it("accepts a filled pair", () => {
    expect(buildLoginSchema(t).safeParse({ username: "admin", password: "admin123" }).success).toBe(true);
  });

  it("puts the translated message into the error", () => {
    const result = buildLoginSchema(t).safeParse({ username: "", password: "" });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe("errors.usernameRequired");
    }
  });
});

describe("buildTotpSchema", () => {
  it("requires exactly six digits", () => {
    expect(buildTotpSchema(t).safeParse({ code: "12345" }).success).toBe(false);
    expect(buildTotpSchema(t).safeParse({ code: "12345a" }).success).toBe(false);
    expect(buildTotpSchema(t).safeParse({ code: "123456" }).success).toBe(true);
  });
});
