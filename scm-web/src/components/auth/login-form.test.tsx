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
    render(<LoginForm onSuccess={onSuccess} onMfaRequired={vi.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: "login.submit" }));

    await waitFor(() => {
      expect(screen.getByText("login.errors.usernameRequired")).toBeInTheDocument();
    });
    expect(login).not.toHaveBeenCalled();
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it("submits the credentials and forwards the response", async () => {
    const onSuccess = vi.fn();
    render(<LoginForm onSuccess={onSuccess} onMfaRequired={vi.fn()} />);

    await userEvent.type(screen.getByLabelText("login.username"), "admin");
    await userEvent.type(screen.getByLabelText("login.password"), "admin123");
    await userEvent.click(screen.getByRole("button", { name: "login.submit" }));

    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
    expect(login).toHaveBeenCalledWith({ username: "admin", password: "admin123" });
  });

  it("exposes name attributes the e2e spec relies on", () => {
    render(<LoginForm onSuccess={vi.fn()} onMfaRequired={vi.fn()} />);
    expect(screen.getByLabelText("login.username")).toHaveAttribute("name", "username");
    expect(screen.getByLabelText("login.password")).toHaveAttribute("name", "password");
  });
});
