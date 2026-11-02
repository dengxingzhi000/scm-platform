import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AuthShell } from "./auth-shell";

vi.mock("next-intl", () => ({
  // 模拟 next-intl 的命名空间行为:useTranslations("login") 返回的 t 会把命名空间拼进 key
  useTranslations: (ns: string) => (key: string) => `${ns}.${key}`,
}));

// next/image 在 jsdom 里没有图片优化器;只保留断言需要的 src / alt。
vi.mock("next/image", () => ({
  default: ({ src, alt }: { src: string; alt: string }) => (
    // eslint-disable-next-line @next/next/no-img-element -- next/image 的测试替身
    <img src={src} alt={alt} />
  ),
}));

describe("AuthShell", () => {
  it("renders the brand wordmark, hero card and legal copy", () => {
    render(
      <AuthShell>
        <p>content</p>
      </AuthShell>
    );

    expect(screen.getByText("login.brandName")).toBeInTheDocument();
    expect(screen.getByText("login.hero.title")).toBeInTheDocument();
    expect(screen.getByText("login.hero.feature1")).toBeInTheDocument();
    expect(screen.getByText("login.hero.feature2")).toBeInTheDocument();
    expect(screen.getByText("login.hero.feature3")).toBeInTheDocument();
    expect(screen.getByText("login.legal")).toBeInTheDocument();
  });

  it("scopes the shadcn variables to the fixed-light auth palette", () => {
    const { container } = render(<AuthShell>content</AuthShell>);
    const root = container.firstElementChild as HTMLElement;
    expect(root.style.getPropertyValue("--background")).toBe("var(--auth-bg)");
    expect(root.style.getPropertyValue("--foreground")).toBe("var(--auth-ink)");
    expect(root.style.getPropertyValue("--primary")).toBe("var(--auth-btn)");
    expect(root.style.getPropertyValue("--ring")).toBe("var(--auth-focus)");
  });

  it("renders the children in the left column and the cover image in the right column", () => {
    render(
      <AuthShell>
        <p>content</p>
      </AuthShell>
    );

    expect(screen.getByText("content")).toBeInTheDocument();
    const cover = screen.getByRole("img", { name: "login.heroAlt" });
    expect(cover).toHaveAttribute("src", "/auth/cover.jpg");
  });
});
