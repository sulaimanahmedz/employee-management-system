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
