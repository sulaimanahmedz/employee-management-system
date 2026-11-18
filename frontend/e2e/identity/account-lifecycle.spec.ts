import {test,expect,type Page} from '@playwright/test';
async function signIn(page:Page,login:string,password:string){
 await page.goto('/');await page.getByLabel('Login',{exact:true}).fill(login);await page.getByLabel('Password',{exact:true}).fill(password);await page.getByRole('button',{name:'Sign in',exact:true}).click();await expect(page.getByRole('button',{name:'Sign out',exact:true})).toBeVisible();
}
test.describe('Account lifecycle and session revocation',()=>{
 test('changes password, resets it once and deactivates an account through real browser flows',async({page,browser},info)=>{
  const password=process.env.EMS_E2E_PASSWORD!;const login=`lifecycle-${info.project.name}`;
  await signIn(page,'synthetic-admin',password);
  const csrf=await(await page.context().request.get('/api/v1/auth/csrf')).json();
  const created=await page.context().request.post('/api/v1/admin/accounts',{headers:{[csrf.headerName]:csrf.token},data:{login,password,role:'EMPLOYEE'}});expect(created.status()).toBe(201);const account=await created.json();
  const member=await browser.newContext({baseURL:'http://127.0.0.1:5173',viewport:page.viewportSize()!});
  try{
   const user=await member.newPage();await signIn(user,login,password);
   await user.getByLabel('Current password',{exact:true}).fill('incorrect');await user.getByLabel('New password',{exact:true}).fill(password+'new');await user.getByRole('button',{name:'Change password',exact:true}).click();await expect(user.getByRole('alert')).toContainText('Password change failed');
   await user.getByLabel('Current password',{exact:true}).fill(password);await user.getByLabel('New password',{exact:true}).fill(password+'new');await user.getByRole('button',{name:'Change password',exact:true}).click();await expect(user.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
   await signIn(user,login,password+'new');
   await page.getByLabel('Account to manage').fill(String(account.id));await page.getByRole('button',{name:'Load account'}).click();await expect(page.getByText(`Managing ${login}`)).toBeVisible();
   await page.getByRole('button',{name:'Issue reset token'}).click();const token=await page.getByLabel('Issued reset token').inputValue();expect(token).toHaveLength(64);
   await user.getByRole('button',{name:'Sign out',exact:true}).click();
   await user.getByLabel('Reset token',{exact:true}).fill(token);await user.getByLabel('Replacement password').fill(password+'reset');await user.getByRole('button',{name:'Reset password',exact:true}).click();await expect(user.getByRole('status')).toContainText('Password reset.');
   await user.getByLabel('Reset token',{exact:true}).fill(token);await user.getByLabel('Replacement password').fill(password+'reset');await user.getByRole('button',{name:'Reset password',exact:true}).click();await expect(user.getByRole('status')).toContainText('Unable to reset');
   await signIn(user,login,password+'reset');
   await page.getByLabel('Account active').uncheck();await page.getByRole('button',{name:'Save account access'}).click();await expect(page.getByRole('status')).toContainText('Account access saved.');
   await user.getByRole('button',{name:'View my account'}).click();await expect(user.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
   await user.getByLabel('Login',{exact:true}).fill(login);await user.getByLabel('Password',{exact:true}).fill(password+'reset');await user.getByRole('button',{name:'Sign in',exact:true}).click();await expect(user.getByRole('alert')).toContainText('Unable to sign in');
   expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();expect(await user.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
   await page.getByRole('button',{name:'View recent security events'}).click();await expect(page.getByText(/ACCOUNT_ACCESS_CHANGED — actor/).first()).toBeVisible();
  }finally{await member.close();}
 });
 test('explicit employee assignment enables only that profile and revokes access when removed',async({page,browser},info)=>{
  const password=process.env.EMS_E2E_PASSWORD!;const login=`scoped-${info.project.name}`;
  const hr=await browser.newContext({baseURL:'http://127.0.0.1:5173'});const member=await browser.newContext({baseURL:'http://127.0.0.1:5173',viewport:page.viewportSize()!});
  try{
   const hrPage=await hr.newPage();await signIn(hrPage,'synthetic-hr',password);const hcsrf=await(await hr.request.get('/api/v1/auth/csrf')).json();
   const createdEmployee=await hr.request.post('/api/v1/employees',{headers:{[hcsrf.headerName]:hcsrf.token},data:{firstName:'Synthetic',lastName:'Scoped',email:`${login}@example.com`,department:'QA',jobTitle:'Analyst',status:'ACTIVE'}});expect(createdEmployee.status()).toBe(201);const employee=await createdEmployee.json();
   await signIn(page,'synthetic-admin',password);const csrf=await(await page.context().request.get('/api/v1/auth/csrf')).json();
   const created=await page.context().request.post('/api/v1/admin/accounts',{headers:{[csrf.headerName]:csrf.token},data:{login,password,role:'MANAGER'}});expect(created.status()).toBe(201);const account=await created.json();
   for(const invalid of [{employeeId:employee.id+0.5,teamEmployeeIds:[]},{employeeId:null,teamEmployeeIds:[employee.id+0.5]}]) {
    const rejected=await page.context().request.put(`/api/v1/admin/accounts/${account.id}/scope`,{headers:{[csrf.headerName]:csrf.token},data:invalid});expect(rejected.status()).toBe(400);
   }
   expect(await(await page.context().request.get(`/api/v1/admin/accounts/${account.id}/scope`)).json()).toEqual({employeeId:null,teamEmployeeIds:[]});
   const user=await member.newPage();await signIn(user,login,password);await user.getByRole('button',{name:'View assigned employees'}).click();await expect(user.getByRole('heading',{name:'Access denied'})).toBeVisible();
   expect((await member.request.get(`/api/v1/self/employees/${employee.id}`)).status()).toBe(403);
   await page.getByLabel('Account to manage').fill(String(account.id));await page.getByRole('button',{name:'Load account'}).click();await expect(page.getByText(`Managing ${login}`)).toBeVisible();
   await page.getByLabel('Team employee IDs').fill(String(employee.id));await page.getByRole('button',{name:'Save employee scope'}).click();await expect(page.getByRole('status')).toContainText('Employee scope saved.');
   await user.getByRole('button',{name:'View assigned employees'}).click();await expect(user.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();await signIn(user,login,password);
   await user.getByRole('button',{name:'View assigned employees'}).click();await user.getByRole('button',{name:`View employee ${employee.id}`,exact:true}).click();await expect(user.getByRole('heading',{name:'Synthetic Scoped'})).toBeVisible();
   expect((await member.request.get('/api/v1/self/employees/999999')).status()).toBe(403);expect((await member.request.get(`/api/v1/employees/${employee.id}/documents`)).status()).toBe(403);
   expect(await user.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
   await page.getByLabel('Team employee IDs').fill('');await page.getByRole('button',{name:'Save employee scope'}).click();await expect(page.getByRole('status')).toContainText('Employee scope saved.');
   await user.getByRole('button',{name:`View employee ${employee.id}`,exact:true}).click();await expect(user.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();await signIn(user,login,password);
   await user.getByRole('button',{name:'View assigned employees'}).click();await expect(user.getByRole('heading',{name:'Access denied'})).toBeVisible();
  }finally{await member.close();await hr.close();}
 });
});
