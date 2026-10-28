import { test, expect } from "@playwright/test";

test("login page displays correctly", async ({ page }) => {
  await page.goto("/zh-CN/login");
  await expect(page.locator("h1")).toContainText("让供应链运转如丝");
  await expect(page.locator('input[name="username"]')).toBeVisible();
  await expect(page.locator('input[name="password"]')).toBeVisible();
});

test("successful login redirects to dashboard", async ({ page }) => {
  await page.goto("/zh-CN/login");
  await page.fill('input[name="username"]', "admin");
  await page.fill('input[name="password"]', "admin123");
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL(/\/zh-CN\/dashboard/);
});

test("login page renders the English copy", async ({ page }) => {
  await page.goto("/en-US/login");
  await expect(page.locator("h1")).toContainText("Make your supply chain flow");
  await expect(page.locator('input[name="username"]')).toBeVisible();
  // 忘记密码链接必须跟随 locale —— 写死 /zh-CN 的实现会在这里变红
  await expect(page.getByRole("link", { name: "Forgot password?" })).toHaveAttribute(
    "href",
    "/en-US/forgot-password"
  );
});
