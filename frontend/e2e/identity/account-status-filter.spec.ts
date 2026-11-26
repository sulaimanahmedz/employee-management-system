import {test,expect} from '@playwright/test';

test.describe('Account access status filtering',()=>{
 test('combines role and status before paging and recovers without stale results',async({page,context},info)=>{
  await page.goto('/');
  await page.getByLabel('Login',{exact:true}).fill('synthetic-admin');
  await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
  await page.getByRole('button',{name:'Sign in',exact:true}).click();
  const directory=page.getByRole('region',{name:'Account directory'});
  await expect(directory).toBeVisible();
  const csrf=await(await context.request.get('/api/v1/auth/csrf')).json();
  for(let i=0;i<11;i++){
   const created=await context.request.post('/api/v1/admin/accounts',{headers:{[csrf.headerName]:csrf.token},data:{login:`state-filter-${info.project.name}-${i}`,password:process.env.EMS_E2E_PASSWORD!,role:'MANAGER'}});
   expect(created.status()).toBe(201);const account=await created.json();
   const changed=await context.request.put(`/api/v1/admin/accounts/${account.id}`,{headers:{[csrf.headerName]:csrf.token},data:{role:'MANAGER',enabled:false}});
   expect(changed.status()).toBe(200);
  }
  expect((await context.request.get('/api/v1/admin/accounts?enabled=unknown')).status()).toBe(400);
  await directory.getByLabel('Filter accounts by role').selectOption('MANAGER');
  await expect(directory.getByRole('button',{name:'View accounts'})).toBeEnabled();
  await directory.getByLabel('Filter accounts by status').selectOption('false');
  async function checkPage(index:number,enabled:string){
   const expected=await(await context.request.get(`/api/v1/admin/accounts?page=${index}&role=MANAGER&enabled=${enabled}`)).json();
   await expect(directory.getByText(`${expected.totalElements} accounts · Page ${index+1} of ${Math.max(1,expected.totalPages)}`)).toBeVisible();
   await expect(directory.getByRole('listitem')).toHaveCount(expected.items.length);
   for(const account of expected.items) await expect(directory.getByRole('listitem').filter({has:page.getByText(account.login,{exact:true})})).toContainText(enabled==='false'?'— Deactivated':'— Active');
  }
  await checkPage(0,'false');
  await directory.getByRole('button',{name:'Next accounts'}).click();await checkPage(1,'false');
  await directory.getByLabel('Filter accounts by status').selectOption('true');await checkPage(0,'true');
  await context.setOffline(true);
  try {await directory.getByRole('button',{name:'View accounts'}).click();await expect(directory.getByRole('alert')).toBeVisible();await expect(directory.getByRole('listitem')).toHaveCount(0);}
  finally {await context.setOffline(false);}
  await directory.getByRole('button',{name:'View accounts'}).click();await checkPage(0,'true');
  await expect(directory.getByLabel('Filter accounts by status')).toHaveValue('true');
  await directory.getByLabel('Filter accounts by status').selectOption('');
  await expect(directory.getByRole('listitem')).toHaveCount(10);
  await expect(directory.getByLabel('Filter accounts by role')).toHaveValue('MANAGER');
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
  expect((await directory.getByLabel('Filter accounts by status').boundingBox())!.height).toBeGreaterThanOrEqual(44);
  await page.getByRole('button',{name:'Sign out'}).click();await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
  expect((await context.request.get('/api/v1/admin/accounts?enabled=false')).status()).toBe(401);
 });
});
