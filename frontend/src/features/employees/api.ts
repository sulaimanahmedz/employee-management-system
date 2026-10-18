import { api } from '../../lib/api';
import type { Employee, EmployeeInput } from './types';

export const listEmployees = (signal?: AbortSignal) => api<Employee[]>('/api/v1/employees', { signal });
export const createEmployee = (employee: EmployeeInput) => api<Employee>('/api/v1/employees', {
  method: 'POST', body: JSON.stringify(employee),
});
