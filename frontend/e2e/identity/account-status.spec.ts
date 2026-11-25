import {test,expect} from '@playwright/test';

test.describe('Administrative account status review',()=>{
 test('refreshes deactivation and reactivation and clears status on transport failure',async({page,context},info)=>{
  await page.goto('/');
  await page.getByLabel('Login',{exact:true}).fill('synthetic-admin');
  await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
  await page.getByRole('button',{name:'Sign in',exact:true}).click();
  const directory=page.getByRole('region',{name:'Account directory'});
  await expect(directory).toBeVisible();
  const csrf=await(await context.request.get('/api/v1/auth/csrf')).json();
  const login=`status-${info.project.name}`;
  const response=await context.request.post('/api/v1/admin/accounts',{headers:{[csrf.headerName]:csrf.token},data:{login,password:process.env.EMS_E2E_PASSWORD!,role:'HR'}});
  expect(response.status()).toBe(201);const account=await response.json();
  async function findAccount(){
   const [refreshed]=await Promise.all([page.waitForResponse(response=>response.url().includes('/api/v1/admin/accounts?page=0')),directory.getByRole('button',{name:'View accounts'}).click()]);
   expect(refreshed.status()).toBe(200);
   const first=await refreshed.json();
   await expect(directory.getByRole('listitem')).toHaveCount(first.items.length);
   await expect(directory.getByText(first.items[0].login,{exact:true})).toBeVisible();
   await expect(directory.getByRole('button',{name:'View accounts'})).toBeEnabled();
   while(await directory.getByText(login,{exact:true}).count()===0){
    const next=directory.getByRole('button',{name:'Next accounts'});await expect(next).toBeEnabled();
    const old=await directory.getByText(/accounts · Page/).textContent();await next.click();
    await expect(directory.getByText(/accounts · Page/)).not.toHaveText(old!);
   }
  }
  await directory.getByLabel('Filter accounts by role').selectOption('HR');
  await findAccount();
  const row=directory.getByRole('listitem').filter({hasText:login});await expect(row).toContainText('— Active');
  await page.getByLabel('Account to manage').fill(String(account.id));await page.getByRole('button',{name:'Load account'}).click();
  await expect(page.getByText(`Managing ${login}`)).toBeVisible();
  await page.getByLabel('Account active').uncheck();await page.getByRole('button',{name:'Save account access'}).click();
  await expect(page.getByRole('status')).toContainText('Account access saved.');
  await findAccount();await expect(row).toContainText('— Deactivated');
  await context.setOffline(true);
  try {await directory.getByRole('button',{name:'View accounts'}).click();await expect(directory.getByRole('alert')).toBeVisible();await expect(directory.getByRole('listitem')).toHaveCount(0);}
  finally {await context.setOffline(false);}
  await findAccount();await expect(row).toContainText('— Deactivated');
  await page.getByLabel('Account active').check();await page.getByRole('button',{name:'Save account access'}).click();
  await expect(page.getByRole('status')).toContainText('Account access saved.');
  await findAccount();await expect(row).toContainText('— Active');
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
  await page.getByRole('button',{name:'Sign out'}).click();await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
  expect((await context.request.get('/api/v1/admin/accounts')).status()).toBe(401);
 });
});
