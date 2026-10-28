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

/** 从验证码步走到重置密码步。 */
const advanceFromCodeToPasswordStep = async () => {
  await userEvent.type(screen.getByLabelText("forgot.code"), "123456");
  await userEvent.click(screen.getByRole("button", { name: "forgot.verify" }));
  await waitFor(() => expect(screen.getByLabelText("forgot.newPassword")).toBeInTheDocument());
};

/** 走完验证码步,停在重置密码步。 */
const advanceToPasswordStep = async () => {
  await advanceToCodeStep();
  await advanceFromCodeToPasswordStep();
};

/** 步骤指示里被标成当前步(aria-current="step")的那一项的标题。 */
const currentStepTitle = () =>
  screen.getByRole("listitem", { current: "step" }).textContent;

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

  it("marks the current step in the step indicator", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);

    expect(currentStepTitle()).toBe("forgot.steps.email");

    await advanceToCodeStep();
    expect(currentStepTitle()).toBe("forgot.steps.code");

    await advanceFromCodeToPasswordStep();
    expect(currentStepTitle()).toBe("forgot.steps.password");
  });

  it("goes back to the email step from the code step", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);
    await advanceToCodeStep();

    await userEvent.click(screen.getByRole("button", { name: "forgot.backStep" }));

    await waitFor(() => expect(screen.getByLabelText("forgot.email")).toBeInTheDocument());
    expect(screen.queryByLabelText("forgot.code")).not.toBeInTheDocument();
  });

  it("keeps the typed email when going back to the email step", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);
    await advanceToCodeStep();

    await userEvent.click(screen.getByRole("button", { name: "forgot.backStep" }));

    await waitFor(() => expect(screen.getByLabelText("forgot.email")).toHaveValue("ops@example.com"));
  });

  it("can send a new code straight after going back to the email step", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);
    await advanceToCodeStep();

    await userEvent.click(screen.getByRole("button", { name: "forgot.backStep" }));
    await waitFor(() => expect(screen.getByLabelText("forgot.email")).toBeInTheDocument());

    // 输入框里还留着地址,校验也得认它 —— 否则会误报「请输入邮箱地址」
    await userEvent.click(screen.getByRole("button", { name: "forgot.sendCode" }));

    await waitFor(() => expect(screen.getByText("forgot.codeSent")).toBeInTheDocument());
    expect(screen.queryByText("forgot.errors.emailRequired")).not.toBeInTheDocument();
  });

  it("goes back to the code step from the password step", async () => {
    render(<ForgotPasswordForm onDone={vi.fn()} />);
    await advanceToPasswordStep();

    await userEvent.click(screen.getByRole("button", { name: "forgot.backStep" }));

    await waitFor(() => expect(screen.getByLabelText("forgot.code")).toBeInTheDocument());
    expect(screen.queryByLabelText("forgot.newPassword")).not.toBeInTheDocument();
  });
});
