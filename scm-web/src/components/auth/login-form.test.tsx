import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { LoginForm } from "./login-form";

const login = vi.fn();

vi.mock("@/lib/api/services/auth", () => ({
  authService: { login: (...args: unknown[]) => login(...args) },
}));

// useMutation 需要 QueryClient 上下文;这里直接 mock 掉,让测试只关心表单行为。
// useQuery 也要一并提供 —— Task 6 会把 SocialAuthButtons(useQuery)插进这个表单。
vi.mock("@tanstack/react-query", () => ({
  useMutation: () => ({
    mutateAsync: (...args: unknown[]) => login(...args),
    isPending: false,
  }),
  useQuery: () => ({ data: [] as string[] }),
}));

vi.mock("next-intl", () => ({
  useLocale: () => "zh-CN",
  // 模拟 next-intl 的命名空间行为:useTranslations("login") 返回的 t 会把命名空间拼进 key
  useTranslations: (ns: string) => (key: string) => `${ns}.${key}`,
}));

describe("LoginForm", () => {
  beforeEach(() => {
    login.mockReset();
    login.mockResolvedValue({ accessToken: "t", refreshToken: "r", userId: "u", username: "admin" });
  });

  it("does not call the API when required fields are empty", async () => {
    const onSuccess = vi.fn();
    render(<LoginForm onSuccess={onSuccess} onMfaRequired={vi.fn()} onError={vi.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: "login.submit" }));

    await waitFor(() => {
      expect(screen.getByText("login.errors.usernameRequired")).toBeInTheDocument();
    });
    expect(login).not.toHaveBeenCalled();
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it("submits the credentials and forwards the response", async () => {
    const onSuccess = vi.fn();
    render(<LoginForm onSuccess={onSuccess} onMfaRequired={vi.fn()} onError={vi.fn()} />);

    await userEvent.type(screen.getByLabelText("login.username"), "admin");
    await userEvent.type(screen.getByLabelText("login.password"), "admin123");
    await userEvent.click(screen.getByRole("button", { name: "login.submit" }));

    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
    expect(login).toHaveBeenCalledWith({ username: "admin", password: "admin123" });
  });

  it("reports a non-MFA server failure instead of swallowing it", async () => {
    const onError = vi.fn();
    // 后端拒绝:密码错误。必须走 onError,不能静默 ——
    // 旧页面在这里调 message.error,丢了就是「点登录没反应」。
    login.mockRejectedValue(
      Object.assign(new Error("密码错误"), {
        response: { status: 400, data: { code: 40001, message: "密码错误" } },
      })
    );
    render(<LoginForm onSuccess={vi.fn()} onMfaRequired={vi.fn()} onError={onError} />);

    await userEvent.type(screen.getByLabelText("login.username"), "admin");
    await userEvent.type(screen.getByLabelText("login.password"), "wrong");
    await userEvent.click(screen.getByRole("button", { name: "login.submit" }));

    await waitFor(() => expect(onError).toHaveBeenCalledTimes(1));
    expect(onError.mock.calls[0][0]).toMatchObject({ message: "密码错误" });
  });

  it("exposes name attributes the e2e spec relies on", () => {
    render(<LoginForm onSuccess={vi.fn()} onMfaRequired={vi.fn()} onError={vi.fn()} />);
    expect(screen.getByLabelText("login.username")).toHaveAttribute("name", "username");
    expect(screen.getByLabelText("login.password")).toHaveAttribute("name", "password");
  });

  it("links to forgot-password in the active locale", () => {
    render(<LoginForm onSuccess={vi.fn()} onMfaRequired={vi.fn()} onError={vi.fn()} />);
    // useLocale 在测试里 mock 成 "zh-CN";写死 /zh-CN 的实现也能过,所以这条只证明前缀来自 locale,
    // 真正的多语言验证靠下面 Step 7 的 e2e 用 /en-US/login 再跑一遍
    expect(screen.getByRole("link", { name: "login.forgotPassword" })).toHaveAttribute(
      "href",
      "/zh-CN/forgot-password"
    );
  });

  it("routes a 40104 challenge to the MFA step, not to the error path", async () => {
    const onMfaRequired = vi.fn();
    const onError = vi.fn();
    login.mockRejectedValue(
      Object.assign(new Error("需要二次验证"), {
        response: { status: 401, data: { code: 40104, message: "需要二次验证" } },
      })
    );
    render(<LoginForm onSuccess={vi.fn()} onMfaRequired={onMfaRequired} onError={onError} />);

    await userEvent.type(screen.getByLabelText("login.username"), "admin");
    await userEvent.type(screen.getByLabelText("login.password"), "admin123");
    await userEvent.click(screen.getByRole("button", { name: "login.submit" }));

    await waitFor(() => expect(onMfaRequired).toHaveBeenCalledTimes(1));
    expect(onMfaRequired).toHaveBeenCalledWith({ username: "admin", password: "admin123", remember: true });
    expect(onError).not.toHaveBeenCalled();
  });

  it("routes a message-only MFA challenge the same way", async () => {
    const onMfaRequired = vi.fn();
    const onError = vi.fn();
    login.mockRejectedValue(
      Object.assign(new Error("MFA required"), {
        response: { status: 401, data: { code: 0, message: "MFA required" } },
      })
    );
    render(<LoginForm onSuccess={vi.fn()} onMfaRequired={onMfaRequired} onError={onError} />);

    await userEvent.type(screen.getByLabelText("login.username"), "admin");
    await userEvent.type(screen.getByLabelText("login.password"), "admin123");
    await userEvent.click(screen.getByRole("button", { name: "login.submit" }));

    await waitFor(() => expect(onMfaRequired).toHaveBeenCalledTimes(1));
    expect(onError).not.toHaveBeenCalled();
  });
});
