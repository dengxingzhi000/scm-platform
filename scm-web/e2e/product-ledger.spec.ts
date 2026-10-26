import { test, expect } from "@playwright/test";

/**
 * Core product ledger path.
 *
 * The proxy requires an `access_token` cookie; the product service is still a
 * local mock, so this E2E test runs without the gateway.
 */
test.beforeEach(async ({ context }) => {
  await context.addCookies([
    {
      name: "access_token",
      value: "e2e-token",
      domain: "localhost",
      path: "/",
    },
  ]);
});

test("product ledger lists, searches and opens a product", async ({ page }) => {
  await page.goto("/zh-CN/product");

  await expect(
    page.getByRole("heading", { name: "商品管理" }),
  ).toBeVisible();
  await expect(page.getByText("iPhone 15 Pro")).toBeVisible();

  await page.getByPlaceholder("搜索商品名称/编码").fill("MacBook");
  await page.getByRole("button", { name: "搜索" }).click();

  await expect(page.getByText("MacBook Air M3")).toBeVisible();
  await expect(page.getByText("iPhone 15 Pro")).toHaveCount(0);

  await page.getByRole("button", { name: "查看" }).first().click();
  await expect(page).toHaveURL(/\/zh-CN\/product\/.+/);
});
