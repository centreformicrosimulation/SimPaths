import {test,expect} from '@playwright/test';
test('public site has no version controls or develop site',async({page})=>{
  for (const path of ['/', '/research/', '/overview/']) {
    await page.goto(path);
    await expect(page.locator('.sp-version, [data-version-target]')).toHaveCount(0);
    await expect(page.locator('link[rel="canonical"]')).toHaveAttribute('href','https://simpaths.org'+path);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
  }
  expect((await page.request.get('/develop/')).status()).toBe(404);
  expect((await page.request.get('/version.json')).status()).toBe(404);
  expect((await page.request.get('/api/index.html')).ok()).toBeTruthy();
  const search=await page.request.get('/search/search_index.json');
  expect(search.ok()).toBeTruthy();
  expect((await search.json()).docs.length).toBeGreaterThan(20);
});
