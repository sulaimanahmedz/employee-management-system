import { test, expect } from '@playwright/test';

test.describe('Account role review', () => {
  test('filters persisted accounts before paging and retains the role after transport failure', async ({page,context},info) => {
    await page.goto('/');
    await page.getByLabel('Login',{exact:true}).fill('synthetic-admin');
    await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
    await page.getByRole('button',{name:'Sign in',exact:true}).click();
    const directory=page.getByRole('region',{name:'Account directory'});
    await expect(directory).toBeVisible();
    const csrf=await(await context.request.get('/api/v1/auth/csrf')).json();
    for(let i=0;i<11;i++) {
      const response=await context.request.post('/api/v1/admin/accounts',{headers:{[csrf.headerName]:csrf.token},data:{login:`role-review-${info.project.name}-${i}`,password:process.env.EMS_E2E_PASSWORD!,role:'MANAGER'}});
      expect(response.status()).toBe(201);
    }
    expect((await context.request.get('/api/v1/admin/accounts?role=OWNER')).status()).toBe(400);
    await directory.getByLabel('Filter accounts by role').selectOption('MANAGER');
    await expect(directory.getByRole('listitem')).toHaveCount(10);
    for(const item of await directory.getByRole('listitem').all()) await expect(item).toContainText('— MANAGER');
    const first=await(await context.request.get('/api/v1/admin/accounts?role=MANAGER&page=0')).json();
    await expect(directory.getByText(first.items[0].login,{exact:true})).toBeVisible();
    await directory.getByRole('button',{name:'Next accounts'}).click();
    await expect(directory.getByText(/Page 2 of/)).toBeVisible();
    const second=await(await context.request.get('/api/v1/admin/accounts?role=MANAGER&page=1')).json();
    await expect(directory.getByText(second.items[0].login,{exact:true})).toBeVisible();
    await directory.getByLabel('Filter accounts by role').selectOption('ADMIN');
    await expect(directory.getByText(/Page 1 of/)).toBeVisible();
    for(const item of await directory.getByRole('listitem').all()) await expect(item).toContainText('— ADMIN');
    await context.setOffline(true);
    try {
      await directory.getByRole('button',{name:'View accounts'}).click();
      await expect(directory.getByRole('alert')).toBeVisible();
      await expect(directory.getByRole('listitem')).toHaveCount(0);
    } finally { await context.setOffline(false); }
    await directory.getByRole('button',{name:'View accounts'}).click();
    await expect(directory.getByText(/Page 1 of/)).toBeVisible();
    await expect(directory.getByLabel('Filter accounts by role')).toHaveValue('ADMIN');
    await directory.getByLabel('Filter accounts by role').selectOption('');
    await expect(directory.getByRole('listitem')).toHaveCount(10);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
    expect((await directory.getByLabel('Filter accounts by role').boundingBox())!.height).toBeGreaterThanOrEqual(44);
    await page.getByRole('button',{name:'Sign out'}).click();
    await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
    expect((await context.request.get('/api/v1/admin/accounts?role=MANAGER')).status()).toBe(401);
  });
});
