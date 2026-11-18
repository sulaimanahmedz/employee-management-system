import {test,expect} from '@playwright/test';
test.describe('Access rejection privacy boundary',()=>{
  test('rejects anonymous and out-of-scope requests without exposing details or ending a valid session',async({page})=>{
    await page.goto('/');
    const anonymous=await page.context().request.get('/api/v1/employees?secret=synthetic-value');
    expect(anonymous.status()).toBe(401);expect(await anonymous.text()).toBe('');
    await page.getByLabel('Login',{exact:true}).fill('synthetic-manager');
    await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
    await page.getByRole('button',{name:'Sign in',exact:true}).click();
    await expect(page.getByRole('heading',{name:'Access denied'})).toBeVisible();
    for(const path of ['/api/v1/employees','/api/v1/admin/accounts','/api/v1/unassigned']) {
      const response=await page.context().request.get(path);
      expect(response.status()).toBe(403);expect(await response.text()).toBe('');
    }
    await page.getByRole('button',{name:'View my account'}).click();
    await expect(page.getByRole('region',{name:'My account'}).getByText('MANAGER',{exact:true})).toBeVisible();
    await page.reload();
    await expect(page.getByRole('heading',{name:'Access denied'})).toBeVisible();
    await expect(page.locator('body')).not.toContainText('event=ACCESS_');
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
    await page.getByRole('button',{name:'Sign out',exact:true}).click();
    await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
  });
});
