import { api } from '../../lib/api';
import type { Employee, EmployeeInput } from './types';

export const listEmployees = (signal?: AbortSignal) => api<Employee[]>('/api/v1/employees', { signal });
export const createEmployee = (employee: EmployeeInput) => api<Employee>('/api/v1/employees', {
  method: 'POST', body: JSON.stringify(employee),
});

export const getEmployee = (id: number, signal?: AbortSignal) =>
  api<Employee>(`/api/v1/employees/${id}`, { signal });

export const updateEmployee = (id: number, employee: EmployeeInput) =>
  api<Employee>(`/api/v1/employees/${id}`, {
    method: 'PUT', body: JSON.stringify(employee),
  });
