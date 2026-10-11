import { expect, test } from "@playwright/test";

test("annual module inspection preserves the diagram and exposes the selected explanation", async ({ page }) => {
  await page.goto("/overview/");
  const explorer = page.locator("[data-annual-explorer]");
  await expect(page.locator("figure.module-diagram")).toBeVisible();
  await expect(explorer).not.toHaveAttribute("open", "");
  await explorer.locator(":scope > summary").click();

  const picker = explorer.getByRole("combobox", { name: "Module", exact: true });
  const tabs = explorer.getByRole("tablist");
  if (await tabs.isVisible()) {
    const first = tabs.getByRole("tab", { name: "Ageing", exact: true });
    await first.focus();
    await first.press("ArrowDown");
    const education = tabs.getByRole("tab", { name: "Education", exact: true });
    await expect(education).toBeFocused();
    await expect(education).toHaveAttribute("aria-selected", "true");
    await expect(explorer.getByRole("tabpanel")).toContainText("educational attainment");
    await education.press("End");
    await expect(tabs.getByRole("tab", { name: "Mental-health adjustments", exact: true })).toBeFocused();
    await tabs.getByRole("tab", { name: "Social care", exact: true }).click();
  } else {
    await picker.selectOption({ label: "Social care" });
  }

  await expect(explorer.getByRole("tabpanel")).toContainText("Informal care uses time");
  await expect(explorer.locator(".annual-explorer__stage:visible")).toHaveCount(1);
  await explorer.getByRole("link", { name: "Care receipt and provision", exact: true }).click();
  await expect(page).toHaveURL(/\/overview\/modules\/social-care\/$/);
  await page.goBack();
  await expect(page.locator("[data-annual-explorer] .annual-explorer__tabs")).toHaveCount(1);
});

test("annual module inspection remains readable and navigable without JavaScript", async ({ browser, baseURL }) => {
  const context = await browser.newContext({ javaScriptEnabled: false, baseURL });
  const page = await context.newPage();
  await page.goto("/overview/");
  await expect(page.locator("figure.module-diagram")).toBeVisible();
  const explorer = page.locator("[data-annual-explorer]");
  await explorer.locator(":scope > summary").click();
  const stage = explorer.locator("#cycle-employment");
  await stage.locator(":scope > summary").click();
  await expect(stage).toContainText("precomputed tax–benefit results");
  await expect(stage.getByRole("link", { name: "Disposable income", exact: true })).toBeVisible();
  await stage.getByRole("link", { name: "Disposable income", exact: true }).click();
  await expect(page).toHaveURL(/\/overview\/modules\/disposable-income\/$/);
  await context.close();
});
