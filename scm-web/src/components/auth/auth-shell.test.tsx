import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AuthShell } from "./auth-shell";

vi.mock("next-intl", () => ({
  // 模拟 next-intl 的命名空间行为:useTranslations("login") 返回的 t 会把命名空间拼进 key
  useTranslations: (ns: string) => (key: string) => `${ns}.${key}`,
}));

describe("AuthShell", () => {
  it("renders the brand mark and both header actions", () => {
    render(
      <AuthShell>
        <p>content</p>
      </AuthShell>
    );

    expect(screen.getByText("login.brandName")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "login.header.contactSales" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "login.header.tryPlatform" })).toBeInTheDocument();
  });

  it("scopes the Claude accent to the login experience only", () => {
    const { container } = render(<AuthShell>content</AuthShell>);
    const root = container.firstElementChild as HTMLElement;
    expect(root.style.getPropertyValue("--primary")).toBe("var(--color-brand-accent)");
    expect(root.style.getPropertyValue("--ring")).toBe("var(--color-brand-accent)");
  });

  it("renders the children in the left column and the hero image in the right column", () => {
    render(
      <AuthShell>
        <p>content</p>
      </AuthShell>
    );

    expect(screen.getByText("content")).toBeInTheDocument();
    const hero = screen.getByRole("img", { name: "login.heroAlt" });
    expect(hero).toHaveAttribute("src", "/auth-hero.svg");
  });
});
