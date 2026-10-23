import { expect, test } from "@playwright/test";

test("model overview keeps the diagram, notes and module links usable", async ({ page }) => {
  await page.goto("/overview/#the-annual-cycle");
  const diagram = page.locator("figure.module-diagram");
  await expect(diagram).toBeVisible();
  await expect.poll(() => diagram.locator("img").evaluate(image => image.complete && image.naturalWidth > 0)).toBe(true);
  await expect(diagram.getByRole("link")).toHaveAttribute("href", /model-structure-uk\.svg$/);

  const notes = page.locator(".diagram-key__notes");
  await expect(notes.locator("dl")).toBeHidden();
  await notes.locator(":scope > summary").click();
  await expect(notes.locator("dl")).toBeVisible();
  await notes.locator(":scope > summary").click();
  await expect(notes.locator("dl")).toBeHidden();
  await expect(page.getByRole("heading", { name: "Transitions and individual variation", exact: true })).toBeVisible();

  await page.locator(".model-overview__components").getByRole("link", { name: "Social care", exact: true }).click();
  await expect(page).toHaveURL(/\/overview\/modules\/social-care\/$/);
});

test("model overview remains readable and navigable without JavaScript", async ({ browser, baseURL }) => {
  const context = await browser.newContext({ javaScriptEnabled: false, baseURL });
  const page = await context.newPage();
  await page.goto("/overview/");
  await expect(page.locator("figure.module-diagram img")).toBeVisible();
  const notes = page.locator(".diagram-key__notes");
  await notes.locator(":scope > summary").click();
  await expect(notes.locator("dl")).toBeVisible();
  await page.locator(".model-overview__components").getByRole("link", { name: "Disposable income", exact: true }).click();
  await expect(page).toHaveURL(/\/overview\/modules\/disposable-income\/$/);
  await context.close();
});
