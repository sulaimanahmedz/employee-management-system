import {test,expect} from '@playwright/test';
test.describe('Session logout boundary',()=>{
  test('rejects logout without CSRF, retains the session and then completes browser logout',async({page})=>{
    await page.goto('/');
    await page.getByLabel('Login',{exact:true}).fill('synthetic-hr');
    await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
    await page.getByRole('button',{name:'Sign in',exact:true}).click();
    await expect(page.getByRole('heading',{name:'Your people, connected.'})).toBeVisible();
    await page.getByRole('button',{name:'View my account'}).click();
    await expect(page.getByRole('region',{name:'My account'}).getByText('HR',{exact:true})).toBeVisible();
    expect((await page.context().request.post('/api/v1/auth/logout')).status()).toBe(403);
    expect((await page.context().request.get('/api/v1/auth/profile')).status()).toBe(200);
    await page.reload();
    await expect(page.getByRole('heading',{name:'Your people, connected.'})).toBeVisible();
    await page.getByRole('button',{name:'Sign out',exact:true}).click();
    await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
    await expect(page.getByRole('region',{name:'My account'})).toHaveCount(0);
    expect((await page.context().request.get('/api/v1/auth/profile')).status()).toBe(401);
    const csrf=await(await page.context().request.get('/api/v1/auth/csrf')).json();
    expect((await page.context().request.post('/api/v1/auth/logout',{headers:{[csrf.headerName]:csrf.token}})).status()).toBe(204);
    expect((await page.context().request.get('/api/v1/auth/profile')).status()).toBe(401);
    await expect(page.locator('body')).not.toContainText('AUTH_LOGOUT');
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
  });
});
