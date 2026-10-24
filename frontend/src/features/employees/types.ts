export interface Employee {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  department: string;
  jobTitle: string;
  status: 'ACTIVE' | 'ON_LEAVE' | 'INACTIVE';
}

export type EmployeeInput = Omit<Employee, 'id'>;

export interface EmployeeDirectory {
  items: Employee[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  totalEmployees: number;
  activeEmployees: number;
  departments: number;
}

export interface DirectoryFilters {
  q: string;
  status: '' | Employee['status'];
  size: number;
}

export interface EmploymentHistoryPage {
  items: { id: number; recordedAt: string; kind: 'CREATED' | 'BASELINE' | 'CHANGED';
    department: string; jobTitle: string; status: Employee['status'] }[];
  page: number; size: number; totalElements: number; totalPages: number;
}
