import {test,expect} from '@playwright/test';
test.describe('API login without saved requests',()=>{
  test('rejects without creating a session then signs in without redirects or replay',async({page})=>{
    const rejected=await page.context().request.get('/api/v1/employees?search=synthetic-private-value');
    expect(rejected.status()).toBe(401);
    expect(rejected.headers()['set-cookie']).toBeUndefined();
    expect(rejected.headers()['location']).toBeUndefined();
    expect(await page.context().cookies()).toEqual([]);
    const requested:string[]=[];page.on('request',request=>requested.push(request.url()));
    await page.goto('/');
    await page.getByLabel('Login',{exact:true}).fill('synthetic-employee');
    await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
    const loginResponse=page.waitForResponse(response=>response.url().endsWith('/api/v1/auth/login'));
    await page.getByRole('button',{name:'Sign in',exact:true}).click();
    const login=await loginResponse;expect(login.status()).toBe(204);expect(login.headers()['location']).toBeUndefined();
    await expect(page.getByRole('heading',{name:'Access denied'})).toBeVisible();
    await page.getByRole('button',{name:'View my account'}).click();
    await expect(page.getByRole('region',{name:'My account'}).getByText('EMPLOYEE',{exact:true})).toBeVisible();
    expect(requested.some(url=>url.includes('synthetic-private-value'))).toBe(false);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
    await page.getByRole('button',{name:'Sign out',exact:true}).click();
    await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
  });
});
