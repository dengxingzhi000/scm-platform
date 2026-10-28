import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { TotpForm } from "./totp-form";

const login = vi.fn();

vi.mock("@/lib/api/services/auth", () => ({
  authService: { login: (...args: unknown[]) => login(...args) },
}));

// useMutation 需要 QueryClient 上下文;直接 mock 掉,让测试只关心表单行为
vi.mock("@tanstack/react-query", () => ({
  useMutation: () => ({
    mutateAsync: (...args: unknown[]) => login(...args),
    isPending: false,
  }),
}));

vi.mock("next-intl", () => ({
  // 模拟命名空间:useTranslations("login") 的 t("mfa.submit") 解析为 login.mfa.submit
  useTranslations: (ns: string) => (key: string) => `${ns}.${key}`,
}));

const credentials = { username: "admin", password: "admin123" };

describe("TotpForm", () => {
  beforeEach(() => {
    login.mockReset();
  });

  it("sends the pending credentials together with the code", async () => {
    const onSuccess = vi.fn();
    login.mockResolvedValue({ accessToken: "t", refreshToken: "r", userId: "u", username: "admin" });
    render(<TotpForm pendingCredentials={credentials} onBack={vi.fn()} onSuccess={onSuccess} />);

    await userEvent.type(screen.getByLabelText("login.mfa.title"), "123456");
    await userEvent.click(screen.getByRole("button", { name: "login.mfa.submit" }));

    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
    expect(login).toHaveBeenCalledWith({ ...credentials, twoFactorCode: "123456" });
  });

  it("rejects a code that is not six digits before hitting the API", async () => {
    render(<TotpForm pendingCredentials={credentials} onBack={vi.fn()} onSuccess={vi.fn()} />);

    await userEvent.type(screen.getByLabelText("login.mfa.title"), "123");
    await userEvent.click(screen.getByRole("button", { name: "login.mfa.submit" }));

    await waitFor(() => {
      expect(screen.getByText("login.errors.codeLength")).toBeInTheDocument();
    });
    expect(login).not.toHaveBeenCalled();
  });

  it("surfaces a rejected code without leaving the TOTP step", async () => {
    const onSuccess = vi.fn();
    login.mockRejectedValue(
      Object.assign(new Error("验证码错误"), {
        response: { status: 400, data: { code: 40002, message: "验证码错误" } },
      })
    );
    render(<TotpForm pendingCredentials={credentials} onBack={vi.fn()} onSuccess={onSuccess} />);

    await userEvent.type(screen.getByLabelText("login.mfa.title"), "000000");
    await userEvent.click(screen.getByRole("button", { name: "login.mfa.submit" }));

    await waitFor(() => {
      expect(screen.getByText("验证码错误")).toBeInTheDocument();
    });
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it("goes back to the password step", async () => {
    const onBack = vi.fn();
    render(<TotpForm pendingCredentials={credentials} onBack={onBack} onSuccess={vi.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: "login.mfa.back" }));
    expect(onBack).toHaveBeenCalledTimes(1);
    expect(login).not.toHaveBeenCalled();
  });
});
