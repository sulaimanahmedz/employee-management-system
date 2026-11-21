import {test,expect} from '@playwright/test';
test.describe('Security event action filtering',()=>{
 test('filters persisted events before paging, retries errors and restores all actions',async({page,context},info)=>{
  await page.goto('/');await page.getByLabel('Login',{exact:true}).fill('synthetic-admin');await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);await page.getByRole('button',{name:'Sign in',exact:true}).click();await expect(page.getByRole('heading',{name:'Create account'})).toBeVisible();
  const csrf=await(await context.request.get('/api/v1/auth/csrf')).json();const created=await context.request.post('/api/v1/admin/accounts',{headers:{[csrf.headerName]:csrf.token},data:{login:`filter-${info.project.name}`,password:process.env.EMS_E2E_PASSWORD!,role:'EMPLOYEE'}});expect(created.status()).toBe(201);const target=await created.json();
  for(let i=0;i<21;i++)expect((await context.request.put(`/api/v1/admin/accounts/${target.id}`,{headers:{[csrf.headerName]:csrf.token},data:{role:'EMPLOYEE',enabled:i%2===0}})).status()).toBe(200);
  expect((await context.request.get('/api/v1/admin/audit?action=UNKNOWN')).status()).toBe(400);
  const review=page.getByRole('region',{name:'Security event review'});const select=review.getByLabel('Security event action');await select.selectOption('ACCOUNT_ACCESS_CHANGED');await expect(review.getByRole('listitem')).toHaveCount(20);
  for(const item of await review.getByRole('listitem').allTextContents())expect(item).toContain('ACCOUNT_ACCESS_CHANGED');
  await review.getByRole('button',{name:'Older security events'}).click();await expect(review.getByRole('status')).toContainText('Page 2');
  const actual=await(await context.request.get('/api/v1/admin/audit?page=1&action=ACCOUNT_ACCESS_CHANGED')).json();await expect(review.getByRole('listitem')).toHaveCount(actual.length);await expect(review.getByRole('listitem').first()).toContainText(`Event ${actual[0].id}:`);
  await context.setOffline(true);try{await select.selectOption('PASSWORD_CHANGED');await expect(review.getByRole('alert')).toBeVisible();await expect(review.getByRole('listitem')).toHaveCount(0);}finally{await context.setOffline(false);}
  await review.getByRole('button',{name:'Retry security events'}).click();await expect(review.getByRole('status')).toContainText('Page 1');await expect(select).toHaveValue('PASSWORD_CHANGED');
  await select.selectOption('');await expect(review.getByRole('listitem')).toHaveCount(20);expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();expect((await select.boundingBox())!.height).toBeGreaterThanOrEqual(44);
  await page.getByRole('button',{name:'Sign out'}).click();await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();expect((await context.request.get('/api/v1/admin/audit?action=PASSWORD_CHANGED')).status()).toBe(401);
 });
});
