import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { AuthShell } from "./auth-shell";

describe("AuthShell", () => {
  it("renders the brand mark and both header actions", () => {
    render(
      <AuthShell>
        <p>content</p>
      </AuthShell>
    );

    expect(screen.getByText("SCM 供应链管理平台")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "联系销售" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "试用平台" })).toBeInTheDocument();
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
    const hero = screen.getByRole("img", { name: "供应链协同" });
    expect(hero).toHaveAttribute("src", "/auth-hero.svg");
  });
});
