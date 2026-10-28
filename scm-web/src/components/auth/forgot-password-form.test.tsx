import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { ForgotPasswordForm } from "./forgot-password-form";

vi.mock("next-intl", () => ({
  // 模拟 next-intl 的命名空间行为:useTranslations("forgot") 返回的 t 会把命名空间拼进 key
  useTranslations: (ns: string) => (key: string) => `${ns}.${key}`,
}));

describe("ForgotPasswordForm", () => {
  it("requires a valid email before sending a code", async () => {
    render(<ForgotPasswordForm />);

    await userEvent.click(screen.getByRole("button", { name: "forgot.sendCode" }));
    await waitFor(() => {
      expect(screen.getByText("forgot.errors.emailRequired")).toBeInTheDocument();
    });
  });

  it("advances to the code step after a valid email", async () => {
    render(<ForgotPasswordForm />);

    await userEvent.type(screen.getByLabelText("forgot.email"), "ops@example.com");
    await userEvent.click(screen.getByRole("button", { name: "forgot.sendCode" }));

    await waitFor(() => {
      expect(screen.getByText("forgot.codeSent")).toBeInTheDocument();
    });
  });
});
