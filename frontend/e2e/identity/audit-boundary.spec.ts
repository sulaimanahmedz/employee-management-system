import {test,expect} from '@playwright/test';

test.describe('Credential audit privacy boundary',()=>{
  test('keeps rejection generic and permits subsequent successful login without exposing audit details',async({page},info)=>{
    await page.goto('/');
    await page.getByLabel('Login',{exact:true}).fill(`unknown-audit-${info.project.name}`);
    await page.getByLabel('Password',{exact:true}).fill('synthetic-audit-rejected-password');
    const rejected=page.waitForResponse(response=>response.url().endsWith('/api/v1/auth/login'));
    await page.getByRole('button',{name:'Sign in',exact:true}).click();
    const response=await rejected;expect(response.status()).toBe(401);
    // Chromium may not retain a body resource for an empty fetch response.
    // Inspect the same real HTTP contract using APIResponse, without replacing the UI flow.
    const csrf=await(await page.context().request.get('/api/v1/auth/csrf')).json();
    const contract=await page.context().request.post('/api/v1/auth/login',{
      headers:{[csrf.headerName]:csrf.token},
      form:{username:`unknown-contract-${info.project.name}`,password:'synthetic-rejected-password'},
    });
    expect(contract.status()).toBe(401);expect(await contract.text()).toBe('');
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
