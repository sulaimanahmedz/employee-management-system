import {test,expect} from '@playwright/test';
test.describe('Authenticated account profile',()=>{
  for(const role of ['admin','hr','manager','employee']){
    test(`${role} reads only their account and recovers after connectivity failure`,async({page})=>{
      await page.goto('/');
      await page.getByLabel('Login',{exact:true}).fill(`synthetic-${role}`);
      await page.getByLabel('Password',{exact:true}).fill(process.env.EMS_E2E_PASSWORD!);
      await page.getByRole('button',{name:'Sign in',exact:true}).click();
      await page.getByRole('button',{name:'View my account'}).click();
      const profile=page.getByRole('region',{name:'My account'});
      await expect(profile.getByText(`synthetic-${role}`,{exact:true})).toBeVisible();
      await expect(profile.getByText(role.toUpperCase(),{exact:true})).toBeVisible();
      const response=await page.context().request.get('/api/v1/auth/profile?login=synthetic-admin&id=1');
      expect(response.status()).toBe(200);const body=await response.json();
      expect(body.login).toBe(`synthetic-${role}`);expect(Object.keys(body).sort()).toEqual(['id','login','role']);
      expect((await page.context().request.get('/api/v1/auth/profile/1')).status()).toBe(403);
      expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
      await page.context().setOffline(true);
      try{
        await profile.getByRole('button').click();
        await expect(profile.getByRole('alert')).toContainText('Unable to load your account');
        await expect(profile.getByText(`synthetic-${role}`,{exact:true})).toHaveCount(0);
      }finally{await page.context().setOffline(false);}
      await profile.getByRole('button').click();
      await expect(profile.getByText(`synthetic-${role}`,{exact:true})).toBeVisible();
      await page.getByRole('button',{name:'Sign out',exact:true}).click();
      await expect(page.getByRole('heading',{name:'Sign in',exact:true})).toBeVisible();
      await expect(profile).toHaveCount(0);
      expect((await page.context().request.get('/api/v1/auth/profile')).status()).toBe(401);
    });
  }
});
