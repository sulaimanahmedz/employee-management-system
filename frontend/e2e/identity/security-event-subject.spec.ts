import {test,expect} from '@playwright/test';
test.describe('Security event subject investigation',()=>{
 test('reviews one affected account with action, paging, validation and recovery',async({page,context},info)=>{
  await page.goto('/');await page.getByLabel('Login',{exact:true}).fill('synthetic-admin');await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);await page.getByRole('button',{name:'Sign in',exact:true}).click();
  const review=page.getByRole('region',{name:'Security event review'});await expect(review).toBeVisible();
  const csrf=await(await context.request.get('/api/v1/auth/csrf')).json();
  const created=await context.request.post('/api/v1/admin/accounts',{headers:{[csrf.headerName]:csrf.token},data:{login:`subject-${info.project.name}`,password:process.env.EMS_E2E_PASSWORD!,role:'EMPLOYEE'}});expect(created.status()).toBe(201);const target=await created.json();
  for(let i=0;i<21;i++)expect((await context.request.put(`/api/v1/admin/accounts/${target.id}`,{headers:{[csrf.headerName]:csrf.token},data:{role:'EMPLOYEE',enabled:i%2===0}})).status()).toBe(200);
  expect((await context.request.get('/api/v1/admin/audit?subjectId=0')).status()).toBe(400);
  const input=review.getByLabel('Subject account ID');await input.fill('0');await review.getByRole('button',{name:'Apply subject filter'}).click();expect(await input.evaluate((el:HTMLInputElement)=>el.validity.valid)).toBe(false);
  await input.fill(String(target.id));await review.getByRole('button',{name:'Apply subject filter'}).click();await expect(review.getByRole('listitem')).toHaveCount(20);
  for(const row of await review.getByRole('listitem').allTextContents())expect(row).toMatch(new RegExp(`subject ${target.id}$`));
  await review.getByRole('button',{name:'Older security events'}).click();await expect(review.getByRole('status')).toHaveText('Security events · Page 2');await expect(review.getByRole('listitem')).toHaveCount(1);
  await review.getByLabel('Security event action').selectOption('ACCOUNT_ACCESS_CHANGED');await expect(review.getByRole('status')).toHaveText('Security events · Page 1');await expect(review.getByRole('listitem')).toHaveCount(20);
  await context.setOffline(true);try{await review.getByRole('button',{name:'View recent security events'}).click();await expect(review.getByRole('alert')).toBeVisible();await expect(review.getByRole('listitem')).toHaveCount(0);}finally{await context.setOffline(false);}
  await review.getByRole('button',{name:'Retry security events'}).click();await expect(review.getByRole('listitem')).toHaveCount(20);await expect(input).toHaveValue(String(target.id));
  await review.getByLabel('Security event action').selectOption('PASSWORD_RESET');await expect(review.getByText('No security events on this page.')).toBeVisible();
  await review.getByRole('button',{name:'Clear subject filter'}).click();await expect(input).toHaveValue('');await expect(review.getByLabel('Security event action')).toHaveValue('PASSWORD_RESET');
  await expect(review.getByText(/Subject filter: All subjects/)).toBeVisible();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();expect((await input.boundingBox())!.height).toBeGreaterThanOrEqual(44);
  await page.getByRole('button',{name:'Sign out'}).click();await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();expect((await context.request.get(`/api/v1/admin/audit?subjectId=${target.id}`)).status()).toBe(401);
 });
});
