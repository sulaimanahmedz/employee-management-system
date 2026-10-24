import { api } from '../../lib/api';
import type { Employee, EmployeeInput, EmployeeDirectory, DirectoryFilters } from './types';

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

export function searchEmployees(filters: DirectoryFilters, page: number, signal?: AbortSignal) {
  const params = new URLSearchParams({ q: filters.q, page: String(page), size: String(filters.size),
    sortBy: filters.sortBy, direction: filters.direction });
  if (filters.status) params.set('status', filters.status);
  return api<EmployeeDirectory>(`/api/v1/employees/directory?${params}`, { signal });
}

export const getEmploymentHistory = (id: number, page: number, signal?: AbortSignal,
  kind: '' | import('./types').EmploymentHistoryPage['items'][number]['kind'] = '') =>
  api<import('./types').EmploymentHistoryPage>(`/api/v1/employees/${id}/history?page=${page}&size=10${kind ? `&kind=${kind}` : ''}`, { signal });
