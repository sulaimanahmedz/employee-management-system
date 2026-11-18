import { test, expect } from '@playwright/test';

test.describe('Administrative account directory',()=>{
  test('reviews paginated accounts, rejects invalid pages and recovers after connectivity loss',async({page,context},info)=>{
    expect((await context.request.get('/api/v1/admin/accounts')).status()).toBe(401);
    await page.goto('/');
    await page.getByLabel('Login',{exact:true}).fill('synthetic-admin');
    await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
    await page.getByRole('button',{name:'Sign in',exact:true}).click();
    await expect(page.getByRole('heading',{name:'Create account'})).toBeVisible();
    const csrf=await(await context.request.get('/api/v1/auth/csrf')).json();
    for(let i=0;i<11;i++){
      const response=await context.request.post('/api/v1/admin/accounts',{headers:{[csrf.headerName]:csrf.token},data:{login:`directory-${info.project.name}-${i}`.padEnd(64,'x'),password:process.env.EMS_E2E_PASSWORD!,role:'EMPLOYEE'}});
      expect(response.status()).toBe(201);
    }
    expect((await context.request.get('/api/v1/admin/accounts?page=-1')).status()).toBe(400);
    const first=await(await context.request.get('/api/v1/admin/accounts?page=0')).json();
    expect(first.items).toHaveLength(10);
    first.items.forEach((item:object)=>expect(Object.keys(item).sort()).toEqual(['id','login','role']));
    await page.getByRole('button',{name:'View accounts'}).click();
    const directory=page.getByRole('region',{name:'Account directory'});
    await expect(directory.getByRole('listitem')).toHaveCount(10);
    await expect(directory.getByText(first.items[0].login,{exact:true})).toBeVisible();
    await expect(directory.getByRole('button',{name:'Previous accounts'})).toBeDisabled();
    await directory.getByRole('button',{name:'Next accounts'}).click();
    await expect(directory.getByText(/Page 2 of/)).toBeVisible();
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
    await directory.getByRole('button',{name:'Previous accounts'}).click();
    await expect(directory.getByText(/Page 1 of/)).toBeVisible();
    await context.setOffline(true);
    try {
      await directory.getByRole('button',{name:'View accounts'}).click();
      await expect(directory.getByRole('alert')).toHaveText('Unable to load accounts. Please retry.');
      await expect(directory.getByRole('listitem')).toHaveCount(0);
    } finally {await context.setOffline(false);}
    await directory.getByRole('button',{name:'View accounts'}).click();
    await expect(directory.getByRole('listitem')).toHaveCount(10);
    await page.getByRole('button',{name:'Sign out'}).click();
    await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
    expect((await context.request.get('/api/v1/admin/accounts')).status()).toBe(401);
  });
});
