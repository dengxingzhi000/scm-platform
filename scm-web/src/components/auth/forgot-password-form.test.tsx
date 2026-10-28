import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { ForgotPasswordForm } from "./forgot-password-form";

vi.mock("next-intl", () => ({
  // 模拟 next-intl 的命名空间行为:useTranslations("forgot") 返回的 t 会把命名空间拼进 key
  useTranslations: (ns: string) => (key: string) => `${ns}.${key}`,
}));

/** 走完邮箱步,停在验证码步。 */
const advanceToCodeStep = async () => {
  await userEvent.type(screen.getByLabelText("forgot.email"), "ops@example.com");
  await userEvent.click(screen.getByRole("button", { name: "forgot.sendCode" }));
  await waitFor(() => expect(screen.getByText("forgot.codeSent")).toBeInTheDocument());
};

/** 走完验证码步,停在重置密码步。 */
const advanceToPasswordStep = async () => {
  await advanceToCodeStep();
  await userEvent.type(screen.getByLabelText("forgot.code"), "123456");
  await userEvent.click(screen.getByRole("button", { name: "forgot.verify" }));
  await waitFor(() => expect(screen.getByLabelText("forgot.newPassword")).toBeInTheDocument());
};

describe("ForgotPasswordForm", () => {
  it("requires a valid email before sending a code", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);

    await userEvent.click(screen.getByRole("button", { name: "forgot.sendCode" }));
    await waitFor(() => {
      expect(screen.getByText("forgot.errors.emailRequired")).toBeInTheDocument();
    });
  });

  it("rejects a malformed email", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);

    await userEvent.type(screen.getByLabelText("forgot.email"), "not-an-email");
    await userEvent.click(screen.getByRole("button", { name: "forgot.sendCode" }));

    await waitFor(() => {
      expect(screen.getByText("forgot.errors.emailInvalid")).toBeInTheDocument();
    });
  });

  it("advances to the code step after a valid email", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);

    await advanceToCodeStep();
  });

  it("blocks the code step until six digits are entered", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);
    await advanceToCodeStep();

    await userEvent.type(screen.getByLabelText("forgot.code"), "123");
    await userEvent.click(screen.getByRole("button", { name: "forgot.verify" }));

    await waitFor(() => {
      expect(screen.getByText("forgot.errors.codeLength")).toBeInTheDocument();
    });
    // 没通过校验就不能进入下一步
    expect(screen.getByText("forgot.codeSent")).toBeInTheDocument();
  });

  it("advances to the password step with a six-digit code", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);

    await advanceToPasswordStep();
  });

  it("rejects a password shorter than eight characters", async () => {
    const onDone = vi.fn();
    render(<ForgotPasswordForm onDone={onDone} />);
    await advanceToPasswordStep();

    await userEvent.type(screen.getByLabelText("forgot.newPassword"), "short");
    await userEvent.type(screen.getByLabelText("forgot.confirmPassword"), "short");
    await userEvent.click(screen.getByRole("button", { name: "forgot.reset" }));

    await waitFor(() => {
      expect(screen.getByText("forgot.errors.passwordMin")).toBeInTheDocument();
    });
    expect(onDone).not.toHaveBeenCalled();
  });

  it("rejects a mismatched confirmation", async () => {
    const onDone = vi.fn();
    render(<ForgotPasswordForm onDone={onDone} />);
    await advanceToPasswordStep();

    await userEvent.type(screen.getByLabelText("forgot.newPassword"), "admin12345");
    await userEvent.type(screen.getByLabelText("forgot.confirmPassword"), "admin12346");
    await userEvent.click(screen.getByRole("button", { name: "forgot.reset" }));

    await waitFor(() => {
      expect(screen.getByText("forgot.errors.passwordMismatch")).toBeInTheDocument();
    });
    expect(onDone).not.toHaveBeenCalled();
  });

  it("reports completion once the passwords match", async () => {
    const onDone = vi.fn();
    render(<ForgotPasswordForm onDone={onDone} />);
    await advanceToPasswordStep();

    await userEvent.type(screen.getByLabelText("forgot.newPassword"), "admin12345");
    await userEvent.type(screen.getByLabelText("forgot.confirmPassword"), "admin12345");
    await userEvent.click(screen.getByRole("button", { name: "forgot.reset" }));

    await waitFor(() => expect(onDone).toHaveBeenCalledTimes(1));
  });
});
