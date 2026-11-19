import {test,expect} from '@playwright/test';
test.describe('Administrative security event review',()=>{
 test('browses older events, retries transport errors and rejects unauthorized readers',async({page,context},info)=>{
  expect((await context.request.get('/api/v1/admin/audit?page=1')).status()).toBe(401);
  await page.goto('/');await page.getByLabel('Login',{exact:true}).fill('synthetic-admin');await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);await page.getByRole('button',{name:'Sign in',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Create account'})).toBeVisible();
  const csrf=await(await context.request.get('/api/v1/auth/csrf')).json();
  const login=`audit-reader-${info.project.name}`;
  const created=await context.request.post('/api/v1/admin/accounts',{headers:{[csrf.headerName]:csrf.token},data:{login,password:process.env.EMS_E2E_PASSWORD!,role:'EMPLOYEE'}});expect(created.status()).toBe(201);const account=await created.json();
  for(let i=0;i<21;i++)expect((await context.request.put(`/api/v1/admin/accounts/${account.id}`,{headers:{[csrf.headerName]:csrf.token},data:{role:'EMPLOYEE',enabled:i%2===0}})).status()).toBe(200);
  const first=await(await context.request.get('/api/v1/admin/audit?page=0')).json();const second=await(await context.request.get('/api/v1/admin/audit?page=1')).json();
  expect((await context.request.get('/api/v1/admin/audit?page=-1')).status()).toBe(400);
  const review=page.getByRole('region',{name:'Security event review'});
  await review.getByRole('button',{name:'View recent security events'}).click();await expect(review.getByRole('listitem')).toHaveCount(20);await expect(review.getByRole('listitem').first()).toContainText(`Event ${first[0].id}:`);
  await expect(review.getByRole('button',{name:'Newer security events'})).toBeDisabled();
  await review.getByRole('button',{name:'Older security events'}).click();await expect(review.getByRole('status')).toContainText('Page 2');await expect(review.getByRole('listitem').first()).toContainText(`Event ${second[0].id}:`);
  const buttons=review.getByRole('navigation').getByRole('button');const a=(await buttons.nth(0).boundingBox())!,b=(await buttons.nth(1).boundingBox())!;
  expect(a.height).toBeGreaterThanOrEqual(44);expect(b.height).toBeGreaterThanOrEqual(44);expect(b.x>=a.x+a.width+11 || b.y>=a.y+a.height+11).toBeTruthy();expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
  await context.setOffline(true);try {await review.getByRole('button',{name:'Older security events'}).click();await expect(review.getByRole('alert')).toBeVisible();await expect(review.getByRole('listitem')).toHaveCount(0);} finally {await context.setOffline(false);}
  await review.getByRole('button',{name:'Retry security events'}).click();await expect(review.getByRole('status')).toContainText('Page 3');await review.getByRole('button',{name:'View recent security events'}).click();await expect(review.getByRole('status')).toContainText('Page 1');
  await page.getByRole('button',{name:'Sign out'}).click();await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
  await page.getByLabel('Login',{exact:true}).fill(login);await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);await page.getByRole('button',{name:'Sign in',exact:true}).click();await expect(page.getByRole('heading',{name:'Access denied'})).toBeVisible();expect((await context.request.get('/api/v1/admin/audit?page=1')).status()).toBe(403);await expect(review).toHaveCount(0);await page.getByRole('button',{name:'Sign out'}).click();
 });
});
