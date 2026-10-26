import { expect, type APIRequestContext } from '@playwright/test';
export async function createProfileEmployee(request: APIRequestContext, suffix: string) {
  const input = { firstName: 'Synthetic', lastName: suffix, email: `profile.${suffix}@example.com`, department: 'IT', jobTitle: 'Engineer', status: 'ACTIVE' };
  const response = await request.post('/api/v1/employees', { data: input });
  expect(response.status()).toBe(201);
  return response.json() as Promise<typeof input & { id: number }>;
}
