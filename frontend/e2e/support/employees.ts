import { expect } from '@playwright/test';

export async function employee(request: import('@playwright/test').APIRequestContext) {
  const r = await request.post('/api/v1/employees', { data: {
    firstName: 'Mobile', lastName: 'Test', email: 'mobile@example.com',
    department: 'Engineering', jobTitle: 'Engineer', status: 'ACTIVE',
  } });
  expect(r.status()).toBe(201);
  return (await r.json()).id as number;
}
