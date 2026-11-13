import {test,expect} from '@playwright/test';

test.describe('Credential audit privacy boundary',()=>{
  test('keeps rejection generic and permits subsequent successful login without exposing audit details',async({page},info)=>{
    await page.goto('/');
    await page.getByLabel('Login',{exact:true}).fill(`unknown-audit-${info.project.name}`);
    await page.getByLabel('Password',{exact:true}).fill('synthetic-audit-rejected-password');
    const rejected=page.waitForResponse(response=>response.url().endsWith('/api/v1/auth/login'));
    await page.getByRole('button',{name:'Sign in',exact:true}).click();
    const response=await rejected;expect(response.status()).toBe(401);expect(await response.text()).toBe('');
    await expect(page.getByRole('alert')).toContainText('Unable to sign in');
    await expect(page.getByLabel('Password',{exact:true})).toHaveValue('');
    await expect(page.locator('body')).not.toContainText('AUTH_CREDENTIALS');
    await page.getByLabel('Login',{exact:true}).fill('synthetic-hr');
    for(let i=0;i<4;i++){
      await page.getByLabel('Password',{exact:true}).fill('incorrect-synthetic-password');
      const failed=page.waitForResponse(response=>response.url().endsWith('/api/v1/auth/login'));
      await page.getByRole('button',{name:'Sign in',exact:true}).click();
      expect((await failed).status()).toBe(401);
      await expect(page.getByLabel('Password',{exact:true})).toHaveValue('');
    }
    await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
    await page.getByRole('button',{name:'Sign in',exact:true}).click();
    await expect(page.getByRole('heading',{name:'Your people, connected.'})).toBeVisible();
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
    await expect(page.locator('body')).not.toContainText('AUTH_CREDENTIALS');
    await page.getByRole('button',{name:'Sign out'}).click();
    await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
  });
});
