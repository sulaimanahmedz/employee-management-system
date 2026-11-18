package com.ems.identity.dto;
import java.util.List;
public record ScopeView(Long employeeId,List<Long> teamEmployeeIds) {}
