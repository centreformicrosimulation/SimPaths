import {test, expect} from '@playwright/test';
test('switching preserves page and fragment and reloads channel assets', async ({page}) => {
  await page.goto('/overview/#the-annual-cycle');
  await expect(page.locator('[data-version-ready]')).toHaveAttribute('data-version-ready','true');
  await page.getByRole('link', {name:'View develop',exact:true}).click();
  await expect(page).toHaveURL(/\/develop\/overview\/#the-annual-cycle$/);
  await expect(page.locator('[data-docs-channel="develop"]')).toContainText('Unreleased development');
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content','noindex, nofollow');
  await page.getByRole('link', {name:'View main',exact:true}).click();
  await expect(page).toHaveURL(/8316\/overview\/#the-annual-cycle$/);
  await expect(page.locator('meta[name="robots"]')).toHaveCount(0);
});
test('missing page fallback is explicit', async ({page}) => {
  await page.route('**/develop/version.json', route => route.fulfill({json:{pages:['']}}));
  await page.goto('/research/');
  await page.getByRole('link',{name:'View develop',exact:true}).click();
  await expect(page).toHaveURL(/\/develop\/\?version-page-missing=1$/);
  await expect(page.locator('[data-version-notice]')).toContainText('not available');
});
test('failed version fetch keeps the current page and explains failure',async({page})=>{
  await page.route('**/develop/version.json',route=>route.fulfill({status:503,body:''}));
  await page.goto('/research/');
  await page.getByRole('link',{name:'View develop',exact:true}).click();
  await expect(page.locator('[data-version-notice]')).toContainText('temporarily unavailable');
  await expect(page).toHaveURL(/\/research\/$/);
});
test('develop navigation and search stay within develop', async({page})=>{
  await page.goto('/develop/research/');
  const links=await page.locator('header a[href]').evaluateAll(nodes=>nodes.map(n=>new URL(n.href).pathname));
  expect(links.filter(p=>!p.startsWith('/develop/'))).toEqual([]);
  const search=await page.request.get('/develop/search/search_index.json');
  expect(search.ok()).toBeTruthy();
  expect((await search.json()).docs.length).toBeGreaterThan(20);
  expect((await page.request.get('/develop/api/index.html')).ok()).toBeTruthy();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
});
