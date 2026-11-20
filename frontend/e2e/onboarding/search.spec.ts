import {test,expect} from '../support/auth-fixture';
test.describe('Onboarding task search',()=>{
 test('searches titles and owners with filters, retains failed searches and refreshes after edits',async({page,request})=>{
  const response=await request.post('/api/v1/employees',{data:{firstName:'Synthetic',lastName:'Tasksearch',email:'task-search@example.com',department:'IT',jobTitle:'Engineer',status:'ACTIVE'}});expect(response.status()).toBe(201);const employee=await response.json();const path=`/api/v1/employees/${employee.id}`;
  try {
   for(const [title,owner] of [['Laptop preparation','IT team'],['Orientation','People team'],['100%_access','IT team']])expect((await request.post(path+'/onboarding-tasks',{data:{title,owner,dueOn:'2026-12-01'}})).status()).toBe(201);
   expect((await request.get(path+'/onboarding-tasks?q='+ 'x'.repeat(151))).status()).toBe(400);
   await page.goto('/');await page.getByRole('button',{name:'View profile for Synthetic Tasksearch'}).click();const section=page.getByRole('region',{name:'Onboarding checklist'});const search=section.getByLabel('Search task title or owner');
   await search.fill('  LAPTOP  ');await section.getByRole('button',{name:'Search tasks',exact:true}).click();await expect(section.getByText('0 of 3 tasks complete · 1 matching tasks')).toBeVisible();await expect(section.getByText('Orientation',{exact:true})).toHaveCount(0);
   await search.fill('it TEAM');await section.getByRole('button',{name:'Search tasks',exact:true}).click();await expect(section.getByText('0 of 3 tasks complete · 2 matching tasks')).toBeVisible();
   await section.getByLabel('Show onboarding tasks').selectOption('false');await section.getByLabel('Tasks due on or before').fill('2026-12-01');
   await search.fill('%_');await section.getByRole('button',{name:'Search tasks',exact:true}).click();await expect(section.getByText('100%_access',{exact:true})).toBeVisible();await expect(section.getByText('0 of 3 tasks complete · 1 matching tasks')).toBeVisible();
   await section.getByRole('button',{name:'Edit task 100%_access'}).click();await expect(search).toBeDisabled();await section.getByLabel('Task title',{exact:true}).fill('Access');await section.getByRole('button',{name:'Save task changes'}).click();await expect(section.getByText('No tasks match these filters.')).toBeVisible();
   await page.context().setOffline(true);try {await search.fill('People');await section.getByRole('button',{name:'Search tasks',exact:true}).click();await expect(section.getByRole('alert')).toBeVisible();}finally{await page.context().setOffline(false);}
   await section.getByRole('button',{name:'Refresh checklist'}).click();await expect(section.getByText('Orientation',{exact:true})).toBeVisible();await section.getByRole('button',{name:'Clear task search'}).click();await expect(section.getByText('0 of 3 tasks complete · 3 matching tasks')).toBeVisible();
   const a=(await section.getByRole('button',{name:'Search tasks',exact:true}).boundingBox())!,b=(await section.getByRole('button',{name:'Clear task search'}).boundingBox())!;expect(a.height).toBeGreaterThanOrEqual(44);expect(b.height).toBeGreaterThanOrEqual(44);expect(b.x>=a.x+a.width+11 || b.y>=a.y+a.height+11).toBeTruthy();expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
  }finally{expect((await request.delete(path)).status()).toBe(204);}
 });
});
